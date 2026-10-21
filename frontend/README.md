# frontend

Сайт онлайн-школы — http://localhost:3000.

Обычные HTML, CSS и JavaScript без сборщиков: файлы лежат в `src/main/resources/static`,
Spring Boot только раздаёт их на порту `3000`. Все данные сайт берёт **только через api-gateway**
(`http://localhost:8080/api/...`) — сам он не знает адресов сервисов. Запросы с порта 3000 на 8080
разрешены настройкой CORS в `config-repo/api-gateway.yml`.

| Файл | Что внутри |
|---|---|
| `static/index.html` | Разметка всех разделов |
| `static/css/style.css` | Оформление |
| `static/js/app.js` | Логика: запросы к API, отрисовка, формы |

## Разделы и сервисы

| Раздел | Сервис | Запросы |
|---|---|---|
| Вход / регистрация | auth-service | `POST /api/auth/login`, `POST /api/auth/register` |
| Курсы | course-service, enrollment-service | `GET /api/courses`, `POST /api/enrollments?studentId=&courseId=` |
| Мои курсы | student-, enrollment-, certificate-service | `GET/POST /api/students`, `GET /api/enrollments?studentId=`, `GET /api/certificates?enrollmentId=` |
| Поддержка | support-service | `GET /api/support/faq`, `GET/POST /api/support/tickets`, сообщения и статус |
| Уведомления (колокольчик) | notification-service | `GET /api/notifications?userId=`, `unread-count`, `PATCH .../read` |
| Студенты | student-service | полный CRUD `/api/students` |
| Проверка сертификата | certificate-service | `GET /api/certificates/{номер}` |

Вошедший пользователь хранится в `localStorage` браузера. Профиль студента связан с пользователем
полем `userId` в student-service.

Если сервис выключен, раздел покажет понятную ошибку, остальные продолжат работать.
