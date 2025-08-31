-- Production migration: Create initial admin user only
-- Note: Password is 'admin123' hashed with BCrypt (change in production!)
INSERT INTO users (full_name, email, password, role) 
VALUES ('System Administrator', 'admin@klaro.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ROLE_ADMIN')
ON CONFLICT (email) DO NOTHING;
