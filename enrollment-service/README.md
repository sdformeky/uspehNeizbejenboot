# enrollment-service

Записи на курсы, прохождение и оплата.

Запись студента на курс и её статус, прогресс по урокам, сдача заданий, попытки тестов, оплаты и промокоды. Студента и курс проверяет через Feign в student-service и course-service.

- Порт: `8083`
- Таблицы в `uspeh_db`: `enrollments`, `lesson_progress`, `submissions`, `quiz_attempts`, `payments`, `promo_codes`

## Эндпоинты

| Метод | Путь | Описание | Ответ |
|---|---|---|---|
| POST | `/enrollments?studentId=1&courseId=1` | Записать студента на курс | `201`, `404` (нет студента/курса), `409` (уже записан), `502` (сосед недоступен) |
| GET | `/enrollments?studentId=1` | Записи (все или одного студента) | `200` |

Через api-gateway: `POST http://localhost:8080/api/enrollments?studentId=1&courseId=1`.

Порядок: проверка студента в student-service и курса в course-service (Feign, по имени из Eureka) →
сохранение записи → вызов certificate-service → уведомление в notification-service (если профиль студента связан с пользователем сайта).
Если certificate- или notification-service недоступен, запись всё равно создаётся, ошибка пишется в лог.

## Circuit Breaker

Каждый Feign-клиент обёрнут в предохранитель Resilience4j. Пороги — в `config-repo/enrollment-service.yml`:
из последних 10 вызовов (минимум 5) ошибок ≥ 50% → `OPEN` на 15 секунд, потом `HALF_OPEN` (3 пробных вызова).

- Если сосед недоступен — срабатывает fallback (`client/*Fallback`) и клиент получает `502` с понятным сообщением
- `404` от соседа (студента нет) — не отказ: fallback пробрасывает его как есть, в процент ошибок он не идёт
- Состояние: `http://localhost:8083/actuator/circuitbreakers`

Демо: выключить student-service → несколько раз `POST /api/enrollments` → `502` «Сервис студентов временно недоступен»,
в `/actuator/circuitbreakers` у `StudentClientgetStudentByIdLong` — `OPEN`. Включить student-service →
примерно через 30 секунд (пока он появится в Eureka) запись снова проходит, состояние — `CLOSED`.
