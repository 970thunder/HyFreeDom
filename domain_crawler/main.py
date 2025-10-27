#!/usr/bin/env python3
"""
域名内容审核爬虫
自动爬取二级域名网站内容，进行合规性审核，发现违规内容发送邮件通知
"""

#!/usr/bin/env python3
import asyncio
import logging
from datetime import datetime
from config.config import config, ensure_directories
from database.db_manager import DatabaseManager
from crawler.domain_crawler import DomainCrawler
from analyzer.content_analyzer import ContentAnalyzer
from notifier.email_notifier import EmailNotifier
from scheduler.scheduler import Scheduler
from utils.logger import setup_logging

class DomainCrawlerApp:
    def __init__(self):
        self.config = config
        self.setup_logging()
        self.db_manager = None
        self.crawler = None
        self.analyzer = None
        self.notifier = None
        self.scheduler = None

    def setup_logging(self):
        """设置日志配置"""
        ensure_directories()  # 确保日志目录存在
        setup_logging(self.config.LOG_LEVEL, self.config.LOG_FILE)
        self.logger = logging.getLogger(__name__)

    async def initialize(self):
        """初始化组件"""
        try:
            # 初始化数据库管理器
            self.db_manager = DatabaseManager(self.config.DB_CONFIG)
            await self.db_manager.connect()

            # 初始化爬虫
            self.crawler = DomainCrawler(self.config.CRAWLER_CONFIG)

            # 初始化内容分析器
            self.analyzer = ContentAnalyzer(self.config.ANALYZER_CONFIG)

            # 初始化邮件通知
            self.notifier = EmailNotifier(self.config.EMAIL_CONFIG)

            # 初始化调度器
            self.scheduler = Scheduler(
                interval_minutes=self.config.CRAWL_INTERVAL,
                callback=self.run_crawling_job
            )

            self.logger.info("域名爬虫应用初始化完成")

        except Exception as e:
            self.logger.error(f"应用初始化失败: {e}")
            raise

    async def run_crawling_job(self):
        """执行爬虫任务"""
        self.logger.info("开始执行域名爬虫任务")

        try:
            # 获取所有二级域名记录
            domains = await self.db_manager.get_subdomains()
            self.logger.info(f"获取到 {len(domains)} 个二级域名")

            violations = []
            for domain in domains:
                self.logger.info(f"开始检查域名: {domain['name']}")

                try:
                    # 爬取网站内容
                    content = await self.crawler.crawl(domain['name'])

                    if content:
                        # 分析内容合规性
                        analysis_result = await self.analyzer.analyze(content, domain['name'])

                        if not analysis_result['is_compliant']:
                            violations.append({
                                'domain': domain['name'],
                                'content': content[:1000],  # 截取部分内容
                                'reasons': analysis_result['reasons'],
                                'timestamp': datetime.now()
                            })
                            self.logger.warning(f"发现违规内容 - 域名: {domain['name']}, 原因: {analysis_result['reasons']}")

                except Exception as e:
                    self.logger.error(f"域名 {domain['name']} 检查失败: {e}")
                    continue

            # 发送违规通知
            if violations:
                await self.send_violation_notification(violations)
            else:
                self.logger.info("本次检查未发现违规内容")

        except Exception as e:
            self.logger.error(f"爬虫任务执行失败: {e}")

    async def send_violation_notification(self, violations):
        """发送违规通知"""
        try:
            await self.notifier.send_violation_alert(violations)
            self.logger.info(f"已发送 {len(violations)} 个违规通知")
        except Exception as e:
            self.logger.error(f"发送违规通知失败: {e}")

    async def start(self):
        """启动应用"""
        try:
            await self.initialize()
            self.logger.info("启动域名爬虫调度器")
            await self.scheduler.start()
        except Exception as e:
            self.logger.error(f"应用启动失败: {e}")
            await self.shutdown()

    async def shutdown(self):
        """关闭应用"""
        self.logger.info("正在关闭域名爬虫应用")

        if self.scheduler:
            await self.scheduler.stop()

        if self.db_manager:
            await self.db_manager.close()

        self.logger.info("域名爬虫应用已关闭")

async def main():
    """主函数"""
    app = DomainCrawlerApp()

    try:
        await app.start()
    except KeyboardInterrupt:
        await app.shutdown()
    except Exception as e:
        logging.error(f"应用运行异常: {e}")
        await app.shutdown()

if __name__ == "__main__":
    asyncio.run(main())