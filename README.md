# Klaro - Collaborative Project Management System

## 📋 Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Setup Instructions](#setup-instructions)
- [API Documentation](#api-documentation)
  - [Authentication APIs](#authentication-apis)
  - [Group Management APIs](#group-management-apis)
  - [Project Management APIs](#project-management-apis)
  - [Issue Tracking APIs](#issue-tracking-apis)
  - [Comment APIs](#comment-apis)
  - [AI Integration APIs](#ai-integration-apis)
  - [Notification APIs](#notification-apis)
  - [Admin APIs](#admin-apis)
- [Database Schema](#database-schema)
- [Security](#security)
- [Error Handling](#error-handling)
- [Quick Start Guide](#quick-start-guide)

## 🚀 Overview

Klaro is a comprehensive collaborative project management system designed for educational and professional environments. It enables users to create groups, manage projects, track issues, collaborate through comments, and leverage AI assistance for project management tasks.

### Key Capabilities
- **User Management**: Secure authentication with JWT tokens and role-based access control
- **Group Collaboration**: Create and manage collaborative groups with member management
- **Project Management**: Organize projects within groups with proper access controls
- **Issue Tracking**: Full-featured issue tracking with priorities, status, and assignments
- **AI Integration**: OpenRouter AI for intelligent project assistance and suggestions
- **Real-time Notifications**: Keep users updated on project activities
- **Admin Panel**: Administrative functions for user and system management

## ✨ Features

### 🔐 Authentication & Authorization
- User registration and login with email validation
- JWT-based stateless authentication with refresh tokens
- Role-based access control (ADMIN/USER)
- Secure password encryption with BCrypt
- Token refresh mechanism for extended sessions

### 👥 Group Management
- Create and manage collaborative groups (Admin only)
- Owner-based permission system
- Add/remove group members with batch operations
- Search functionality for groups
- Member count and project tracking

### 📊 Project Management
- Create projects individually or within groups (Admin only)
- Users can view projects in their groups
- Project-group association tracking
- Creation timestamp and ownership tracking

### 🎯 Issue Tracking
- Comprehensive issue management with full CRUD operations
- Priority levels: LOW, MEDIUM, HIGH
- Status tracking: TO_DO, IN_PROGRESS, DONE
- Issue types: BUG, FEATURE, TASK
- Assignment capabilities with user restrictions
- Project-based issue organization

### 💬 Communication
- Comment system for issues with author tracking
- Notification system for updates and assignments
- Real-time collaboration features

### 🤖 AI Integration
- OpenRouter AI integration for intelligent content generation
- Project assistance and suggestions based on issue descriptions
- Natural language processing for project insights

### 👑 Admin Features
- User management and role assignment
- System statistics and analytics
- Bulk member management operations
- Global access to all system resources

## 🛠 Technology Stack

### Backend
- **Java 17+**
- **Spring Boot 3.3.3**
- **Spring Security 6.x** with JWT
- **Spring Data JPA** with Hibernate ORM
- **PostgreSQL Database**
- **Flyway** for database migrations

### Security & Authentication
- **JWT (JSON Web Tokens)** with refresh token support
- **BCrypt Password Encryption**
- **Role-based Access Control**
- **CORS Configuration** for cross-origin requests

### External APIs
- **OpenRouter AI API** for intelligent assistance
- **OkHttp3** for HTTP client operations

### Tools & Libraries
- **Lombok** for code generation and boilerplate reduction
- **Bean Validation** for input validation
- **Jackson** for JSON processing

## 📦 Setup Instructions

### Prerequisites
- Java 17 or higher
- Gradle 7+ 
- PostgreSQL 12+ 
- OpenRouter AI API Key (optional)

### 1. Clone the Repository
```bash
git clone <your-repository-url>
cd Klaro
```

### 2. Configure Database
Create PostgreSQL database:
```sql
CREATE DATABASE klaro;
CREATE USER klaro_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE klaro TO klaro_user;
```

Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/klaro
spring.datasource.username=klaro_user
spring.datasource.password=your_password
```

### 3. Configure API Keys
Add to `application.properties`:
```properties
# JWT Configuration (generate a secure 512-bit key)
jwt.secret=your_base64_encoded_secret_key

# OpenRouter AI Configuration (optional)
openrouter.api.key=your_openrouter_api_key
```

### 4. Run Database Migrations
```bash
./gradlew flywayMigrate
```

### 5. Build and Run
```bash
./gradlew build
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Base URL
```
http://localhost:8080
```

### Authentication Header
For protected endpoints, include:
```
Authorization: Bearer <your_jwt_token>
```

---

## 🔐 Authentication APIs

### 1. User Registration
**Endpoint:** `POST /auth/signup`

**Description:** Register a new user with role selection

**Request Body:**
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "role": "ROLE_USER"
}
```

**Validation Rules:**
- `fullName`: 2-255 characters, required
- `email`: Valid email format, unique
- `password`: Minimum 6 characters
- `confirmPassword`: Must match password
- `role`: "ROLE_ADMIN" or "ROLE_USER" (defaults to "ROLE_USER")

**Response:**
```json
"User registered successfully!"
```

**Status Codes:**
- `200 OK`: User registered successfully
- `400 Bad Request`: Validation errors or email already exists

---

### 2. User Login
**Endpoint:** `POST /auth/signin`

**Description:** Authenticate user and receive access & refresh tokens

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "uuid-refresh-token",
  "tokenType": "Bearer"
}
```

**Status Codes:**
- `200 OK`: Login successful
- `400 Bad Request`: Invalid credentials

---

### 3. Refresh Token
**Endpoint:** `POST /auth/refresh`

**Description:** Get a new access token using refresh token

**Request Body:**
```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**Response:**
```json
{
  "accessToken": "new_jwt_token",
  "refreshToken": "same_refresh_token",
  "tokenType": "Bearer"
}
```

**Status Codes:**
- `200 OK`: Token refreshed successfully
- `400 Bad Request`: Invalid or expired refresh token

---

### 4. Get Current User
**Endpoint:** `GET /auth/user`

**Description:** Get information about the currently authenticated user

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
{
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "ROLE_USER"
}
```

**Status Codes:**
- `200 OK`: User information retrieved
- `401 Unauthorized`: Invalid or missing token

---

### 5. Logout
**Endpoint:** `POST /auth/logout`

**Description:** Invalidate the user's refresh token

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
"Logout successful"
```

**Status Codes:**
- `200 OK`: Logged out successfully
- `401 Unauthorized`: Invalid or missing token

---

## 👥 Group Management APIs

### Role-Based Access Control
- **Admins**: Can create groups, manage all groups and members
- **Users**: Can only view groups they are members of

### 1. Create Group
**Endpoint:** `POST /groups`

**Description:** Create a new collaborative group (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Request Body:**
```json
{
  "name": "Development Team",
  "description": "Main development team for the project",
  "memberEmails": [
    "developer1@example.com",
    "developer2@example.com"
  ]
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Development Team",
  "description": "Main development team for the project",
  "ownerName": "Admin User",
  "ownerEmail": "admin@example.com",
  "members": [
    {
      "id": 1,
      "fullName": "Admin User",
      "email": "admin@example.com",
      "owner": true
    },
    {
      "id": 2,
      "fullName": "Developer One",
      "email": "developer1@example.com",
      "owner": false
    }
  ],
  "createdAt": "2025-08-29T10:30:00",
  "updatedAt": "2025-08-29T10:30:00",
  "memberCount": 3,
  "projectCount": 0
}
```

**Status Codes:**
- `201 Created`: Group created successfully
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can create groups

---

### 2. Get User's Groups
**Endpoint:** `GET /groups`

**Description:** Get all groups where the current user is owner or member

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Development Team",
    "description": "Main development team for the project",
    "ownerName": "Admin User",
    "ownerEmail": "admin@example.com",
    "memberCount": 3,
    "projectCount": 2
  }
]
```

**Status Codes:**
- `200 OK`: Groups retrieved successfully
- `401 Unauthorized`: Invalid token

---

### 3. Get All Groups
**Endpoint:** `GET /groups/all`

**Description:** Get all groups in the system (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Development Team",
    "memberCount": 3,
    "projectCount": 2
  },
  {
    "id": 2,
    "name": "Testing Team",
    "memberCount": 2,
    "projectCount": 1
  }
]
```

**Status Codes:**
- `200 OK`: Groups retrieved successfully
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can view all groups

---

### 4. Get Group by ID
**Endpoint:** `GET /groups/{groupId}`

**Description:** Get detailed information about a specific group

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
- `groupId`: ID of the group to retrieve

**Response:**
```json
{
  "id": 1,
  "name": "Development Team",
  "description": "Main development team for the project",
  "ownerName": "Admin User",
  "ownerEmail": "admin@example.com",
  "members": [
    {
      "id": 1,
      "fullName": "Admin User",
      "email": "admin@example.com",
      "owner": true
    }
  ]
}
```

**Status Codes:**
- `200 OK`: Group retrieved successfully
- `401 Unauthorized`: Invalid token
- `404 Not Found`: Group not found

---

### 5. Search Groups
**Endpoint:** `GET /groups/search`

**Description:** Search for groups by name or description

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**
- `query`: Search term (searches in name and description)

**Example:** `GET /groups/search?query=development`

**Response:**
```json
[
  {
    "id": 1,
    "name": "Development Team",
    "description": "Main development team for the project"
  }
]
```

**Status Codes:**
- `200 OK`: Search completed successfully
- `401 Unauthorized`: Invalid token

---

### 6. Add Member to Group
**Endpoint:** `POST /groups/{groupId}/members`

**Description:** Add a member to an existing group (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Path Parameters:**
- `groupId`: ID of the group

**Query Parameters:**
- `memberEmail`: Email of user to add

**Example:** `POST /groups/1/members?memberEmail=newuser@example.com`

**Response:**
```json
{
  "id": 1,
  "name": "Development Team",
  "memberCount": 4,
  "members": [
    {
      "id": 1,
      "fullName": "Admin User",
      "email": "admin@example.com",
      "owner": true
    },
    {
      "id": 3,
      "fullName": "New User",
      "email": "newuser@example.com",
      "owner": false
    }
  ]
}
```

**Status Codes:**
- `200 OK`: Member added successfully
- `400 Bad Request`: User already a member or user not found
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can add members

---

### 7. Add Multiple Members to Group
**Endpoint:** `POST /groups/{groupId}/members/batch`

**Description:** Add multiple members to a group at once (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Path Parameters:**
- `groupId`: ID of the group

**Request Body:**
```json
[
  "user1@example.com",
  "user2@example.com",
  "user3@example.com"
]
```

**Response:**
```json
{
  "id": 1,
  "name": "Development Team",
  "memberCount": 6,
  "members": [
    {
      "id": 1,
      "fullName": "Admin User",
      "email": "admin@example.com",
      "owner": true
    }
  ]
}
```

**Status Codes:**
- `200 OK`: Members added successfully
- `400 Bad Request`: Some users not found or already members
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can add members

---

### 8. Remove Member from Group
**Endpoint:** `DELETE /groups/{groupId}/members`

**Description:** Remove a member from a group (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Path Parameters:**
- `groupId`: ID of the group

**Query Parameters:**
- `memberEmail`: Email of user to remove

**Example:** `DELETE /groups/1/members?memberEmail=user@example.com`

**Response:**
```json
{
  "id": 1,
  "name": "Development Team",
  "memberCount": 2
}
```

**Status Codes:**
- `200 OK`: Member removed successfully
- `400 Bad Request`: User not a member, trying to remove owner
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can remove members

---

### 9. Delete Group
**Endpoint:** `DELETE /groups/{groupId}`

**Description:** Delete a group entirely (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Path Parameters:**
- `groupId`: ID of the group to delete

**Response:**
```json
"Group deleted successfully"
```

**Status Codes:**
- `200 OK`: Group deleted successfully
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can delete groups
- `404 Not Found`: Group not found

---

## 📊 Project Management APIs

### Role-Based Access Control
- **Admins**: Can create projects, view all projects, assign projects to groups
- **Users**: Can only view projects in groups they are members of

### 1. Create Project
**Endpoint:** `POST /projects`

**Description:** Create a new project (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Request Body:**
```json
{
  "name": "E-commerce Platform",
  "description": "Building a comprehensive e-commerce solution",
  "groupId": 1
}
```

**Response:**
```json
{
  "id": 1,
  "name": "E-commerce Platform",
  "description": "Building a comprehensive e-commerce solution",
  "createdBy": "Admin User",
  "createdAt": "2025-08-29T10:30:00"
}
```

**Status Codes:**
- `201 Created`: Project created successfully
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can create projects
- `404 Not Found`: Group not found (if groupId provided)

**Validation Rules:**
- `name`: 1-255 characters, required
- `description`: Max 1000 characters
- `groupId`: Optional, must be valid group ID

---

### 2. Get User's Projects
**Endpoint:** `GET /projects`

**Description:** Get projects accessible to the current user
- **Admins**: Get all projects they created
- **Users**: Get projects in groups they are members of

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "E-commerce Platform",
    "description": "Building a comprehensive e-commerce solution",
    "createdBy": "Admin User",
    "createdAt": "2025-08-29T10:30:00"
  }
]
```

**Status Codes:**
- `200 OK`: Projects retrieved successfully
- `401 Unauthorized`: Invalid token

---

### 3. Get All Projects
**Endpoint:** `GET /projects/all`

**Description:** Get all projects in the system (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "E-commerce Platform",
    "description": "Building a comprehensive e-commerce solution",
    "createdBy": "Admin User",
    "groupName": "Development Team",
    "createdAt": "2025-08-29T10:30:00"
  }
]
```

**Status Codes:**
- `200 OK`: Projects retrieved successfully
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can view all projects

---

### 4. Get Projects by Group
**Endpoint:** `GET /projects/groups/{groupId}/projects`

**Description:** Get all projects belonging to a specific group

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
- `groupId`: ID of the group

**Response:**
```json
[
  {
    "id": 1,
    "name": "E-commerce Platform",
    "createdBy": "Admin User",
    "groupName": "Development Team"
  }
]
```

**Status Codes:**
- `200 OK`: Projects retrieved successfully
- `401 Unauthorized`: Invalid token
- `404 Not Found`: Group not found

---

## 🎯 Issue Tracking APIs

### Role-Based Access Control
- **Admins**: Can create issues, assign to any user, view all issues
- **Users**: Can create issues only in projects where they are group members, can comment on issues

### 1. Create Issue
**Endpoint:** `POST /api/issues`

**Description:** Create a new issue for a project
- **Admins**: Can create issues in any project
- **Users**: Can create issues only in projects where they are group members

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "projectId": 1,
  "title": "User login not working",
  "description": "Users unable to login with correct credentials",
  "priority": "HIGH",
  "status": "TO_DO",
  "type": "BUG",
  "assigneeId": 2
}
```

**Response:**
```json
{
  "id": 1,
  "title": "User login not working",
  "status": "TO_DO",
  "priority": "HIGH",
  "assignedTo": "Jane Smith",
  "createdBy": "John Doe"
}
```

**Status Codes:**
- `200 OK`: Issue created successfully
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: No permission to create issues in this project
- `404 Not Found`: Project or assignee not found

**Enum Values:**
- **Priority**: `LOW`, `MEDIUM`, `HIGH`
- **Status**: `TO_DO`, `IN_PROGRESS`, `DONE`
- **Type**: `BUG`, `FEATURE`, `TASK`

---

### 2. Get Issues by Project
**Endpoint:** `GET /api/issues/projects/{projectId}/issues`

**Description:** Get all issues for a specific project

**Path Parameters:**
- `projectId`: ID of the project

**Response:**
```json
[
  {
    "id": 1,
    "title": "User login not working",
    "status": "TO_DO",
    "priority": "HIGH",
    "assignedTo": "Jane Smith",
    "createdBy": "John Doe"
  }
]
```

**Status Codes:**
- `200 OK`: Issues retrieved successfully
- `404 Not Found`: Project not found

---

### 3. Get Issues by Status
**Endpoint:** `GET /api/issues/issues`

**Description:** Get issues filtered by status (optional filter)

**Query Parameters:**
- `status` (optional): Filter by status (TO_DO, IN_PROGRESS, DONE)

**Example:** `GET /api/issues/issues?status=TO_DO`

**Response:**
```json
[
  {
    "id": 1,
    "title": "User login not working",
    "status": "TO_DO",
    "priority": "HIGH",
    "assignedTo": "Jane Smith",
    "createdBy": "John Doe"
  }
]
```

**Status Codes:**
- `200 OK`: Issues retrieved successfully
- `400 Bad Request`: Invalid status value

---

### 4. Get Issues Assigned to Me
**Endpoint:** `GET /api/issues/issues/assigned-to/me`

**Description:** Get all issues assigned to the current user

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": 1,
    "title": "User login not working",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "assignedTo": "Current User",
    "createdBy": "Admin User"
  }
]
```

**Status Codes:**
- `200 OK`: Issues retrieved successfully
- `401 Unauthorized`: Invalid token

---

## 💬 Comment APIs

### 1. Add Comment to Issue
**Endpoint:** `POST /comments/issue/{issueId}`

**Description:** Add a comment to an issue

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
- `issueId`: ID of the issue

**Request Body:**
```json
{
  "content": "I think this might be related to the authentication service"
}
```

**Response:**
```json
{
  "id": 1,
  "content": "I think this might be related to the authentication service",
  "author": "John Doe",
  "createdAt": "2025-08-29T10:30:00"
}
```

**Status Codes:**
- `200 OK`: Comment added successfully
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Invalid token
- `404 Not Found`: Issue not found

---

### 2. Get Comments for Issue
**Endpoint:** `GET /comments/issue/{issueId}`

**Description:** Get all comments for a specific issue

**Path Parameters:**
- `issueId`: ID of the issue

**Response:**
```json
[
  {
    "id": 1,
    "content": "I think this might be related to the authentication service",
    "author": "John Doe",
    "createdAt": "2025-08-29T10:30:00"
  },
  {
    "id": 2,
    "content": "Let me check the database connections",
    "author": "Jane Smith",
    "createdAt": "2025-08-29T11:00:00"
  }
]
```

**Status Codes:**
- `200 OK`: Comments retrieved successfully
- `404 Not Found`: Issue not found

---

## 🤖 AI Integration APIs

### 1. Generate AI Content
**Endpoint:** `POST /ai/generate`

**Description:** Generate AI-powered content using OpenRouter AI API

**Request Body:**
```json
{
  "issueDescription": "Our e-commerce website needs a shopping cart feature that allows users to add, remove, and modify quantities of products before checkout"
}
```

**Response:**
```json
{
  "aiSuggestion": "For implementing a shopping cart feature, consider the following approach:\n\n1. **Backend Implementation:**\n   - Create a Cart entity with user relationship\n   - Add CartItem entity for individual products\n   - Implement REST APIs for CRUD operations\n\n2. **Key Features:**\n   - Add/remove items\n   - Update quantities\n   - Calculate totals\n   - Session persistence\n\n3. **Database Schema:**\n   - cart table (id, user_id, created_at)\n   - cart_items table (id, cart_id, product_id, quantity, price)\n\n4. **Security Considerations:**\n   - Validate user ownership\n   - Sanitize input data\n   - Implement rate limiting"
}
```

**Status Codes:**
- `200 OK`: Content generated successfully
- `400 Bad Request`: Invalid request
- `500 Internal Server Error`: AI service error

**Note:** This endpoint is public and doesn't require authentication

---

## 🔔 Notification APIs

### 1. Get User Notifications
**Endpoint:** `GET /notifications/username`

**Description:** Get all notifications for the current user

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": 1,
    "recipient": "john.doe@example.com",
    "message": "You have been assigned to issue: User login not working",
    "read": false,
    "createdAt": "2025-08-29T10:30:00"
  }
]
```

**Status Codes:**
- `200 OK`: Notifications retrieved successfully
- `401 Unauthorized`: Invalid token

---

### 2. Get Unread Notification Count
**Endpoint:** `GET /notifications/unread-count`

**Description:** Get the count of unread notifications for the current user

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
5
```

**Status Codes:**
- `200 OK`: Count retrieved successfully
- `401 Unauthorized`: Invalid token

---

### 3. Mark Notification as Read
**Endpoint:** `POST /notifications/{notificationId}/read`

**Description:** Mark a specific notification as read

**Path Parameters:**
- `notificationId`: ID of the notification to mark as read

**Response:**
```json
"Marked as read"
```

**Status Codes:**
- `200 OK`: Notification marked as read
- `404 Not Found`: Notification not found

---

## 👑 Admin APIs

### Role-Based Access Control
All admin endpoints require `ROLE_ADMIN` role.

### 1. Get All Users
**Endpoint:** `GET /admin/users`

**Description:** Get all users in the system (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Response:**
```json
[
  {
    "email": "admin@example.com",
    "fullName": "Admin User",
    "role": "ROLE_ADMIN"
  },
  {
    "email": "user@example.com",
    "fullName": "Regular User",
    "role": "ROLE_USER"
  }
]
```

**Status Codes:**
- `200 OK`: Users retrieved successfully
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can view all users

---

### 2. Get Available Users for Group Assignment
**Endpoint:** `GET /admin/users/available`

**Description:** Get all users available for adding to a group (excludes current members)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Query Parameters:**
- `groupId` (optional): Filter out users who are already members of this group

**Example:** `GET /admin/users/available?groupId=1`

**Response:**
```json
[
  {
    "email": "available@example.com",
    "fullName": "Available User",
    "role": "ROLE_USER"
  }
]
```

**Status Codes:**
- `200 OK`: Available users retrieved successfully
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can view all users

---

### 3. Get System Statistics
**Endpoint:** `GET /admin/stats`

**Description:** Get system statistics and analytics (Admin only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Response:**
```json
"System statistics - placeholder for analytics"
```

**Status Codes:**
- `200 OK`: Statistics retrieved successfully
- `401 Unauthorized`: Invalid token
- `403 Forbidden`: Only admins can view system statistics

---

## 🗄 Database Schema

### Entity Relationships
```
User (1) ──── (N) Project [created_by]
User (1) ──── (N) Group [owner]
User (N) ──── (N) Group [members via group_members]
Group (1) ──── (N) Project
Project (1) ──── (N) Issue
User (1) ──── (N) Issue [created_by]
User (1) ──── (N) Issue [assigned_to]
Issue (1) ──── (N) Comment
User (1) ──── (N) Comment [author]
User (1) ──── (1) RefreshToken
```

### Key Tables

#### users
- **id**: Primary key (BIGSERIAL)
- **full_name**: VARCHAR(255), NOT NULL
- **email**: VARCHAR(255), UNIQUE, NOT NULL
- **password**: VARCHAR(255), NOT NULL (BCrypt hashed)
- **role**: VARCHAR(50), DEFAULT 'ROLE_USER'

#### groups
- **id**: Primary key (BIGSERIAL)
- **name**: VARCHAR(100), UNIQUE, NOT NULL
- **description**: VARCHAR(500)
- **owner_id**: Foreign key to users(id)
- **created_at**, **updated_at**: TIMESTAMP

#### group_members
- **group_id**, **user_id**: Composite primary key
- Foreign keys to groups(id) and users(id)

#### projects
- **id**: Primary key (BIGSERIAL)
- **name**: VARCHAR(255), NOT NULL
- **description**: VARCHAR(1000)
- **created_by**: Foreign key to users(id)
- **group_id**: Foreign key to groups(id)
- **created_at**: TIMESTAMP

#### issue
- **id**: Primary key (BIGSERIAL)
- **title**: VARCHAR(255), NOT NULL
- **description**: TEXT
- **priority**: ENUM('LOW', 'MEDIUM', 'HIGH')
- **status**: ENUM('TO_DO', 'IN_PROGRESS', 'DONE')
- **type**: ENUM('BUG', 'FEATURE', 'TASK')
- **created_by**, **assigned_to**: Foreign keys to users(id)
- **project_id**: Foreign key to projects(id)
- **created_at**: TIMESTAMP

#### comment
- **id**: Primary key (BIGSERIAL)
- **content**: TEXT, NOT NULL
- **author_id**: Foreign key to users(id)
- **issue_id**: Foreign key to issue(id)
- **created_at**: TIMESTAMP

#### notification
- **id**: Primary key (BIGSERIAL)
- **recipient**: VARCHAR(255), NOT NULL
- **message**: TEXT, NOT NULL
- **read**: BOOLEAN, DEFAULT FALSE
- **created_at**: TIMESTAMP

#### refresh_token
- **id**: Primary key (BIGSERIAL)
- **user_id**: Foreign key to users(id)
- **token**: VARCHAR(255), UNIQUE, NOT NULL
- **expiry_date**: TIMESTAMP, NOT NULL

## 🔒 Security

### Authentication & Authorization
- **JWT Tokens**: HS512 algorithm with 512-bit secret key
- **Access Token**: 24-hour expiry
- **Refresh Token**: 7-day expiry
- **Password Security**: BCrypt encryption with salt
- **Role-Based Access**: ADMIN and USER roles with different permissions

### API Security
- **Protected Endpoints**: Most endpoints require valid JWT token
- **Public Endpoints**: `/auth/signin`, `/auth/signup`, `/auth/refresh`, `/ai/generate`
- **Admin-Only Endpoints**: Group creation, project creation, user management
- **CORS Configuration**: Configured for specific origins

### Role-Based Permissions

| Feature | Admin | User |
|---------|-------|------|
| Create Group | ✅ | ❌ |
| View All Groups | ✅ | ❌ |
| View My Groups | ✅ | ✅ |
| Manage Group Members | ✅ | ❌ |
| Create Project | ✅ | ❌ |
| View All Projects | ✅ | ❌ |
| View My Projects | ✅ | ✅ (Group projects only) |
| Create Issue | ✅ | ✅ (If member of project group) |
| Assign Issue | ✅ | ❌ |
| Comment on Issues | ✅ | ✅ |
| View All Users | ✅ | ❌ |
| AI Suggestions | ✅ | ✅ |

## ⚠️ Error Handling

### Common HTTP Status Codes
- **200 OK**: Successful operation
- **201 Created**: Resource created successfully
- **400 Bad Request**: Validation errors or malformed request
- **401 Unauthorized**: Missing, invalid, or expired authentication token
- **403 Forbidden**: Insufficient permissions for the requested operation
- **404 Not Found**: Requested resource does not exist
- **500 Internal Server Error**: Server error or external service failure

### Error Response Format
```json
{
  "error": "Error message description",
  "timestamp": "2025-08-29T10:30:00",
  "status": 400
}
```

### Common Error Scenarios
1. **Invalid JWT Token**: Check token format and expiration
2. **Insufficient Permissions**: Ensure user has required role
3. **Resource Not Found**: Verify resource IDs in request
4. **Validation Errors**: Check required fields and format constraints
5. **Duplicate Resources**: Handle unique constraint violations

## 🎯 Quick Start Guide

### 1. Setup Database (5 minutes)
```bash
# Create PostgreSQL database
createdb klaro

# Run migrations
./gradlew flywayMigrate
```

### 2. Start Application (1 minute)
```bash
./gradlew bootRun
```

### 3. Create Admin User (2 minutes)
```bash
# Register admin user
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Admin User",
    "email": "admin@example.com",
    "password": "password123",
    "confirmPassword": "password123",
    "role": "ROLE_ADMIN"
  }'
```

### 4. Login and Get Token (1 minute)
```bash
# Login to get access token
curl -X POST http://localhost:8080/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "password123"
  }'
```

### 5. Create Your First Group (2 minutes)
```bash
# Use admin token from login response
curl -X POST http://localhost:8080/groups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN_HERE" \
  -d '{
    "name": "My Development Team",
    "description": "Main development team",
    "memberEmails": ["developer@example.com"]
  }'
```

### 6. Create a Project (2 minutes)
```bash
# Create project in the group
curl -X POST http://localhost:8080/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN_HERE" \
  -d '{
    "name": "My First Project",
    "description": "A test project",
    "groupId": 1
  }'
```

### 7. Generate AI Suggestions (1 minute)
```bash
# Get AI assistance for your project
curl -X POST http://localhost:8080/ai/generate \
  -H "Content-Type: application/json" \
  -d '{
    "issueDescription": "Need to implement user authentication with JWT tokens"
  }'
```

🎉 **Congratulations!** You now have Klaro running with your first group, project, and AI assistance.

---

**Built with ❤️ using Spring Boot, PostgreSQL, and modern Java technologies.**
