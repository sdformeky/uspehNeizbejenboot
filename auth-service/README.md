# auth-service

Пользователи, роли и вход.

Регистрация и вход, хранение учётных записей и ролей: STUDENT, INSTRUCTOR, SUPPORT, ADMIN. На `users.id` ссылаются студенты, преподаватели, обращения и уведомления.

- Порт: `8085`
- Таблицы в `uspeh_db`: `users`, `roles`, `user_roles`

## Эндпоинты

| Метод | Путь | Описание | Ответ |
|---|---|---|---|
| POST | `/auth/register` | Регистрация `{email, password}`, роль STUDENT; ответ `{token, user}` | `201`, `400`, `409` (email занят) |
| POST | `/auth/login` | Вход `{email, password}`; ответ `{token, user}` | `200`, `401` |
| GET | `/auth/users` | Список пользователей (только ADMIN) | `200` |
| GET | `/auth/users/{id}` | Пользователь по id | `200`, `404` |

Через api-gateway: `http://localhost:8080/api/auth/...`. Пароль хранится только в виде хэша BCrypt и наружу не отдаётся.
Роли создаются из `data.sql` при старте.

## Токен (JWT)

После входа клиент получает `token` и отправляет его в каждом запросе: `Authorization: Bearer <token>`.
В токене — id пользователя, email и роли, срок жизни `jwt.ttl-hours` (12 часов). Подписывается секретом `jwt.secret`
из `config-repo/application.yml`, api-gateway проверяет подпись тем же секретом.

## Демо-аккаунты

Создаются из `data.sql` при старте (в базе — только хэши BCrypt):

| Email | Пароль | Роль |
|---|---|---|
| `admin@uspeh.ru` | `admin123` | ADMIN — управление студентами и пользователями |
| `teacher@uspeh.ru` | `teacher123` | INSTRUCTOR — посты на страницах курсов |

Студента создайте через регистрацию на сайте.

