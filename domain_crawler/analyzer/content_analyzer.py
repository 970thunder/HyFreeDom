#!/usr/bin/env python3
"""
内容分析模块
负责检查网站内容是否符合法律法规要求
"""

import re
import jieba
from typing import Dict, Any, List, Set
import logging

class ContentAnalyzer:
    """内容分析器"""

    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger(__name__)
        self.sensitive_keywords = set(config.get('sensitive_keywords', []))
        self.illegal_keywords = set(config.get('illegal_keywords', []))
        self.check_threshold = config.get('check_threshold', 0.8)

        # 初始化中文分词
        self._init_jieba()

    def _init_jieba(self):
        """初始化中文分词配置"""
        # 加载自定义词典（敏感词）
        for keyword in self.sensitive_keywords:
            jieba.add_word(keyword)

        for keyword in self.illegal_keywords:
            jieba.add_word(keyword)

    async def analyze(self, content_data: Dict[str, Any], domain: str) -> Dict[str, Any]:
        """
        分析网站内容

        Args:
            content_data: 网站内容数据
            domain: 域名（用于日志记录）

        Returns:
            分析结果字典
        """
        if not content_data or not content_data.get('content'):
            return {
                'is_compliant': False,
                'reasons': ['无内容或内容为空'],
                'violation_score': 1.0
            }

        text_content = content_data.get('content', '')
        title = content_data.get('title', '')
        description = content_data.get('description', '')

        # 合并所有文本内容
        full_text = f"{title} {description} {text_content}"

        # 分析敏感内容
        analysis_result = self._analyze_text(full_text)

        # 计算违规分数
        violation_score = self._calculate_violation_score(analysis_result)

        # 判断是否违规
        is_compliant = violation_score < self.check_threshold

        result = {
            'is_compliant': is_compliant,
            'violation_score': violation_score,
            'reasons': analysis_result.get('reasons', []),
            'matched_keywords': analysis_result.get('matched_keywords', []),
            'keyword_counts': analysis_result.get('keyword_counts', {}),
            'analysis_time': analysis_result.get('analysis_time', 0)
        }

        self._log_analysis_result(domain, result)

        return result

    def _analyze_text(self, text: str) -> Dict[str, Any]:
        """
        分析文本内容

        Args:
            text: 要分析的文本

        Returns:
            分析结果
        """
        import time
        start_time = time.time()

        # 清洗文本
        clean_text = self._clean_text(text)

        # 中文分词
        words = list(jieba.cut(clean_text))

        # 关键词匹配
        matched_keywords = []
        keyword_counts = {}
        reasons = []

        # 检查敏感关键词
        for keyword in self.sensitive_keywords:
            count = text.count(keyword)
            if count > 0:
                matched_keywords.append(f"敏感词: {keyword}")
                keyword_counts[f"敏感_{keyword}"] = count

                if count >= 3:  # 超过3次认为是显著违规
                    reasons.append(f"包含敏感关键词 '{keyword}' ({count}次)")

        # 检查违法关键词
        for keyword in self.illegal_keywords:
            count = text.count(keyword)
            if count > 0:
                matched_keywords.append(f"违法词: {keyword}")
                keyword_counts[f"违法_{keyword}"] = count
                reasons.append(f"包含违法关键词 '{keyword}' ({count}次)")

        # 检查其他违规模式
        patterns = self._get_patterns()
        for pattern_name, pattern_info in patterns.items():
            matches = re.findall(pattern_info['pattern'], text, re.IGNORECASE)
            if matches:
                matched_keywords.append(f"模式匹配: {pattern_name}")
                keyword_counts[f"模式_{pattern_name}"] = len(matches)
                reasons.append(f"匹配违规模式 '{pattern_name}' ({len(matches)}处)")

        analysis_time = time.time() - start_time

        return {
            'matched_keywords': matched_keywords,
            'keyword_counts': keyword_counts,
            'reasons': reasons,
            'analysis_time': analysis_time
        }

    def _calculate_violation_score(self, analysis_result: Dict[str, Any]) -> float:
        """
        计算违规分数

        Args:
            analysis_result: 分析结果

        Returns:
            违规分数（0-1之间，越高越严重）
        """
        keyword_counts = analysis_result.get('keyword_counts', {})
        reasons = analysis_result.get('reasons', [])

        # 基础分数：基于关键词数量
        total_keywords = sum(keyword_counts.values())
        keyword_score = min(total_keywords / 10, 1.0)  # 每个关键词贡献0.1分，最多1分

        # 严重性分数：基于违规类型
        severity_score = 0.0

        for reason in reasons:
            if '违法关键词' in reason:
                severity_score += 0.3
            elif '敏感关键词' in reason:
                severity_score += 0.2
            elif '违规模式' in reason:
                severity_score += 0.1

        severity_score = min(severity_score, 1.0)

        # 综合分数
        final_score = (keyword_score + severity_score) / 2

        return round(final_score, 2)

    def _clean_text(self, text: str) -> str:
        """清理文本"""
        # 去除HTML标签
        text = re.sub(r'<[^>]+>', ' ', text)
        # 去除特殊字符
        text = re.sub(r'[^\w\u4e00-\u9fff\s]', ' ', text)
        # 去除多余空格
        text = re.sub(r'\s+', ' ', text)
        return text.strip()

    def _get_patterns(self) -> Dict[str, Dict[str, Any]]:
        """获取正则表达式模式"""
        patterns = {
            '电话号码': {
                'pattern': r'1[3-9]\d{9}',
                'severity': 0.2
            },
            '邮箱地址': {
                'pattern': r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b',
                'severity': 0.1
            },
            '网址链接': {
                'pattern': r'https?://[^\s]+',
                'severity': 0.1
            },
            '银行卡号': {
                'pattern': r'\b\d{16,19}\b',
                'severity': 0.3
            },
            '身份证号': {
                'pattern': r'\b\d{17}[0-9Xx]\b',
                'severity': 0.3
            }
        }
        return patterns

    def _log_analysis_result(self, domain: str, result: Dict[str, Any]):
        """记录分析结果"""
        if result['is_compliant']:
            self.logger.info(f"域名 {domain} 内容分析通过，违规分数: {result['violation_score']}")
        else:
            self.logger.warning(
                f"域名 {domain} 内容分析不通过，违规分数: {result['violation_score']}, "
                f"原因: {result['reasons']}"
            )

    def add_keywords(self, keywords: List[str], keyword_type: str = 'sensitive'):
        """
        动态添加关键词

        Args:
            keywords: 关键词列表
            keyword_type: 关键词类型 ('sensitive' 或 'illegal')
        """
        if keyword_type == 'sensitive':
            self.sensitive_keywords.update(keywords)
            for keyword in keywords:
                jieba.add_word(keyword)
        elif keyword_type == 'illegal':
            self.illegal_keywords.update(keywords)
            for keyword in keywords:
                jieba.add_word(keyword)

# 测试函数
def test_analyzer():
    """测试分析器"""
    config = {
        'sensitive_keywords': ['色情', '赌博', '诈骗'],
        'illegal_keywords': ['毒品', '武器'],
        'check_threshold': 0.8
    }

    analyzer = ContentAnalyzer(config)

    # 测试合规内容
    compliant_content = "欢迎来到我们的网站，我们提供优质的服务和产品。"
    result = analyzer._analyze_text(compliant_content)
    score = analyzer._calculate_violation_score(result)
    print(f"合规内容分数: {score}")

    # 测试违规内容
    violating_content = "这是一个赌博网站，提供色情内容和赌球服务。"
    result = analyzer._analyze_text(violating_content)
    score = analyzer._calculate_violation_score(result)
    print(f"违规内容分数: {score}")
    print(f"违规原因: {result['reasons']}")

if __name__ == "__main__":
    test_analyzer()