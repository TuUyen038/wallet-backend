-- Tạo sẵn test user để dùng trong Postman, không cần register thủ công mỗi lần
-- Password: "password123" đã bcrypt hash (cost=10)

INSERT INTO users (email, password, full_name)
VALUES ('alice@wallet.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Alice Nguyen'),
       ('bob@wallet.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Bob Tran');