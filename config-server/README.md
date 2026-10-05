# config-server

Централизованная конфигурация сервисов.

Раздаёт настройки остальным сервисам из папки [`config-repo`](../config-repo) в корне проекта. В `application.yml` лежат общие настройки: адрес Eureka, actuator, логирование и подключение к БД `uspeh_db`. Настройки отдельного сервиса — в `<имя-сервиса>.yml`.

Проверка: `GET http://localhost:8888/student-service/default` отдаёт настройки student-service (свой файл + общий).

- Порт: `8888`
- Про базу данных и подключение к ней: [DATABASE.md](DATABASE.md)
