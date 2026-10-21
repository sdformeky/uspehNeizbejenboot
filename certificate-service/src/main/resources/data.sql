-- Шаблон сертификата по умолчанию. Выполняется при каждом старте, повторный запуск ничего не дублирует.
INSERT INTO cert_templates (name, layout)
VALUES ('Стандартный', 'Сертификат подтверждает, что {student} прошёл(а) курс «{course}».')
ON CONFLICT (name) DO NOTHING;
