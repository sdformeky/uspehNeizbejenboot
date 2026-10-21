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
| `frontend` | 3000 | Сайт школы: все данные берёт через api-gateway |

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
5. `frontend` — сайт на http://localhost:3000

Сборка всех модулей: `./mvnw package -DskipTests` (Windows: `mvnw.cmd`), запуск — из IDEA или `java -jar <модуль>/target/<модуль>-0.0.1-SNAPSHOT.jar`
из корня проекта (config-server ищет папку `config-repo` рядом). Сервисы появляются в Eureka примерно через 30 секунд после старта.
Gateway в первые ~30 секунд после своего старта может отвечать `503` — он ещё не получил список сервисов из Eureka, нужно просто подождать.

## Ссылки после запуска

| Что | Адрес |
|---|---|
| Сайт | http://localhost:3000 |
| Дашборд Eureka | http://localhost:8761 |
| Swagger всех сервисов | http://localhost:8080/swagger-ui.html |
| API через gateway | `http://localhost:8080/api/<students, courses, enrollments, certificates, ...>` |
| Health сервиса | `http://localhost:<порт>/actuator/health` |
| Состояние Circuit Breaker | http://localhost:8083/actuator/circuitbreakers |
| Логи | `logs/<имя-сервиса>.log` |

## API через gateway

Все запросы — на `http://localhost:8080/api/...`, gateway убирает `/api` и передаёт запрос сервису.
CORS открыт для любого порта `localhost` и `127.0.0.1`. Ошибки — в формате
[Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`title`, `detail`, `status`, при ошибке валидации — `errors` по полям).

Кроме открытых запросов нужен токен: `Authorization: Bearer <token>` из `POST /api/auth/login` или `/register`
(подробнее — раздел «Вход и права» ниже). Без токена — `401`, без нужной роли — `403`.

| Сервис | Метод и путь | Кому доступно |
|---|---|---|
| auth-service | `POST /api/auth/register`, `POST /api/auth/login` | всем — выдают токен |
| | `GET /api/auth/users` | ADMIN |
| course-service | `GET /api/courses`, `GET /api/courses/{id}` | всем |
| | `POST /api/courses`, `PUT /api/courses/{id}`, `DELETE /api/courses/{id}` | ADMIN |
| student-service | `POST /api/students` | вошедшему — только свой профиль (`userId` = свой); ADMIN — любой |
| | `GET /api/students/{id}` | вошедшему |
| | `GET /api/students`, `PUT /api/students/{id}`, `DELETE /api/students/{id}` | ADMIN |
| enrollment-service | `POST /api/enrollments?studentId=&courseId=` | вошедшему — только свой профиль; параметры в URL, не в теле |
| | `GET /api/enrollments?studentId=` / `GET /api/enrollments` | свои / все — ADMIN |
| certificate-service | `GET /api/certificates?enrollmentId=`, `GET /api/certificates/{номер}` | всем — проверка подлинности |
| notification-service | `GET /api/notifications?userId=`, `GET /api/notifications/unread-count?userId=` | только свои |

Подробности, тела запросов и коды ответов — в README сервисов и в Swagger.

## Postman

Коллекция и окружение — в папке [`postman/`](postman/):

1. Postman → Import → оба файла из `postman/`.
2. Справа вверху выбрать окружение **uspeh — localhost**.
3. Запустить папку **0. Вход и права**: вход администратора (демо-аккаунт), регистрация нового студента и проверки
   `401`/`403`. Токены сохраняются в переменные коллекции, остальные запросы подставляют их сами.
4. Папка **1. Демо-сценарий** — для защиты, по порядку: администратор создаёт и публикует курс → студент
   регистрируется, заполняет профиль и записывается → повторная запись (`409`) → свои записи, сертификат,
   проверка сертификата по номеру и уведомление о записи. Папка самодостаточна.
5. Остальные папки — по сервисам: успешные запросы и ошибки (`400`, `401`, `403`, `404`, `409`). Каждый прогон
   регистрирует нового студента и создаёт свои данные, поэтому коллекцию можно гонять сколько угодно раз.
   В каждом запросе есть автотесты — вкладка Test Results.

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
| `401` «Нужно войти» / «Сессия недействительна» | Нет токена или он истёк (12 часов) — войти заново, в Postman — перезапустить папку «0. Вход и права» |
| `403` «Доступно только администратору» | Запрос требует роль ADMIN — войти как `admin@uspeh.ru` |
| Запись на курс отвечает `502` | student- или course-service недоступен, сработал circuit breaker — запустить сервис и подождать |

## Демо-сценарий

1. `GET /api/students/1` и `GET /api/courses/1` — студент и курс есть
2. `POST /api/enrollments?studentId=1&courseId=1` — `201`, enrollment-service проверил студента и курс через Feign и вызвал certificate-service
3. Тот же запрос ещё раз — `409`, студент уже записан; `studentId=999` — `404`
4. Выключить student-service и повторить запись несколько раз — `502` с сообщением fallback, в `/actuator/circuitbreakers` — `OPEN`
5. Включить student-service — через ~30 секунд запись снова проходит, состояние — `CLOSED`

## Вход и права

Пользователь входит через auth-service и получает JWT; api-gateway проверяет токен и права на каждый запрос
(правила — в `api-gateway/README.md`). Демо-аккаунты создаются при старте:

| Email | Пароль | Роль |
|---|---|---|
| `admin@uspeh.ru` | `admin123` | ADMIN — раздел «Студенты», управление пользователями |
| `teacher@uspeh.ru` | `teacher123` | INSTRUCTOR — посты на странице курса |

Студент регистрируется сам на сайте.

### То же через сайт

1. http://localhost:3000 → «Войти» → регистрация → «Мои курсы»: заполнить профиль студента
   (auth-service создаёт пользователя, student-service — профиль, связанный через `userId`)
2. «Курсы» → «Подробнее» — страница курса: программа, вебинары, новости и отзывы; «Записаться»: enrollment-service проверяет студента и курс, certificate-service выпускает сертификат,
   notification-service присылает уведомление — колокольчик в шапке
3. «Мои курсы» — запись и номер сертификата, по клику — проверка в «Проверка сертификата»
4. «Поддержка» — FAQ и обращения с перепиской (support-service); после записи на курс можно оставить отзыв на его странице
5. Войти как `teacher@uspeh.ru` — на странице курса появится форма поста; как `admin@uspeh.ru` — раздел «Студенты» (CRUD)
6. Выключить student-service и нажать «Записаться» — сайт покажет сообщение fallback Circuit Breaker
