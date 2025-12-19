CREATE TABLE IF NOT EXISTS `featured_sites` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '网站名称',
  `url` varchar(255) NOT NULL COMMENT '网站链接',
  `logo_url` varchar(255) DEFAULT NULL COMMENT '网站Logo链接',
  `description` varchar(500) DEFAULT NULL COMMENT '网站描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐网站表';
