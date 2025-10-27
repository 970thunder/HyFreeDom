#!/usr/bin/env python3
"""
域名爬虫模块
负责爬取网站内容并进行初步处理
"""

import aiohttp
import asyncio
import re
from typing import Optional, Dict, Any
import logging
from bs4 import BeautifulSoup
import hashlib
from urllib.parse import urljoin

class DomainCrawler:
    """域名爬虫"""

    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger(__name__)
        self.session = None
        self.timeout = aiohttp.ClientTimeout(total=config['timeout'])

    async def __aenter__(self):
        await self.start()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.close()

    async def start(self):
        """启动会话"""
        connector = aiohttp.TCPConnector(limit=10, limit_per_host=5)
        self.session = aiohttp.ClientSession(
            connector=connector,
            timeout=self.timeout,
            headers={'User-Agent': self.config['user_agent']}
        )

    async def close(self):
        """关闭会话"""
        if self.session:
            await self.session.close()

    async def crawl(self, domain: str) -> Optional[Dict[str, Any]]:
        """
        爬取指定域名的内容

        Args:
            domain: 域名（如 example.com）

        Returns:
            包含网站内容的字典，失败返回None
        """
        urls_to_try = [
            f"http://{domain}",
            f"https://{domain}",
            f"http://www.{domain}",
            f"https://www.{domain}"
        ]

        for url in urls_to_try:
            try:
                content = await self._fetch_url_content(url)
                if content:
                    self.logger.info(f"成功爬取 {domain} (URL: {url})")
                    return content
            except Exception as e:
                self.logger.warning(f"尝试 {url} 失败: {e}")
                continue

        self.logger.error(f"所有URL尝试失败: {domain}")
        return None

    async def _fetch_url_content(self, url: str) -> Optional[Dict[str, Any]]:
        """
        获取URL内容

        Args:
            url: 完整URL地址

        Returns:
            网站内容信息字典
        """
        for attempt in range(self.config['max_retries']):
            try:
                async with self.session.get(url, ssl=False) as response:
                    if response.status != 200:
                        self.logger.warning(f"HTTP {response.status} for {url}")
                        return None

                    # 检查内容类型
                    content_type = response.headers.get('content-type', '').lower()
                    if 'text/html' not in content_type and 'text/plain' not in content_type:
                        self.logger.warning(f"不支持的内容类型: {content_type} for {url}")
                        return None

                    # 读取内容（限制大小）
                    content_bytes = await response.read()

                    if len(content_bytes) > self.config['max_content_size']:
                        content_bytes = content_bytes[:self.config['max_content_size']]

                    # 尝试解码
                    try:
                        content_str = content_bytes.decode('utf-8')
                    except UnicodeDecodeError:
                        try:
                            content_str = content_bytes.decode('gbk')
                        except UnicodeDecodeError:
                            content_str = content_bytes.decode('utf-8', errors='ignore')

                    return self._process_content(content_str, url)

            except asyncio.TimeoutError:
                self.logger.warning(f"请求超时 (尝试 {attempt + 1}/{self.config['max_retries']}): {url}")
            except aiohttp.ClientError as e:
                self.logger.warning(f"客户端错误 (尝试 {attempt + 1}/{self.config['max_retries']}): {e}")
            except Exception as e:
                self.logger.error(f"未知错误 (尝试 {attempt + 1}/{self.config['max_retries']}): {e}")

            # 重试前等待
            if attempt < self.config['max_retries'] - 1:
                await asyncio.sleep(self.config['delay'] * (2 ** attempt))  # 指数退避

        return None

    def _process_content(self, content: str, url: str) -> Dict[str, Any]:
        """
        处理网站内容

        Args:
            content: 网站HTML/文本内容
            url: 原始URL

        Returns:
            处理后的内容字典
        """
        # 创建内容hash用于去重
        content_hash = hashlib.md5(content.encode('utf-8')).hexdigest()

        # 提取基本信息
        title = ""
        description = ""
        text_content = ""

        try:
            soup = BeautifulSoup(content, 'html.parser')

            # 提取标题
            title_tag = soup.find('title')
            if title_tag:
                title = title_tag.get_text().strip()

            # 提取meta描述
            meta_desc = soup.find('meta', attrs={'name': 'description'})
            if meta_desc:
                description = meta_desc.get('content', '').strip()

            # 提取纯文本内容（去除脚本和样式）
            for script in soup(['script', 'style']):
                script.decompose()

            text_content = soup.get_text()
            # 清理文本内容
            text_content = re.sub(r'\s+', ' ', text_content).strip()
            text_content = text_content[:10000]  # 限制文本长度

        except Exception as e:
            self.logger.warning(f"HTML解析失败: {e}")
            # 如果解析失败，直接使用原始文本
            text_content = content[:10000]

        result = {
            'url': url,
            'title': title,
            'description': description,
            'content': text_content,
            'content_hash': content_hash,
            'content_length': len(text_content),
            'timestamp': asyncio.get_event_loop().time()
        }

        return result

    async def crawl_multiple(self, domains: list) -> Dict[str, Optional[Dict[str, Any]]]:
        """
        批量爬取多个域名

        Args:
            domains: 域名列表

        Returns:
            域名到内容的映射字典
        """
        if not self.session:
            await self.start()

        results = {}

        # 分组处理以避免并发过多
        batch_size = 5
        domains_batches = [domains[i:i + batch_size]
                          for i in range(0, len(domains), batch_size)]

        for batch in domains_batches:
            tasks = []
            for domain in batch:
                task = asyncio.create_task(self.crawl(domain))
                tasks.append((domain, task))

            # 等待批处理完成
            for domain, task in tasks:
                try:
                    result = await task
                    results[domain] = result
                except Exception as e:
                    self.logger.error(f"批量爬取失败 {domain}: {e}")
                    results[domain] = None

            # 批次间延迟
            await asyncio.sleep(self.config['delay'])

        return results

# 测试函数
async def test_crawler():
    """测试爬虫"""
    config = {
        'timeout': 10,
        'user_agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'max_retries': 3,
        'delay': 0.5,
        'max_content_size': 1048576
    }

    crawler = DomainCrawler(config)

    async with crawler:
        # 测试示例域名（使用baidu.com，因为它相对稳定）
        result = await crawler.crawl("baidu.com")

        if result:
            print(f"爬取成功!")
            print(f"标题: {result.get('title', 'N/A')}")
            print(f"内容长度: {result.get('content_length', 0)} 字符")
            print(f"内容预览: {result.get('content', '')[:200]}...")
        else:
            print("爬取失败")

if __name__ == "__main__":
    asyncio.run(test_crawler())