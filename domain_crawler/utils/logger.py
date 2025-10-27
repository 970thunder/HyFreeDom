#!/usr/bin/env python3
"""
日志记录模块
提供统一的日志配置和管理
"""

import logging
import logging.handlers
import os
from datetime import datetime

def setup_logging(log_level: str = 'INFO', log_file: str = 'logs/crawler.log'):
    """
    设置日志配置

    Args:
        log_level: 日志级别 (DEBUG, INFO, WARNING, ERROR)
        log_file: 日志文件路径
    """
    # 创建日志目录
    os.makedirs(os.path.dirname(log_file), exist_ok=True)

    # 设置日志级别
    numeric_level = getattr(logging, log_level.upper(), None)
    if not isinstance(numeric_level, int):
        numeric_level = logging.INFO

    # 创建日志格式
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(filename)s:%(lineno)d - %(message)s'
    )

    # 配置根日志记录器
    logger = logging.getLogger()
    logger.setLevel(numeric_level)

    # 清除现有的处理器
    for handler in logger.handlers[:]:
        logger.removeHandler(handler)

    # 添加文件处理器
    file_handler = logging.handlers.RotatingFileHandler(
        log_file,
        maxBytes=10*1024*1024,  # 10MB
        backupCount=5,
        encoding='utf-8'
    )
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)

    # 添加控制台处理器
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    # 创建独立的邮件日志记录器
    mail_logger = logging.getLogger('mail')
    mail_logger.setLevel(logging.ERROR)

    # 只记录错误级别的邮件日志到单独文件
    mail_file_handler = logging.handlers.RotatingFileHandler(
        'logs/mail.log',
        maxBytes=5*1024*1024,
        backupCount=3,
        encoding='utf-8'
    )
    mail_file_handler.setFormatter(formatter)
    mail_logger.addHandler(mail_file_handler)
    mail_logger.propagate = False

def get_logger(name: str) -> logging.Logger:
    """
    获取指定名称的日志记录器

    Args:
        name: 日志记录器名称

    Returns:
        配置好的日志记录器
    """
    return logging.getLogger(name)

class DomainCrawlerLogger:
    """域名爬虫专用日志类"""

    def __init__(self, name: str):
        self.logger = get_logger(name)

    def debug(self, message: str, extra: dict = None):
        """记录调试信息"""
        self.logger.debug(message, extra=extra)

    def info(self, message: str, extra: dict = None):
        """记录一般信息"""
        self.logger.info(message, extra=extra)

    def warning(self, message: str, extra: dict = None):
        """记录警告信息"""
        self.logger.warning(message, extra=extra)

    def error(self, message: str, extra: dict = None):
        """记录错误信息"""
        self.logger.error(message, extra=extra)

    def critical(self, message: str, extra: dict = None):
        """记录严重错误信息"""
        self.logger.critical(message, extra=extra)

    def log_crawling_start(self, domain: str):
        """记录爬虫开始"""
        self.info(f"开始爬取域名: {domain}")

    def log_crawling_success(self, domain: str, content_length: int):
        """记录爬虫成功"""
        self.info(f"域名 {domain} 爬取成功，内容长度: {content_length} 字符")

    def log_crawling_failure(self, domain: str, error: str):
        """记录爬虫失败"""
        self.error(f"域名 {domain} 爬取失败: {error}")

    def log_violation_found(self, domain: str, violation_type: str, reasons: list):
        """记录发现违规内容"""
        self.warning(f"域名 {domain} 发现违规内容 - 类型: {violation_type}, 原因: {reasons}")

    def log_notification_sent(self, violations_count: int):
        """记录通知发送"""
        self.info(f"已发送 {violations_count} 个违规通知")

    def log_database_operation(self, operation: str, count: int = None):
        """记录数据库操作"""
        if count is not None:
            self.info(f"数据库操作: {operation}, 影响记录数: {count}")
        else:
            self.info(f"数据库操作: {operation}")

# 测试函数
if __name__ == "__main__":
    # 设置日志
    setup_logging('DEBUG', 'logs/test.log')

    # 测试日志记录
    logger = DomainCrawlerLogger('test_logger')

    logger.debug("这是调试信息")
    logger.info("这是一般信息")
    logger.warning("这是警告信息")
    logger.error("这是错误信息")

    # 测试专用方法
    logger.log_crawling_start("example.com")
    logger.log_crawling_success("example.com", 1024)
    logger.log_crawling_failure("example.com", "连接超时")
    logger.log_violation_found("example.com", "政治敏感", ["敏感关键词1", "敏感关键词2"])
    logger.log_notification_sent(3)
    logger.log_database_operation("查询域名记录", 15)