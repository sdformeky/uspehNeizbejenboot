INSERT INTO groups (name, start_year, created_at)
VALUES ('ИСП-21', 2021, now()),
       ('ИСП-22', 2022, now()),
       ('ВЕБ-23', 2023, now())
ON CONFLICT (name) DO NOTHING;

INSERT INTO students (group_id, first_name, last_name, phone, birth_date, status, created_at)
SELECT g.id, v.first_name, v.last_name, v.phone, v.birth_date::date, v.status, now()
FROM (VALUES ('ИСП-21', 'Алексей', 'Кузнецов', '+79161234567', '2005-03-14', 'ACTIVE'),
             ('ИСП-21', 'Мария', 'Попова', '+79167654321', '2005-07-02', 'ACTIVE'),
             ('ИСП-22', 'Дмитрий', 'Волков', '+79031112233', '2006-01-25', 'ACTIVE'),
             ('ИСП-22', 'Екатерина', 'Морозова', NULL, '2006-09-10', 'ACTIVE'),
             ('ВЕБ-23', 'Никита', 'Соколов', '+79254445566', '2007-04-30', 'BLOCKED'))
         AS v(group_name, first_name, last_name, phone, birth_date, status)
         JOIN groups g ON g.name = v.group_name
WHERE NOT EXISTS (SELECT 1 FROM students s WHERE s.first_name = v.first_name AND s.last_name = v.last_name);
