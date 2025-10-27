#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
域名爬虫配置文件
支持环境变量配置，更安全灵活
"""

import os
from typing import Dict, Any

class Config:
    """配置管理类"""

    def __init__(self):
        # 应用配置
        self.LOG_LEVEL = os.getenv('CRAWLER_LOG_LEVEL', 'INFO')
        self.LOG_FILE = os.getenv('CRAWLER_LOG_FILE', 'logs/crawler.log')
        self.CRAWL_INTERVAL = int(os.getenv('CRAWL_INTERVAL', '5'))  # 分钟

        # 数据库配置
        self.DB_CONFIG = {
            'host': os.getenv('DB_HOST', 'localhost'),
            'port': int(os.getenv('DB_PORT', '3306')),
            'user': os.getenv('DB_USER', 'root'),
            'password': os.getenv('DB_PASSWORD', ''),
            'database': os.getenv('DB_NAME', 'domaindns'),
            'charset': 'utf8mb4'
        }

        # 爬虫配置
        self.CRAWLER_CONFIG = {
            'timeout': int(os.getenv('CRAWLER_TIMEOUT', '10')),
            'user_agent': os.getenv('CRAWLER_USER_AGENT',
                                   'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'),
            'max_retries': int(os.getenv('CRAWLER_MAX_RETRIES', '3')),
            'delay': float(os.getenv('CRAWLER_DELAY', '0.5')),
            'max_content_size': int(os.getenv('CRAWLER_MAX_CONTENT_SIZE', '1048576'))  # 1MB
        }

        # 内容分析配置
        self.ANALYZER_CONFIG = {
            'sensitive_keywords': self._load_sensitive_keywords(),
            'illegal_keywords': self._load_illegal_keywords(),
            'check_threshold': float(os.getenv('ANALYZER_THRESHOLD', '0.8'))
        }

        # 邮件配置
        self.EMAIL_CONFIG = {
            'host': os.getenv('SMTP_HOST', 'smtp.qq.com'),
            'port': int(os.getenv('SMTP_PORT', '587')),
            'username': os.getenv('SMTP_USER', '1010411661@qq.com'),
            'password': os.getenv('SMTP_PASSWORD', ''),
            'from_addr': os.getenv('SMTP_USER', '1010411661@qq.com'),
            'to_addrs': [os.getenv('SMTP_USER', '1010411661@qq.com')],
            'use_tls': True
        }

    def _load_sensitive_keywords(self) -> list:
        """加载敏感关键词"""
        sensitive_keywords = [
            # 政治敏感内容
            '反动', '反党', '反政府', '反社会', '分裂国家',
            '颠覆', '暴恐', '恐怖主义', '极端主义', '邪教',
            '法轮功', '藏独', '疆独', '台独',

            # 淫秽色情内容
            '色情', '淫秽', '成人', '情色', '性爱',
            'AV', '三级', 'A片', '黄片', '黄色网站',
            '成人视频', '成人图片', '裸体', '色情小说',

            # 赌博相关内容
            '赌博', '赌场', '博彩', '六合彩', '老虎机',
            '彩票', '网上赌博', '赌博网站', '赌球',
            '棋牌', '扑克', '麻将',

            # 诈骗相关内容
            '诈骗', '钓鱼', '假冒', '欺诈', '网络诈骗',
            '电信诈骗', '赌博平台', '假证件', '假发票',
            '代办', '代考', '枪手',

            # 毒品相关内容
            '毒品', '吸毒', '贩毒', '大麻', '冰毒',
            '摇头丸', 'K粉', '可卡因', '海洛因',

            # 暴力恐怖内容
            '暴力', '血腥', '恐怖', '杀人', '抢劫',
            '爆炸', '武器', '枪支', '炸药', '管制刀具',

            # 违规内容
            '黑客', '破解', '木马', '病毒', '恶意软件'
        ]

        # 从环境变量加载额外关键词
        additional_keywords = os.getenv('SENSITIVE_KEYWORDS', '')
        if additional_keywords:
            sensitive_keywords.extend(additional_keywords.split(','))

        return sensitive_keywords

    def _load_illegal_keywords(self) -> list:
        """加载违法关键词"""
        illegal_keywords = [
            # 国家机密相关
            '国家机密', '军事机密', '国防机密', '国家安全',

            # 法律法规禁止的内容
            '非法集资', '传销', '洗钱', '假币',
            '走私', '逃税', '偷税', '骗税',

            # 社会秩序
            '扰乱社会秩序', '聚众闹事', '非法集会',
            '群体性事件', '维权', '上访',

            # 其他违法内容
            '侵权', '盗版', '造假', '伪劣产品',
            '虚假宣传', '不正当竞争'
        ]

        # 从环境变量加载额外关键词
        additional_keywords = os.getenv('ILLEGAL_KEYWORDS', '')
        if additional_keywords:
            illegal_keywords.extend(additional_keywords.split(','))

        return illegal_keywords

    def validate(self) -> bool:
        """验证配置的有效性"""
        errors = []

        # 检查数据库配置
        if not self.DB_CONFIG['host'] or not self.DB_CONFIG['database']:
            errors.append("数据库配置不完整")

        # 检查邮件配置
        if not self.EMAIL_CONFIG['password']:
            errors.append("SMTP密码未配置")

        if errors:
            print("配置验证失败:")
            for error in errors:
                print(f"  - {error}")
            return False

        return True

# 全局配置实例
config = Config()

def get_db_url():
    """获取数据库连接URL"""
    cfg = config.DB_CONFIG
    return f"mysql+pymysql://{cfg['user']}:{cfg['password']}@{cfg['host']}:{cfg['port']}/{cfg['database']}?charset={cfg['charset']}"

def ensure_directories():
    """确保必要的目录存在"""
    directories = [
        'logs',
        'data',
        'data/cache',
        'data/backup'
    ]
    for directory in directories:
        os.makedirs(directory, exist_ok=True)

# 初始化时确保目录存在
ensure_directories()

if __name__ == "__main__":
    print("配置项:")
    print(f"数据库主机: {config.DB_CONFIG['host']}")
    print(f"数据库: {config.DB_CONFIG['database']}")
    print(f"爬虫间隔: {config.CRAWL_INTERVAL} 分钟")
    print(f"敏感关键词数量: {len(config.ANALYZER_CONFIG['sensitive_keywords'])}")
    print(f"违法关键词数量: {len(config.ANALYZER_CONFIG['illegal_keywords'])}")

    if not config.validate():
        print("请配置必要的环境变量")
    else:
        print("配置验证通过")