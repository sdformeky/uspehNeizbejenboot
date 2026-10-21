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
