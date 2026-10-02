# config-server

Централизованная конфигурация сервисов.

Раздаёт настройки остальным сервисам из `src/main/resources/config/`. В `application.yml` лежат общие настройки: адрес Eureka, actuator, логирование и подключение к БД `uspeh_db`.

- Порт: `8888`
