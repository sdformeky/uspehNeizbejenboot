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
| GET | `/courses/{id}/page` | Страница курса: преподаватель, программа (модули → уроки), вебинары, посты, отзывы и средняя оценка | `200`, `404` |
| POST | `/courses` | Создать курс (только ADMIN) | `201`, `400`, `404` (нет категории, преподавателя или тегов) |
| PUT | `/courses/{id}` | Обновить курс целиком (только ADMIN) | `200`, `400`, `404` |
| DELETE | `/courses/{id}` | Удалить курс (только ADMIN) | `204`, `404`, `409` (есть модули, отзывы или вебинары) |
| POST | `/courses/{id}/posts` | Пост курса `{title, text}` (только INSTRUCTOR/ADMIN) | `201`, `400`, `404` |
| POST | `/courses/{id}/reviews` | Отзыв `{rating 1–5, comment}` | `201`, `400`, `403` (нет профиля или не записан), `409` (отзыв уже есть) |

Кто что может — проверяет api-gateway (см. `api-gateway/README.md`).

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

## Связь с другими сервисами (Feign)

- Имена авторов отзывов — из student-service; если он недоступен, страница всё равно открывается («Студент»).
- Отзыв: профиль студента ищется по пользователю из токена (`X-User-Id` → student-service `?userId=`),
  запись на курс проверяется в enrollment-service — оставить отзыв может только тот, кто записан.

Программа, вебинары, посты и отзывы для демо — в `data.sql`.

