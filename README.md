# E-Learning Platform - Backend

This repository contains the backend component of an e-learning platform built as an engineering thesis project. The application enables users to create accounts, publish courses, purchase access to educational content, leave reviews, and interact through a points-based economy system.

## Table of Contents

- [Technologies](#technologies)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Setup & Configuration](#setup--configuration)
    - [Database Setup](#database-setup)
    - [IntelliJ Database Configuration](#intellij-database-configuration)
    - [Application Properties](#application-properties)
    - [Email Configuration](#email-configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Security](#security)

## Technologies

- Java 22
- Spring Boot 3.3.2
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT Authentication
- Maven
- Hibernate
- Caffeine Cache
- JavaMail API

## Features

- **User Management**: Registration, authentication, email verification, profile management
- **Role-Based Authorization**: Different access levels (User, Verified, Teacher, Admin)
- **Course Management**: Creation, updating, and publishing educational content
- **Content Organization**: Courses, chapters, subchapters, and various content items (text, quiz, image, video)
- **Points System**: Virtual currency for purchasing courses and teacher payouts
- **Reviews & Ratings**: Users can rate and review courses, chapters, and teachers
- **Caching System**: Performance optimization using Caffeine cache
- **Security**: JWT-based authentication, brute force prevention, password encryption

## Prerequisites

- Java JDK 22
- PostgreSQL 16 or newer
- IntelliJ IDEA (recommended)
- SMTP Server access (for email verification)
- Maven 3.8+ (if not using IntelliJ's embedded Maven)

## Setup & Configuration

### Database Setup

- Install PostgreSQL if you don't have it already
- Create a new database:
  ```sql
  CREATE DATABASE korki_online;
  ```
- No need to create tables manually - the application uses the included schema.sql file to automatically generate the database schema at first startup

### IntelliJ Database Configuration

- Open IntelliJ IDEA and navigate to Database tool window (or press Alt+1, then select Database)
- Click on the + icon and select Data Source → PostgreSQL
- Configure the connection with these settings:
  ```
  Name: korki_online@localhost
  Host: localhost
  Port: 5432
  Database: korki_online
  User: postgres (or your PostgreSQL username)
  Password: your PostgreSQL password
  ```
- Click Test Connection to verify everything is working
- Navigate to the Schemas tab and make sure demo schema is selected
- Click OK to save the configuration

### Application Properties

Configure your application.properties file in src/main/resources/ with the following settings (adjust as needed for your environment):

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/korki_online
spring.datasource.username=postgres
spring.datasource.password=your_password

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.generate-ddl=false
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.globally_quoted_identifiers=true
spring.jpa.properties.hibernate.format_sql=true

# Initialize schema
spring.sql.init.mode=always

# JWT Configuration
jwt.accessTokenExpiration=86400000
jwt.refreshTokenExpiration=604800000

# Logging
logging.level.org.springframework.web=DEBUG
logging.level.com.example.demo=DEBUG

# Caching
spring.cache.type=caffeine
```

### Email Configuration

For the email verification system to work, configure the SMTP settings in application.properties:

```properties
# Email (SMTP) Configuration
spring.mail.host=your_smtp_server
spring.mail.port=587
spring.mail.username=your_email@example.com
spring.mail.password=your_email_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

If using Gmail as your SMTP provider:

- Use smtp.gmail.com as the host
- Enable 2FA on your Google account
- Generate an App Password for this application
- Use this App Password instead of your regular Gmail password

## Running the Application

### From IntelliJ IDEA

- Import the project into IntelliJ IDEA
- Make sure you have configured the database as described above
- Navigate to com.example.demo.DemoApplication.java
- Right-click and select Run DemoApplication

### Using Maven

- Navigate to the project directory in a terminal
- Run: `mvn spring-boot:run`

The application should now be running at http://localhost:8080

## API Documentation

The main API endpoints are organized by controllers:

- `/user/**` - User management endpoints (registration, login, profile)
- `/course/**` - Course management and discovery
- `/chapter/**` - Chapter management within courses
- `/subchapter/**` - Subchapter management
- `/review/**` - Review creation and retrieval
- `/points/**` - Points system management (buying/withdrawing)

Each controller implements proper error handling and follows REST principles.

## Security

The application implements several security features:

- Password encryption using BCrypt
- JWT-based authentication
- Brute force prevention with login attempt tracking
- Role-based access control
- Email verification for new accounts
- HTTPS support (configure SSL in production)

---

This application is part of an engineering thesis project. For more information or assistance, please refer to the source code documentation or contact the repository owner.