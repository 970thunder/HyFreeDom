-- Update for Real-name Authentication and IP Tracking

-- Add ip_address to users table
ALTER TABLE users ADD COLUMN ip_address VARCHAR(50);

-- Create user_profiles table
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id BIGINT PRIMARY KEY,
    real_name VARCHAR(255), -- Encrypted
    id_card VARCHAR(255),   -- Encrypted
    is_verified TINYINT DEFAULT 0,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
