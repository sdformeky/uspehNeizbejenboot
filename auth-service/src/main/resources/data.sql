-- Роли auth-service. Выполняется при каждом старте, повторный запуск ничего не дублирует.
INSERT INTO roles (name)
VALUES ('STUDENT'), ('INSTRUCTOR'), ('SUPPORT'), ('ADMIN')
ON CONFLICT (name) DO NOTHING;
