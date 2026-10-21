# course-service

Каталог курсов и учебные материалы.

Курсы с категориями, тегами и преподавателями; программа курса: модули → уроки → задания и тесты с вопросами и вариантами ответов; отзывы и вебинары.

- Порт: `8082`
- Таблицы в `uspeh_db`: `categories`, `tags`, `instructors`, `courses`, `course_tags`, `course_modules`, `lessons`, `assignments`, `quizzes`, `questions`, `answer_options`, `reviews`, `webinars`

## Эндпоинты

| Метод | Путь | Описание |
|---|---|---|
| GET | `/courses` | Список всех курсов |
| GET | `/courses/{id}` | Курс по id, `404` если не найден |

Через api-gateway: `http://localhost:8080/api/courses`. Swagger: `http://localhost:8082/swagger-ui.html` или общий на gateway — `http://localhost:8080/swagger-ui.html`.

Курс отдаётся как `CourseDto`: поля курса, категория, преподаватель и теги.

## Тестовые данные

`src/main/resources/data.sql` выполняется при каждом старте после создания таблиц:
3 категории, 5 тегов, 2 преподавателя, 5 курсов. Повторный запуск данные не дублирует.
