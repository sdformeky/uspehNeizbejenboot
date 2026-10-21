# certificate-service

Выдача и проверка сертификатов.

Выдаёт сертификат по завершённой записи на курс (проверяет её в enrollment-service), хранит шаблоны и позволяет проверить сертификат по номеру.

- Порт: `8084`
- Таблицы в `uspeh_db`: `cert_templates`, `certificates`

## Эндпоинты

| Метод | Путь | Описание | Ответ |
|---|---|---|---|
| POST | `/certificates?enrollmentId=1` | Выпустить сертификат по записи на курс | `201` |

Вызывается из enrollment-service после записи на курс. Через api-gateway: `POST http://localhost:8080/api/certificates?enrollmentId=1`.
