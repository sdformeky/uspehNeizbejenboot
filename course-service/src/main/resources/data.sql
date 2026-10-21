-- Тестовые данные course-service. Выполняется при каждом старте, повторный запуск ничего не дублирует.

INSERT INTO categories (name)
VALUES ('Программирование'),
       ('Дизайн'),
       ('Базы данных')
ON CONFLICT (name) DO NOTHING;

INSERT INTO tags (name)
VALUES ('Java'), ('Spring'), ('Для начинающих'), ('SQL'), ('Figma')
ON CONFLICT (name) DO NOTHING;

INSERT INTO instructors (full_name, bio)
SELECT v.full_name, v.bio
FROM (VALUES ('Иванов Пётр Сергеевич', 'Java-разработчик, 10 лет в коммерческой разработке'),
             ('Смирнова Анна Олеговна', 'Продуктовый дизайнер и преподаватель UI/UX')) AS v(full_name, bio)
WHERE NOT EXISTS (SELECT 1 FROM instructors i WHERE i.full_name = v.full_name);

INSERT INTO courses (category_id, instructor_id, title, description, price, max_students, status,
                     start_date, end_date, created_at)
SELECT c.id, i.id, v.title, v.description, v.price, v.max_students, v.status,
       v.start_date::date, v.end_date::date, now()
FROM (VALUES ('Java с нуля', 'Синтаксис, ООП, коллекции и исключения', 'Программирование',
              'Иванов Пётр Сергеевич', 0.00, 30, 'PUBLISHED', '2026-10-12', '2026-12-20'),
             ('Spring Boot и микросервисы', 'REST, Spring Data JPA, Spring Cloud', 'Программирование',
              'Иванов Пётр Сергеевич', 14990.00, 20, 'PUBLISHED', '2026-11-02', '2027-01-31'),
             ('SQL и PostgreSQL', 'Запросы, индексы, транзакции', 'Базы данных',
              'Иванов Пётр Сергеевич', 7990.00, 25, 'PUBLISHED', '2026-10-19', '2026-11-30'),
             ('Основы UI/UX', 'Пользовательские сценарии, прототипы, сетки', 'Дизайн',
              'Смирнова Анна Олеговна', 9990.00, 15, 'PUBLISHED', '2026-10-26', '2026-12-14'),
             ('Figma для разработчиков', 'Как читать макеты и забирать ресурсы', 'Дизайн',
              'Смирнова Анна Олеговна', 4990.00, NULL, 'DRAFT', NULL, NULL))
         AS v(title, description, category, instructor, price, max_students, status, start_date, end_date)
         JOIN categories c ON c.name = v.category
         JOIN instructors i ON i.full_name = v.instructor
WHERE NOT EXISTS (SELECT 1 FROM courses co WHERE co.title = v.title);

INSERT INTO course_tags (course_id, tag_id)
SELECT co.id, t.id
FROM (VALUES ('Java с нуля', 'Java'),
             ('Java с нуля', 'Для начинающих'),
             ('Spring Boot и микросервисы', 'Java'),
             ('Spring Boot и микросервисы', 'Spring'),
             ('SQL и PostgreSQL', 'SQL'),
             ('Основы UI/UX', 'Для начинающих'),
             ('Figma для разработчиков', 'Figma')) AS v(course, tag)
         JOIN courses co ON co.title = v.course
         JOIN tags t ON t.name = v.tag
ON CONFLICT DO NOTHING;
