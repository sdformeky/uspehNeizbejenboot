# notification-service

Уведомления пользователям.

Хранит уведомления: запись на курс, оплата, проверка задания, выдача сертификата, ответ поддержки. Другие сервисы создают их через Feign.

- Порт: `8087`
- Таблицы в `uspeh_db`: `notifications`

## Эндпоинты

| Метод | Путь | Описание | Ответ |
|---|---|---|---|
| GET | `/notifications?userId=1` | Уведомления пользователя, новые сверху | `200` |
| GET | `/notifications/unread-count?userId=1` | Сколько непрочитанных `{count}` | `200` |
| POST | `/notifications` | Отправить `{userId, type, text}` | `201`, `400` |
| PATCH | `/notifications/{id}/read` | Отметить прочитанным | `200`, `404` |

Через api-gateway: `http://localhost:8080/api/notifications/...`.
Уведомление о записи на курс присылает enrollment-service через Feign.
