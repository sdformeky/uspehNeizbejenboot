# uspehNeizbejenboot

Онлайн-школа на микросервисах Spring Cloud. Каждый сервис разрабатывается в одноимённой ветке.

| Сервис / ветка | Порт | За что отвечает |
|---|---|---|
| `config-server` | 8888 | Централизованная конфигурация сервисов |
| `eureka-server` | 8761 | Реестр сервисов (service discovery) |
| `api-gateway` | 8080 | Единая точка входа для клиентов |
| `student-service` | 8081 | Студенты, учебные группы и избранные курсы |
| `course-service` | 8082 | Каталог курсов и учебные материалы |
| `enrollment-service` | 8083 | Записи на курсы, прохождение и оплата |
| `certificate-service` | 8084 | Выдача и проверка сертификатов |
| `auth-service` | 8085 | Пользователи, роли и вход |
| `support-service` | 8086 | Техподдержка |
| `notification-service` | 8087 | Уведомления пользователям |

## База данных

Все сервисы работают с одной базой PostgreSQL `uspeh_db` под пользователем `student_app`,
но каждый сервис владеет только своими таблицами. Таблицы создаёт Hibernate по сущностям (`ddl-auto: update`).
Ссылки между таблицами разных сервисов — обычные поля `*_id` без внешних ключей, они проверяются через Feign.

Пользователь БД создаётся один раз под `postgres`, скрипт в репозиторий не входит.
Как создать базу и задать пароль (`DB_PASSWORD`) — в [config-server/DATABASE.md](config-server/DATABASE.md).

## Запуск с нуля

Нужно: JDK 17+ (проверено на 24), PostgreSQL 13+ (лучше 15+), Maven ставить не надо — в проекте есть `mvnw`.

1. Создать базу `uspeh_db` и пользователя `student_app` — SQL в [config-server/DATABASE.md](config-server/DATABASE.md).
2. Задать пароль БД в переменной окружения `DB_PASSWORD` (в репозитории его нет). Windows, PowerShell:

   ```powershell
   [Environment]::SetEnvironmentVariable('DB_PASSWORD', '<пароль>', 'User')
   ```

   После этого перезапустить IntelliJ IDEA и терминалы — переменные подхватываются только при старте.
3. Собрать: `mvnw.cmd package -DskipTests` (Linux/macOS: `./mvnw package -DskipTests`).
4. Запустить в порядке из раздела ниже. Таблицы создаст Hibernate, тестовые данные (3 группы, 5 студентов,
   3 категории, 5 тегов, 2 преподавателя, 5 курсов) загрузятся из `data.sql` при старте student- и course-service.

## Порядок запуска

1. `config-server`
2. `eureka-server`
3. остальные сервисы
4. `api-gateway`

Сборка всех модулей: `./mvnw package -DskipTests` (Windows: `mvnw.cmd`), запуск — из IDEA или `java -jar <модуль>/target/<модуль>-0.0.1-SNAPSHOT.jar`
из корня проекта (config-server ищет папку `config-repo` рядом). Сервисы появляются в Eureka примерно через 30 секунд после старта.
Gateway в первые ~30 секунд после своего старта может отвечать `503` — он ещё не получил список сервисов из Eureka, нужно просто подождать.

## Ссылки после запуска

| Что | Адрес |
|---|---|
| Дашборд Eureka | http://localhost:8761 |
| Swagger всех сервисов | http://localhost:8080/swagger-ui.html |
| API через gateway | `http://localhost:8080/api/<students, courses, enrollments, certificates, ...>` |
| Health сервиса | `http://localhost:<порт>/actuator/health` |
| Состояние Circuit Breaker | http://localhost:8083/actuator/circuitbreakers |
| Логи | `logs/<имя-сервиса>.log` |

## API через gateway

Все запросы — на `http://localhost:8080/api/...`, gateway убирает `/api` и передаёт запрос сервису.
CORS открыт для любого порта `localhost` и `127.0.0.1`. Ошибки student- и course-service — в формате
[Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`title`, `detail`, `status`, при ошибке валидации — `errors` по полям).

| Сервис | Метод и путь | Что делает |
|---|---|---|
| student-service | `GET /api/students`, `GET /api/students/{id}` | Список студентов, студент по id |
| | `POST /api/students`, `PUT /api/students/{id}`, `DELETE /api/students/{id}` | Создать, обновить целиком, удалить |
| course-service | `GET /api/courses`, `GET /api/courses/{id}` | Каталог курсов, курс по id |
| | `POST /api/courses`, `PUT /api/courses/{id}`, `DELETE /api/courses/{id}` | Создать (по умолчанию `DRAFT`), обновить целиком, удалить |
| enrollment-service | `POST /api/enrollments?studentId=&courseId=` | Записать студента на курс — параметры в URL, не в теле |
| certificate-service | `POST /api/certificates?enrollmentId=` | Выдать сертификат по записи |

Подробности, тела запросов и коды ответов — в README сервисов и в Swagger.
У auth-, support- и notification-service пока есть только модели, эндпоинтов нет.

## Postman

Коллекция и окружение — в папке [`postman/`](postman/):

1. Postman → Import → оба файла из `postman/`.
2. Справа вверху выбрать окружение **uspeh — localhost**.
3. Папка **1. Демо-сценарий** — для защиты, запускать запросы по порядку: курс → публикация → студент →
   запись → повторная запись (`409`) → сертификат. id передаются между запросами сами.
4. Остальные папки — по сервисам: успешные запросы и ошибки (`400`, `404`, `409`). Каждая папка создаёт свои данные,
   поэтому коллекцию можно гонять сколько угодно раз. В каждом запросе есть автотесты — вкладка Test Results.

Прогнать всю коллекцию из консоли (нужен Node.js):

```bash
npx newman run postman/uspeh.postman_collection.json -e postman/uspeh.postman_environment.json
```

## Частые проблемы

| Симптом | Причина и что делать |
|---|---|
| config-server: `Port 8888 was already in use` | Порт занят другой программой (например, Jupyter Notebook). Закрыть её или запустить config-server на другом порту и передать его сервисам: `--server.port=8889` и `--spring.config.import=optional:configserver:http://localhost:8889` |
| `password authentication failed for user "student_app"` | Не задан или неверный `DB_PASSWORD`; после изменения перезапустить IDEA |
| `Failed to configure a DataSource` | config-server не запущен или запущен не из корня проекта (не видит `config-repo`) |
| Gateway отвечает `503` | Сервис ещё не зарегистрировался в Eureka — подождать ~30 секунд |
| Запись на курс отвечает `502` | student- или course-service недоступен, сработал circuit breaker — запустить сервис и подождать |

## Демо-сценарий

1. `GET /api/students/1` и `GET /api/courses/1` — студент и курс есть
2. `POST /api/enrollments?studentId=1&courseId=1` — `201`, enrollment-service проверил студента и курс через Feign и вызвал certificate-service
3. Тот же запрос ещё раз — `409`, студент уже записан; `studentId=999` — `404`
4. Выключить student-service и повторить запись несколько раз — `502` с сообщением fallback, в `/actuator/circuitbreakers` — `OPEN`
5. Включить student-service — через ~30 секунд запись снова проходит, состояние — `CLOSED`
