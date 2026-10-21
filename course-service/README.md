# course-service

Каталог курсов и учебные материалы.

Курсы с категориями, тегами и преподавателями; программа курса: модули → уроки → задания и тесты с вопросами и вариантами ответов; отзывы и вебинары.

- Порт: `8082`
- Таблицы в `uspeh_db`: `categories`, `tags`, `instructors`, `courses`, `course_tags`, `course_modules`, `lessons`, `assignments`, `quizzes`, `questions`, `answer_options`, `reviews`, `webinars`

## Эндпоинты

| Метод | Путь | Описание | Ответ |
|---|---|---|---|
| GET | `/courses` | Список всех курсов | `200` |
| GET | `/courses/{id}` | Курс по id | `200`, `404` |
| POST | `/courses` | Создать курс | `201`, `400`, `404` (нет категории, преподавателя или тегов) |
| PUT | `/courses/{id}` | Обновить курс целиком | `200`, `400`, `404` |
| DELETE | `/courses/{id}` | Удалить курс | `204`, `404`, `409` (есть модули, отзывы или вебинары) |

Через api-gateway: `http://localhost:8080/api/courses`. Swagger: `http://localhost:8082/swagger-ui.html` или общий на gateway — `http://localhost:8080/swagger-ui.html`.

Курс отдаётся как `CourseDto`: поля курса, категория, преподаватель и теги.

Тело запроса POST/PUT (`CourseRequest`):

```json
{
  "title": "Spring Boot и микросервисы",
  "description": "REST, Spring Data JPA, Spring Cloud",
  "price": 14990.00,
  "maxStudents": 20,
  "status": "DRAFT",
  "startDate": "2026-11-02",
  "endDate": "2027-01-31",
  "categoryId": 1,
  "instructorId": 1,
  "tagIds": [1, 2]
}
```

Обязательны `title` и `price` (≥ 0). `maxStudents` > 0 или пусто (без ограничения), `endDate` не раньше `startDate`,
`status` по умолчанию `DRAFT`. PUT заменяет все поля: что не передано, станет пустым.
Ошибки возвращаются в формате Problem Details, при ошибке валидации — с полем `errors` по каждому полю.

## Тестовые данные

`src/main/resources/data.sql` выполняется при каждом старте после создания таблиц:
3 категории, 5 тегов, 2 преподавателя, 5 курсов. Повторный запуск данные не дублирует.
