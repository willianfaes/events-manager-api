-- Create the 'addresses' table as a reusable entity
CREATE TABLE event_addresses
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    street       VARCHAR(255) NOT NULL,
    number       VARCHAR(50)  NOT NULL,
    complement   VARCHAR(255),
    neighborhood VARCHAR(255) NOT NULL,
    city         VARCHAR(255) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    postal_code  VARCHAR(20)  NOT NULL,
    country      VARCHAR(100) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE

);

-- Create the 'addresses' table as a reusable entity
CREATE TABLE user_addresses
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    street       VARCHAR(255) NOT NULL,
    number       VARCHAR(50)  NOT NULL,
    complement   VARCHAR(255),
    neighborhood VARCHAR(255) NOT NULL,
    city         VARCHAR(255) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    postal_code  VARCHAR(20)  NOT NULL,
    country      VARCHAR(100) NOT NULL
);

-- Create the 'users' table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INT,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (address_id) REFERENCES user_addresses(id)
);

-- Create the 'events' table
CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address_id BIGINT NOT NULL,
    category ENUM('PARTY', 'SPORT', 'CONCERT', 'CULTURAL'),
    schedule DATETIME NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (address_id) REFERENCES event_addresses(id) -- Removed ON DELETE CASCADE for soft delete strategy
);

-- Create the 'event_registrations' associative entity table
CREATE TABLE event_registrations ( -- Renamed for consistency with previous discussion
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE (user_id, event_id), -- Ensures a user can register for an event only once
    FOREIGN KEY (user_id) REFERENCES users(id), -- Removed ON DELETE CASCADE for soft delete strategy
    FOREIGN KEY (event_id) REFERENCES events(id) -- Removed ON DELETE CASCADE for soft delete strategy
);