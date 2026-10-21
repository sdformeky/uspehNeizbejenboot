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

-- Программа курсов: модули и уроки
INSERT INTO course_modules (course_id, title, position)
SELECT co.id, v.title, v.position
FROM (VALUES ('Java с нуля', 1, 'Первые шаги'),
             ('Java с нуля', 2, 'Объектно-ориентированное программирование'),
             ('Java с нуля', 3, 'Коллекции и исключения'),
             ('Spring Boot и микросервисы', 1, 'REST на Spring Boot'),
             ('Spring Boot и микросервисы', 2, 'Spring Data JPA'),
             ('Spring Boot и микросервисы', 3, 'Spring Cloud: Config, Eureka, Gateway'),
             ('SQL и PostgreSQL', 1, 'Основы SQL'),
             ('SQL и PostgreSQL', 2, 'Производительность и транзакции'),
             ('Основы UI/UX', 1, 'Исследование пользователей'),
             ('Основы UI/UX', 2, 'Прототипирование'))
         AS v(course, position, title)
         JOIN courses co ON co.title = v.course
ON CONFLICT (course_id, position) DO NOTHING;

INSERT INTO lessons (module_id, title, content, video_url, duration_min, position)
SELECT m.id, v.title, v.content, v.video, v.minutes, v.position
FROM (VALUES ('Java с нуля', 1, 1, 'Установка JDK и IntelliJ IDEA', 'Ставим инструменты и запускаем первую программу', 'video', 25),
             ('Java с нуля', 1, 2, 'Переменные и типы данных', 'int, double, boolean, String и приведение типов', 'video', 40),
             ('Java с нуля', 1, 3, 'Условия и циклы', 'if/else, switch, for, while', 'video', 45),
             ('Java с нуля', 2, 1, 'Классы и объекты', 'Поля, методы, конструкторы', 'video', 50),
             ('Java с нуля', 2, 2, 'Наследование и полиморфизм', 'extends, переопределение, абстрактные классы', 'video', 55),
             ('Java с нуля', 2, 3, 'Интерфейсы', 'Контракты и множественная реализация', NULL, 35),
             ('Java с нуля', 3, 1, 'List, Set, Map', 'Когда какую коллекцию выбрать', 'video', 50),
             ('Java с нуля', 3, 2, 'Исключения', 'try/catch, свои исключения', 'video', 40),
             ('Spring Boot и микросервисы', 1, 1, 'Первый контроллер', '@RestController, @GetMapping, запуск', 'video', 35),
             ('Spring Boot и микросервисы', 1, 2, 'Валидация и ошибки', '@Valid и @RestControllerAdvice', 'video', 45),
             ('Spring Boot и микросервисы', 2, 1, 'Сущности и репозитории', '@Entity, JpaRepository, связи', 'video', 55),
             ('Spring Boot и микросервисы', 2, 2, 'Транзакции', '@Transactional и типичные ошибки', NULL, 30),
             ('Spring Boot и микросервисы', 3, 1, 'Config Server и Eureka', 'Централизованные настройки и реестр сервисов', 'video', 60),
             ('Spring Boot и микросервисы', 3, 2, 'Gateway и Circuit Breaker', 'Маршруты lb:// и Resilience4j', 'video', 60),
             ('SQL и PostgreSQL', 1, 1, 'SELECT и фильтрация', 'WHERE, ORDER BY, LIMIT', 'video', 40),
             ('SQL и PostgreSQL', 1, 2, 'JOIN и группировка', 'INNER/LEFT JOIN, GROUP BY, HAVING', 'video', 55),
             ('SQL и PostgreSQL', 2, 1, 'Индексы', 'B-tree, EXPLAIN ANALYZE', 'video', 45),
             ('SQL и PostgreSQL', 2, 2, 'Транзакции и блокировки', 'Уровни изоляции на примерах', NULL, 50),
             ('Основы UI/UX', 1, 1, 'Интервью с пользователями', 'Как задавать вопросы и не подсказывать ответ', 'video', 40),
             ('Основы UI/UX', 1, 2, 'Пользовательские сценарии', 'Customer Journey Map', NULL, 35),
             ('Основы UI/UX', 2, 1, 'Скетчи и вайрфреймы', 'От бумаги до первого макета', 'video', 45))
         AS v(course, module_pos, position, title, content, video, minutes)
         JOIN courses co ON co.title = v.course
         JOIN course_modules m ON m.course_id = co.id AND m.position = v.module_pos
ON CONFLICT (module_id, position) DO NOTHING;

-- Вебинары
INSERT INTO webinars (course_id, title, starts_at, link)
SELECT co.id, v.title, v.starts_at::timestamp, v.link
FROM (VALUES ('Java с нуля', 'Разбор домашних заданий: циклы', '2026-11-05 19:00', 'https://meet.example.com/java-1'),
             ('Java с нуля', 'Вопрос-ответ по ООП', '2026-11-19 19:00', 'https://meet.example.com/java-2'),
             ('Spring Boot и микросервисы', 'Живой кодинг: свой микросервис за час', '2026-11-12 19:30', 'https://meet.example.com/spring-1'),
             ('SQL и PostgreSQL', 'Оптимизируем медленный запрос', '2026-11-10 19:00', 'https://meet.example.com/sql-1'),
             ('Основы UI/UX', 'Ревью прототипов студентов', '2026-11-16 18:30', 'https://meet.example.com/ux-1'))
         AS v(course, title, starts_at, link)
         JOIN courses co ON co.title = v.course
WHERE NOT EXISTS (SELECT 1 FROM webinars w WHERE w.course_id = co.id AND w.title = v.title);

-- Посты курсов
INSERT INTO course_posts (course_id, title, text, author_name, created_at)
SELECT co.id, v.title, v.text, i.full_name, v.created_at::timestamp
FROM (VALUES ('Java с нуля', 'Добро пожаловать на курс!',
              'Первые уроки уже открыты. Начните с установки JDK — дальше всё пойдёт быстрее. Вопросы задавайте в поддержке.',
              '2026-10-12 10:00'),
             ('Java с нуля', 'Шпаргалка по циклам',
              'Выложили в материалы урока «Условия и циклы» шпаргалку: когда for, а когда while. Пригодится для домашки.',
              '2026-10-18 12:30'),
             ('Spring Boot и микросервисы', 'Старт 2 ноября',
              'До старта осталось немного. Чтобы не тратить время на первом занятии, заранее поставьте JDK 17+ и IntelliJ IDEA.',
              '2026-10-20 09:00'),
             ('SQL и PostgreSQL', 'Тренажёр запросов',
              'Подключили учебную базу с данными магазина — тренируйте JOIN и GROUP BY прямо в браузере.',
              '2026-10-19 15:00'),
             ('Основы UI/UX', 'Набор в группу открыт',
              'Курс стартует 26 октября. Понадобится только Figma (бесплатного тарифа хватит).',
              '2026-10-15 11:00'))
         AS v(course, title, text, created_at)
         JOIN courses co ON co.title = v.course
         JOIN instructors i ON i.id = co.instructor_id
WHERE NOT EXISTS (SELECT 1 FROM course_posts p WHERE p.course_id = co.id AND p.title = v.title);

-- Отзывы (student_id — тестовые студенты из student-service)
INSERT INTO reviews (course_id, student_id, rating, comment, created_at)
SELECT co.id, v.student_id, v.rating, v.comment, v.created_at::timestamp
FROM (VALUES ('Java с нуля', 1, 5, 'Объясняют с самых основ, много практики. Наконец-то понял ООП', '2026-10-17 20:10'),
             ('Java с нуля', 2, 4, 'Хорошая подача, хотелось бы больше задач на коллекции', '2026-10-19 18:45'),
             ('SQL и PostgreSQL', 3, 5, 'После урока про индексы ускорил запрос на работе в десять раз', '2026-10-20 21:00'))
         AS v(course, student_id, rating, comment, created_at)
         JOIN courses co ON co.title = v.course
ON CONFLICT (course_id, student_id) DO NOTHING;
