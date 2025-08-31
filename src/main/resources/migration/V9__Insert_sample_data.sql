-- Insert sample data for testing
-- Note: Password is 'password123' hashed with BCrypt
INSERT INTO users (full_name, email, password, role) VALUES
('Admin User', 'admin@example.com', '$2a$10$DowJonesIndex123456789012345678901234567890123456789012345678', 'ROLE_ADMIN'),
('Test User', 'user@example.com', '$2a$10$DowJonesIndex123456789012345678901234567890123456789012345678', 'ROLE_USER'),
('John Doe', 'john.doe@example.com', '$2a$10$DowJonesIndex123456789012345678901234567890123456789012345678', 'ROLE_USER'),
('Jane Smith', 'jane.smith@example.com', '$2a$10$DowJonesIndex123456789012345678901234567890123456789012345678', 'ROLE_USER');

-- Insert sample groups
INSERT INTO groups (name, description, owner_id) VALUES
('Development Team', 'Main development team for the project', 1),
('Testing Team', 'Quality assurance and testing team', 1),
('Design Team', 'UI/UX design team', 2);

-- Add members to groups
INSERT INTO group_members (group_id, user_id) VALUES
(1, 1), -- Admin in Development Team
(1, 2), -- Test User in Development Team  
(1, 3), -- John in Development Team
(2, 1), -- Admin in Testing Team
(2, 4), -- Jane in Testing Team
(3, 2), -- Test User in Design Team
(3, 4); -- Jane in Design Team

-- Insert sample projects
INSERT INTO projects (name, description, created_by, group_id) VALUES
('E-commerce Platform', 'Building a comprehensive e-commerce solution', 1, 1),
('Mobile App', 'Cross-platform mobile application', 1, 1),
('Design System', 'Company-wide design system and components', 2, 3);

-- Insert sample issues
INSERT INTO issue (title, description, priority, status, type, created_by, assigned_to, project_id) VALUES
('User Authentication Bug', 'Users cannot login with correct credentials', 'HIGH', 'TO_DO', 'BUG', 1, 3, 1),
('Implement Shopping Cart', 'Add shopping cart functionality to the platform', 'MEDIUM', 'IN_PROGRESS', 'FEATURE', 1, 2, 1),
('Database Performance', 'Optimize database queries for better performance', 'LOW', 'TO_DO', 'TASK', 1, 3, 1),
('Mobile UI Issues', 'Fix responsive design issues on mobile devices', 'MEDIUM', 'TO_DO', 'BUG', 1, 4, 2);

-- Insert sample comments
INSERT INTO comment (content, author_id, issue_id) VALUES
('I think this might be related to the JWT token validation', 3, 1),
('Let me check the authentication service logs', 1, 1),
('Working on the cart API endpoints', 2, 2),
('Need to review the database indexing strategy', 3, 3);

-- Insert sample notifications
INSERT INTO notification (recipient, message, read) VALUES
('john.doe@example.com', 'You have been assigned to issue: User Authentication Bug', FALSE),
('user@example.com', 'You have been assigned to issue: Implement Shopping Cart', FALSE),
('jane.smith@example.com', 'You have been assigned to issue: Mobile UI Issues', FALSE),
('admin@example.com', 'New comment added to issue: User Authentication Bug', TRUE);
