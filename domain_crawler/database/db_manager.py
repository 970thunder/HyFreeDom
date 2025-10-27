#!/usr/bin/env python3
"""
数据库管理模块
负责连接数据库并获取域名记录
"""

import asyncio
import aiomysql
import logging
from typing import List, Dict, Any
from config.config import config

class DatabaseManager:
    """数据库管理器"""

    def __init__(self, db_config):
        self.db_config = db_config
        self.pool = None
        self.logger = logging.getLogger(__name__)

    async def connect(self):
        """连接数据库"""
        try:
            self.pool = await aiomysql.create_pool(
                host=self.db_config['host'],
                port=self.db_config['port'],
                user=self.db_config['user'],
                password=self.db_config['password'],
                db=self.db_config['database'],
                charset=self.db_config['charset'],
                autocommit=True
            )
            self.logger.info("数据库连接成功")
        except Exception as e:
            self.logger.error(f"数据库连接失败: {e}")
            raise

    async def close(self):
        """关闭数据库连接"""
        if self.pool:
            self.pool.close()
            await self.pool.wait_closed()
            self.logger.info("数据库连接已关闭")

    async def get_subdomains(self) -> List[Dict[str, Any]]:
        """获取所有二级域名记录

        需要查询UserDomain表获取用户注册的域名信息
        """
        domains = []

        try:
            async with self.pool.acquire() as conn:
                async with conn.cursor(aiomysql.DictCursor) as cursor:
                    # 查询UserDomain表中的所有域名记录
                    await cursor.execute("""
                        SELECT ud.id, ud.domain_name as name, ud.status, ud.created_at,
                               u.username as owner_username
                        FROM user_domain ud
                        LEFT JOIN auth_user u ON ud.user_id = u.id
                        WHERE ud.status = 'active'
                        ORDER BY ud.created_at DESC
                    """)

                    result = await cursor.fetchall()

                    # 构造完整的域名URL
                    for row in result:
                        domain_info = {
                            'id': row['id'],
                            'name': row['name'],
                            'status': row['status'],
                            'created_at': row['created_at'],
                            'owner': row['owner_username'],
                            'url': f"http://{row['name']}"  # 默认HTTP协议
                        }
                        domains.append(domain_info)

                    self.logger.info(f"从数据库获取到 {len(domains)} 个活跃域名")

        except Exception as e:
            self.logger.error(f"获取域名记录失败: {e}")
            raise

        return domains

    async def get_dns_records(self) -> List[Dict[str, Any]]:
        """获取所有DNS记录

        如果需要从DnsRecord表获取记录，可以使用这个函数
        """
        records = []

        try:
            async with self.pool.acquire() as conn:
                async with conn.cursor(aiomysql.DictCursor) as cursor:
                    # 查询DnsRecord表中的记录
                    await cursor.execute("""
                        SELECT dr.name, dr.type, dr.content, dr.ttl, dr.proxied,
                               z.name as zone_name, dr.created_at
                        FROM dns_record dr
                        LEFT JOIN zone z ON dr.zone_id = z.id
                        WHERE dr.type IN ('A', 'CNAME', 'AAAA')
                        ORDER BY dr.created_at DESC
                    """)

                    result = await cursor.fetchall()

                    for row in result:
                        record_info = {
                            'name': row['name'],
                            'type': row['type'],
                            'content': row['content'],
                            'ttl': row['ttl'],
                            'proxied': row['proxied'],
                            'zone_name': row['zone_name'],
                            'created_at': row['created_at']
                        }
                        records.append(record_info)

                    self.logger.info(f"从数据库获取到 {len(records)} 个DNS记录")

        except Exception as e:
            self.logger.error(f"获取DNS记录失败: {e}")

        return records

    async def add_violation_record(self, domain: str, violation_type: str,
                                 content_sample: str, reasons: str) -> bool:
        """添加违规记录到数据库"""
        try:
            async with self.pool.acquire() as conn:
                async with conn.cursor() as cursor:
                    await cursor.execute("""
                        INSERT INTO domain_violations
                        (domain_name, violation_type, content_sample, reasons, created_at)
                        VALUES (%s, %s, %s, %s, NOW())
                    """, (domain, violation_type, content_sample, str(reasons)))

                    self.logger.info(f"已记录域名 {domain} 的违规信息")
                    return True

        except Exception as e:
            self.logger.error(f"添加违规记录失败: {e}")
            return False

    async def update_domain_status(self, domain: str, status: str) -> bool:
        """更新域名状态"""
        try:
            async with self.pool.acquire() as conn:
                async with conn.cursor() as cursor:
                    await cursor.execute("""
                        UPDATE user_domain
                        SET status = %s, updated_at = NOW()
                        WHERE domain_name = %s
                    """, (status, domain))

                    self.logger.info(f"已更新域名 {domain} 状态为 {status}")
                    return True

        except Exception as e:
            self.logger.error(f"更新域名状态失败: {e}")
            return False

# 测试函数
async def test_db_connection():
    """测试数据库连接"""
    db_manager = DatabaseManager(config.DB_CONFIG)

    try:
        await db_manager.connect()
        domains = await db_manager.get_subdomains()
        print(f"找到 {len(domains)} 个域名:")
        for domain in domains[:5]:  # 只显示前5个
            print(f"  - {domain['name']} (所有者: {domain.get('owner', 'N/A')})")

        await db_manager.close()
    except Exception as e:
        print(f"测试失败: {e}")

if __name__ == "__main__":
    asyncio.run(test_db_connection())