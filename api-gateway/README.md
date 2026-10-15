# api-gateway

Единая точка входа для клиентов (Spring Cloud Gateway).

Принимает все внешние запросы и по префиксу пути передаёт их нужному сервису.
Адрес сервиса берётся из Eureka по имени (`lb://student-service`), поэтому порты сервисов клиенту знать не нужно.

- Порт: `8080`
- Регистрируется в Eureka как `api-gateway`

## Маршруты

| Путь | Сервис |
|---|---|
| `/students/**` | student-service |
| `/courses/**` | course-service |
| `/enrollments/**` | enrollment-service |
| `/certificates/**` | certificate-service |
| `/auth/**` | auth-service |
| `/support/**` | support-service |
| `/notifications/**` | notification-service |

## Проверка

1. Запустить config-server, eureka-server, нужные сервисы, потом api-gateway.
2. На дашборде http://localhost:8761 должны быть `API-GATEWAY` и `STUDENT-SERVICE`.
3. http://localhost:8080/students — тот же ответ, что и http://localhost:8081/students.
