# Database Setup Guide for Klaro

## PostgreSQL Database Schema

This guide explains how to set up the PostgreSQL database for the Klaro project management system.

## Prerequisites

1. **PostgreSQL 12+** installed and running
2. **Database created**: `klaro`
3. **User access**: Ensure your PostgreSQL user has CREATE, INSERT, UPDATE, DELETE permissions

## Quick Setup

### 1. Create Database
```sql
-- Connect to PostgreSQL as superuser
CREATE DATABASE klaro;
CREATE USER klaro_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE klaro TO klaro_user;
```

### 2. Environment Variables (Optional)
```bash
export DB_NAME=klaro
export DB_USER=klaro_user  
export DB_PASSWORD=your_secure_password
```

### 3. Run Migrations
```bash
# View migration status
./gradlew flywayInfo

# Run all pending migrations
./gradlew flywayMigrate

# Or use the custom task
./gradlew migrateCollab
```

## Database Schema Overview

The database contains the following tables:

### Core Tables

1. **users** - User accounts and authentication
2. **groups** - Collaborative groups 
3. **group_members** - Many-to-many relationship between users and groups
4. **projects** - Projects within groups or individual
5. **issue** - Issues/tasks within projects
6. **comment** - Comments on issues
7. **notification** - User notifications
8. **refresh_token** - JWT refresh tokens

### Table Relationships

```
users (1) ──── (N) projects [created_by]
users (1) ──── (N) groups [owner]
users (N) ──── (N) groups [members via group_members]
groups (1) ──── (N) projects
projects (1) ──── (N) issues
users (1) ──── (N) issues [created_by]
users (1) ──── (N) issues [assigned_to] 
issues (1) ──── (N) comments
users (1) ──── (N) comments [author]
users (1) ──── (1) refresh_token
```

## Migration Files

The migration files are located in `src/main/resources/migration/`:

- `V1__Create_users_table.sql` - Users table with roles
- `V2__Create_groups_table.sql` - Groups table
- `V3__Create_group_members_table.sql` - Junction table for group membership
- `V4__Create_projects_table.sql` - Projects table
- `V5__Create_issue_table.sql` - Issues table with status/priority
- `V6__Create_comment_table.sql` - Comments table
- `V7__Create_notification_table.sql` - Notifications table
- `V8__Create_refresh_token_table.sql` - JWT refresh tokens
- `V9__Insert_sample_data.sql` - Sample data for development
- `V10__Create_admin_user.sql` - Default admin user

## Sample Data

The `V9__Insert_sample_data.sql` migration includes:

- 4 sample users (1 admin, 3 regular users)
- 3 sample groups with member assignments
- 3 sample projects
- 4 sample issues with different statuses
- Sample comments and notifications

**Note**: Remove or modify sample data for production use.

## Production Setup

### 1. Database Configuration
```properties
# Use environment variables
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/klaro}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
```

### 2. Skip Sample Data
For production, you may want to skip the sample data migration:
```bash
# Skip specific migration
./gradlew flywayMigrate -Dflyway.target=8
```

### 3. Create Admin User
The `V10__Create_admin_user.sql` creates a default admin user:
- **Email**: admin@klaro.com
- **Password**: admin123 (change immediately!)
- **Role**: ROLE_ADMIN

## Useful Commands

### Flyway Commands
```bash
# Check migration status
./gradlew flywayInfo

# Run migrations
./gradlew flywayMigrate

# Clean database (development only!)
./gradlew flywayClean

# Repair migration metadata
./gradlew flywayRepair

# Validate migrations
./gradlew flywayValidate
```

### Database Queries

#### Check User Roles
```sql
SELECT email, full_name, role FROM users;
```

#### View Group Memberships
```sql
SELECT g.name as group_name, u.full_name as member_name, 
       (CASE WHEN g.owner_id = u.id THEN 'Owner' ELSE 'Member' END) as role
FROM groups g
JOIN group_members gm ON g.id = gm.group_id
JOIN users u ON gm.user_id = u.id
ORDER BY g.name, role DESC;
```

#### View Project Issues
```sql
SELECT p.name as project, i.title, i.status, i.priority, 
       u1.full_name as created_by, u2.full_name as assigned_to
FROM issue i
JOIN projects p ON i.project_id = p.id
JOIN users u1 ON i.created_by = u1.id
LEFT JOIN users u2 ON i.assigned_to = u2.id
ORDER BY p.name, i.created_at;
```

## Troubleshooting

### Common Issues

1. **Connection Failed**
   - Check PostgreSQL is running: `sudo systemctl status postgresql`
   - Verify connection details in application.properties
   - Check firewall settings

2. **Migration Failed**
   - Check migration file syntax
   - Verify database user permissions
   - Check Flyway metadata table: `SELECT * FROM flyway_schema_history;`

3. **Duplicate Key Errors**
   - Check for existing data conflicts
   - Use `ON CONFLICT` clauses in migration files

### Reset Database (Development)
```bash
# Clean and rebuild
./gradlew flywayClean
./gradlew flywayMigrate
```

## Security Considerations

1. **Change Default Passwords**: Update all default passwords in production
2. **Environment Variables**: Use environment variables for sensitive data
3. **Database User Permissions**: Use least-privilege principle
4. **Backup Strategy**: Implement regular database backups
5. **SSL Connections**: Use SSL for database connections in production

## Entity-Table Mapping

| Entity | Table | Key Columns |
|--------|-------|-------------|
| User | users | id, email (unique), role |
| Group | groups | id, name (unique), owner_id |
| Project | projects | id, name, created_by, group_id |
| Issue | issue | id, title, status, priority, project_id |
| Comment | comment | id, content, author_id, issue_id |
| Notification | notification | id, recipient, message, read |
| RefreshToken | refresh_token | id, user_id, token (unique) |

This schema supports the full functionality described in your README with proper foreign key relationships and constraints.
