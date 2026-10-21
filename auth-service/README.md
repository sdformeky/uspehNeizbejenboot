# auth-service

Пользователи, роли и вход.

Регистрация и вход, хранение учётных записей и ролей: STUDENT, INSTRUCTOR, SUPPORT, ADMIN. На `users.id` ссылаются студенты, преподаватели, обращения и уведомления.

- Порт: `8085`
- Таблицы в `uspeh_db`: `users`, `roles`, `user_roles`

## Эндпоинты

| Метод | Путь | Описание | Ответ |
|---|---|---|---|
| POST | `/auth/register` | Регистрация `{email, password}`, роль STUDENT | `201`, `400`, `409` (email занят) |
| POST | `/auth/login` | Вход `{email, password}` | `200`, `401` |
| GET | `/auth/users` | Список пользователей | `200` |
| GET | `/auth/users/{id}` | Пользователь по id | `200`, `404` |

Через api-gateway: `http://localhost:8080/api/auth/...`. Пароль хранится только в виде хэша BCrypt и наружу не отдаётся.
Роли создаются из `data.sql` при старте.
