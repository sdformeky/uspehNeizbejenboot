# support-service

Техподдержка.

Обращения пользователей с категорией, приоритетом и статусом, переписка по обращению (включая внутренние заметки поддержки) и база ответов FAQ.

- Порт: `8086`
- Таблицы в `uspeh_db`: `tickets`, `ticket_messages`, `faq_articles`

## Эндпоинты

| Метод | Путь | Описание | Ответ |
|---|---|---|---|
| GET | `/support/faq?category=` | Частые вопросы (все или одной категории) | `200` |
| GET | `/support/tickets?authorId=` | Обращения (все или одного пользователя) | `200` |
| GET | `/support/tickets/{id}` | Обращение по id | `200`, `404` |
| POST | `/support/tickets` | Создать `{authorId, subject, category, priority, text}` | `201`, `400` |
| GET | `/support/tickets/{id}/messages` | Переписка | `200`, `404` |
| POST | `/support/tickets/{id}/messages` | Написать `{authorId, text}`; ответ не автора переводит обращение в работу | `201`, `404`, `409` (закрыто) |
| PATCH | `/support/tickets/{id}/status` | Сменить статус `{status}` | `200`, `404` |

Через api-gateway: `http://localhost:8080/api/support/...`. FAQ создаётся из `data.sql` при старте.
