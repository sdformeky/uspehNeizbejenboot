# enrollment-service

Записи на курсы, прохождение и оплата.

Запись студента на курс и её статус, прогресс по урокам, сдача заданий, попытки тестов, оплаты и промокоды. Студента и курс проверяет через Feign в student-service и course-service.

- Порт: `8083`
- Таблицы в `uspeh_db`: `enrollments`, `lesson_progress`, `submissions`, `quiz_attempts`, `payments`, `promo_codes`
