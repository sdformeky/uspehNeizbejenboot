# База данных

Все сервисы работают с одной базой PostgreSQL — `uspeh_db`. Каждый сервис владеет только своими таблицами,
таблицы создаёт Hibernate по сущностям, вручную их создавать не нужно.

| Параметр | Значение |
|---|---|
| СУБД | PostgreSQL 13+ (рекомендуется 15+) |
| Хост и порт | `localhost:5432` |
| База | `uspeh_db` |
| Пользователь | `student_app` |
| Пароль | в переменной окружения `DB_PASSWORD`, в репозитории его нет |

## Первая настройка

### 1. Создать базу и пользователя

Один раз под суперпользователем `postgres` (pgAdmin → Query Tool или `psql -U postgres -h localhost`):

```sql
CREATE DATABASE uspeh_db;
CREATE ROLE student_app WITH LOGIN PASSWORD '<пароль>';
ALTER DATABASE uspeh_db OWNER TO student_app;
```

Пароль придумайте сами и никуда не коммитьте.

### 2. Задать переменные окружения

Сервисы берут параметры подключения из переменных окружения, значения по умолчанию есть у всех, кроме пароля:

| Переменная | По умолчанию | Обязательна |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/uspeh_db` | нет |
| `DB_USERNAME` | `student_app` | нет |
| `DB_PASSWORD` | — | **да** |

Windows (PowerShell), сохраняется для пользователя навсегда:

```powershell
[Environment]::SetEnvironmentVariable('DB_PASSWORD', '<пароль>', 'User')
```

После этого **полностью перезапустите IntelliJ IDEA** (и терминалы) — переменные подхватываются только при старте.

Вместо системной переменной можно указать её в конфигурации запуска IDEA:
Run → Edit Configurations → нужный сервис → Environment variables → `DB_PASSWORD=<пароль>`.

### 3. Запустить

1. `config-server` — без него сервисы не получат настройки БД.
2. `eureka-server`.
3. Сервисы с базой — при первом старте Hibernate создаст свои таблицы.
4. `api-gateway`.

## Где лежат настройки

Подключение описано один раз в общем конфиге
[`config-repo/application.yml`](../config-repo/application.yml),
config-server раздаёт его всем сервисам:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/uspeh_db}
    username: ${DB_USERNAME:student_app}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
```

`ddl-auto: update` — Hibernate создаёт недостающие таблицы и колонки по сущностям, но ничего не удаляет.
Если переименовали поле или таблицу, старую колонку или таблицу нужно удалить вручную.

## Какие таблицы чьи

| Сервис | Таблицы |
|---|---|
| auth-service | `users`, `roles`, `user_roles` |
| student-service | `groups`, `students`, `favorites` |
| course-service | `categories`, `tags`, `instructors`, `courses`, `course_tags`, `course_modules`, `lessons`, `assignments`, `quizzes`, `questions`, `answer_options`, `reviews`, `webinars` |
| enrollment-service | `enrollments`, `lesson_progress`, `submissions`, `quiz_attempts`, `payments`, `promo_codes` |
| certificate-service | `cert_templates`, `certificates` |
| support-service | `tickets`, `ticket_messages`, `faq_articles` |
| notification-service | `notifications` |

Внешние ключи есть только внутри одного сервиса. Ссылки на таблицы другого сервиса (`student_id`, `course_id`,
`user_id` и т.п.) — обычные числовые поля без FK, их проверяет сам сервис через Feign.
Не делайте JOIN и не пишите в таблицы чужого сервиса — только через его API.

## Как посмотреть базу

- **IntelliJ IDEA**: Database → + → Data Source → PostgreSQL, хост `localhost`, порт `5432`,
  база `uspeh_db`, пользователь `student_app`, пароль — ваш.
- **pgAdmin**: Servers → PostgreSQL → Databases → `uspeh_db` → Schemas → `public` → Tables.
- **psql**:

  ```bash
  psql -U student_app -h localhost -d uspeh_db
  ```

  `\dt` — список таблиц, `\d students` — структура таблицы.

## Частые ошибки

| Ошибка | Причина | Что делать |
|---|---|---|
| `password authentication failed for user "student_app"` | Не задан или неверный `DB_PASSWORD` | Проверить переменную, перезапустить IDEA |
| `Failed to configure a DataSource: 'url' attribute is not specified` | Не запущен config-server | Сначала запустить config-server |
| `database "uspeh_db" does not exist` | База не создана | Шаг 1 первой настройки |
| `HHH000511: The 13.x version ... is no longer supported` | Старая версия PostgreSQL | Предупреждение, работать можно; лучше обновиться до 15+ |
