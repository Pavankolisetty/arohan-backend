-- Local development only. Cloud providers create the database and credentials.
CREATE DATABASE IF NOT EXISTS arohan
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'arohan_app'@'localhost'
    IDENTIFIED BY 'arohan_local';
ALTER USER 'arohan_app'@'localhost'
    IDENTIFIED BY 'arohan_local';

CREATE USER IF NOT EXISTS 'arohan_app'@'127.0.0.1'
    IDENTIFIED BY 'arohan_local';
ALTER USER 'arohan_app'@'127.0.0.1'
    IDENTIFIED BY 'arohan_local';

GRANT ALL PRIVILEGES ON arohan.* TO 'arohan_app'@'localhost';
GRANT ALL PRIVILEGES ON arohan.* TO 'arohan_app'@'127.0.0.1';
FLUSH PRIVILEGES;

