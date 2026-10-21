# config-server

Централизованная конфигурация сервисов.

Раздаёт настройки остальным сервисам из папки [`config-repo`](../config-repo) в корне проекта. В `application.yml` лежат общие настройки: адрес Eureka, actuator, логирование и подключение к БД `uspeh_db`. Настройки отдельного сервиса — в `<имя-сервиса>.yml`.

Проверка: `GET http://localhost:8888/student-service/default` отдаёт настройки student-service (свой файл + общий).

- Порт: `8888`
- Про базу данных и подключение к ней: [DATABASE.md](DATABASE.md)

## Мониторинг и логирование (общее для всех сервисов)

Задаётся один раз в `config-repo/application.yml`:

- **Actuator**: открыты `health`, `info`, `metrics`, `loggers`. В `/actuator/info` — название, описание (из файла сервиса) и версия.
  Проверка: `http://localhost:<порт>/actuator/health` → `"status":"UP"`.
- **Логи**: единый формат `время уровень [сервис] [поток] класс - сообщение`, пишутся в консоль и в файл `logs/<имя-сервиса>.log`.
  Уровни — в `logging.level`, например `org.example: DEBUG`, чтобы видеть подробности своих классов.

```
2026-10-21 17:19:28.739 INFO  [api-gateway] [reactor-http-epoll-2] o.e.apigateway.filter.LoggingFilter - GET /api/notifications -> 200 (167 мс)
```
