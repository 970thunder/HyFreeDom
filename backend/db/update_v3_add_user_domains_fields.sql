-- Update for adding missing fields to user_domains table
-- Supports NS record type handling and UI enhancements

-- Add missing fields to user_domains table
ALTER TABLE user_domains
ADD COLUMN record_type VARCHAR(10) AFTER remark,
ADD COLUMN record_value VARCHAR(255) AFTER record_type,
ADD COLUMN record_ttl INT AFTER record_value;

-- Update existing records to populate these fields
UPDATE user_domains ud
JOIN dns_records dr ON ud.dns_record_id = dr.id
SET ud.record_type = dr.type,
    ud.record_value = dr.content,
    ud.record_ttl = dr.ttl;

-- For NS records that might have multiple user_domain entries
UPDATE user_domains
SET record_type = 'NS'
WHERE id IN (
    SELECT ud.id FROM user_domains ud
    JOIN dns_records dr ON ud.dns_record_id = dr.id
    WHERE dr.type = 'NS'
);

COMMENT ON COLUMN user_domains.record_type IS 'DNS记录类型（冗余存储，便于查询）';
COMMENT ON COLUMN user_domains.record_value IS 'DNS记录值（冗余存储，便于查询）';
COMMENT ON COLUMN user_domains.record_ttl IS 'DNS记录TTL（冗余存储，便于查询）';