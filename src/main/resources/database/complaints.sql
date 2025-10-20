-- Create database
CREATE DATABASE IF NOT EXISTS comp;
USE comp;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    role ENUM('STUDENT','ADMIN','DEPARTMENT') DEFAULT 'STUDENT',
    department VARCHAR(100)
);

-- Complaints table
CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(255),
    category VARCHAR(50),
    description TEXT,
    status VARCHAR(50) DEFAULT 'Pending',
    department_confirmed BOOLEAN DEFAULT FALSE,
    department_email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Facilities department table
CREATE TABLE IF NOT EXISTS facilities_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT,
    department_status ENUM('Pending', 'Confirmed', 'In Progress', 'Resolved') DEFAULT 'Pending',
    remarks TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(id)
);

-- Food Services department table
CREATE TABLE IF NOT EXISTS food_services_dept LIKE facilities_dept;

-- Academic department table
CREATE TABLE IF NOT EXISTS academic_dept LIKE facilities_dept;

-- Technology department table
CREATE TABLE IF NOT EXISTS technology_dept LIKE facilities_dept;

-- Transportation department table
CREATE TABLE IF NOT EXISTS transportation_dept LIKE facilities_dept;

-- Other department table
CREATE TABLE IF NOT EXISTS other_dept LIKE facilities_dept;

-- Alter existing tables to align with application entities (idempotent)
ALTER TABLE users MODIFY COLUMN role ENUM('STUDENT','ADMIN','DEPARTMENT') DEFAULT 'STUDENT';
ALTER TABLE users ADD COLUMN IF NOT EXISTS department VARCHAR(100);

ALTER TABLE complaints ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP NULL DEFAULT NULL;
