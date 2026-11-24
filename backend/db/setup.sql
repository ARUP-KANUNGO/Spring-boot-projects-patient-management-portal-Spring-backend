-- Run once as a MySQL administrator (e.g. `mysql -u root -p < backend/db/setup.sql`).
-- Replace CHANGE_ME with your own password and put the same value in backend/.env (DB_PASSWORD).
CREATE DATABASE IF NOT EXISTS elrics_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'elrics_user'@'localhost' IDENTIFIED BY 'CHANGE_ME';
GRANT ALL PRIVILEGES ON elrics_portal.* TO 'elrics_user'@'localhost';

-- Optional: only needed to run the MySQL-backed integration tests with this user.
-- CREATE DATABASE IF NOT EXISTS elrics_portal_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- GRANT ALL PRIVILEGES ON elrics_portal_test.* TO 'elrics_user'@'localhost';
FLUSH PRIVILEGES;
