# api-gateway

Единая точка входа для клиентов (Spring Cloud Gateway).

Принимает все внешние запросы и по префиксу пути передаёт их нужному сервису.
Адрес сервиса берётся из Eureka по имени (`lb://student-service`), поэтому порты сервисов клиенту знать не нужно.
Если сервис запущен в нескольких экземплярах, запросы распределяются между ними.

- Порт: `8080`
- Регистрируется в Eureka как `api-gateway`
- Маршруты не в коде, а в [`config-repo/api-gateway.yml`](../config-repo/api-gateway.yml) — gateway получает их из config-server

## Маршруты

Префикс `/api` снимается фильтром `StripPrefix=1`: запрос `/api/students/1` приходит в student-service как `/students/1`.

| Путь | Сервис |
|---|---|
| `/api/students/**` | student-service |
| `/api/courses/**` | course-service |
| `/api/enrollments/**` | enrollment-service |
| `/api/certificates/**` | certificate-service |
| `/api/auth/**` | auth-service |
| `/api/support/**` | support-service |
| `/api/notifications/**` | notification-service |

## Логирование запросов

`LoggingFilter` (GlobalFilter) пишет в лог каждый запрос, прошедший через маршруты: метод, путь, статус и время ответа.

```
GET /api/students/1 -> 200 (15 мс)
POST /api/students -> 400 (45 мс)
```

## CORS

Настроен в `config-repo/api-gateway.yml` (`globalcors`): разрешены запросы со страниц на любом порту `localhost` / `127.0.0.1` —
Swagger UI сервисов, фронт на `localhost:3000` и т. п. Запросы с других адресов браузер не пропустит (preflight → 403).
Когда появится фронт на реальном адресе, его нужно добавить в `allowedOriginPatterns`.

## Проверка

1. Запустить config-server, eureka-server, нужные сервисы, потом api-gateway.
   Без config-server gateway стартует, но маршрутов у него не будет (все запросы — 404).
2. На дашборде http://localhost:8761 должны быть `API-GATEWAY` и сервисы.
3. http://localhost:8080/api/students — тот же ответ, что и http://localhost:8081/students.
4. Список маршрутов, которые gateway получил: http://localhost:8080/actuator/gateway/routes
