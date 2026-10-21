-- Роли auth-service. Выполняется при каждом старте, повторный запуск ничего не дублирует.
INSERT INTO roles (name)
VALUES ('STUDENT'), ('INSTRUCTOR'), ('SUPPORT'), ('ADMIN')
ON CONFLICT (name) DO NOTHING;

-- Демо-аккаунты: admin@uspeh.ru / admin123 (ADMIN), teacher@uspeh.ru / teacher123 (INSTRUCTOR).
-- В базе только хэши BCrypt; на настоящем сервере пароли сменить
INSERT INTO users (email, password_hash, enabled, created_at)
SELECT v.email, v.hash, true, now()
FROM (VALUES ('admin@uspeh.ru', '$2a$10$YTGfDue3sSJZEQLkQ22fbOZxWGqx153yhAetGv3Ilro8rwaZEBnuS'),
             ('teacher@uspeh.ru', '$2a$10$eUZdhrOfEy6GpRE5sQLH4.PkbAUYwQgZgo39C5iSAsZhHTVrOlORi')) AS v(email, hash)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM (VALUES ('admin@uspeh.ru', 'ADMIN'),
             ('teacher@uspeh.ru', 'INSTRUCTOR')) AS v(email, role)
         JOIN users u ON u.email = v.email
         JOIN roles r ON r.name = v.role
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
