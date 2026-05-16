-- ============================================================
-- ConnectHub Database Initialization Script
-- Creates all databases needed by the microservices
-- ============================================================

CREATE DATABASE IF NOT EXISTS connecthub_auth;
CREATE DATABASE IF NOT EXISTS connecthub_rooms;
CREATE DATABASE IF NOT EXISTS connecthub_messages;
CREATE DATABASE IF NOT EXISTS connecthub_users;
CREATE DATABASE IF NOT EXISTS connecthub_notifications;

-- Grant privileges (if you use a non-root user in production)
-- CREATE USER IF NOT EXISTS 'connecthub'@'%' IDENTIFIED BY 'your_password';
-- GRANT ALL PRIVILEGES ON connecthub_auth.* TO 'connecthub'@'%';
-- GRANT ALL PRIVILEGES ON connecthub_rooms.* TO 'connecthub'@'%';
-- GRANT ALL PRIVILEGES ON connecthub_messages.* TO 'connecthub'@'%';
-- GRANT ALL PRIVILEGES ON connecthub_users.* TO 'connecthub'@'%';
-- GRANT ALL PRIVILEGES ON connecthub_notifications.* TO 'connecthub'@'%';
-- FLUSH PRIVILEGES;
