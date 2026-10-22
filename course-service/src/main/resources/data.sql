INSERT INTO categories (name)
VALUES ('Программирование'),
       ('Дизайн'),
       ('Базы данных')
ON CONFLICT (name) DO NOTHING;

INSERT INTO tags (name)
VALUES ('Java'), ('Spring'), ('Для начинающих'), ('SQL'), ('Figma')
ON CONFLICT (name) DO NOTHING;

INSERT INTO instructors (full_name, bio)
SELECT v.full_name, v.bio
FROM (VALUES ('Иванов Пётр Сергеевич', 'Java-разработчик, 10 лет в коммерческой разработке'),
             ('Смирнова Анна Олеговна', 'Продуктовый дизайнер и преподаватель UI/UX')) AS v(full_name, bio)
WHERE NOT EXISTS (SELECT 1 FROM instructors i WHERE i.full_name = v.full_name);

INSERT INTO courses (category_id, instructor_id, title, description, price, max_students, status,
                     start_date, end_date, created_at)
SELECT c.id, i.id, v.title, v.description, v.price, v.max_students, v.status,
       v.start_date::date, v.end_date::date, now()
FROM (VALUES ('Java с нуля', 'Синтаксис, ООП, коллекции и исключения', 'Программирование',
              'Иванов Пётр Сергеевич', 0.00, 30, 'PUBLISHED', '2026-10-12', '2026-12-20'),
             ('Spring Boot и микросервисы', 'REST, Spring Data JPA, Spring Cloud', 'Программирование',
              'Иванов Пётр Сергеевич', 14990.00, 20, 'PUBLISHED', '2026-11-02', '2027-01-31'),
             ('SQL и PostgreSQL', 'Запросы, индексы, транзакции', 'Базы данных',
              'Иванов Пётр Сергеевич', 7990.00, 25, 'PUBLISHED', '2026-10-19', '2026-11-30'),
             ('Основы UI/UX', 'Пользовательские сценарии, прототипы, сетки', 'Дизайн',
              'Смирнова Анна Олеговна', 9990.00, 15, 'PUBLISHED', '2026-10-26', '2026-12-14'),
             ('Figma для разработчиков', 'Как читать макеты и забирать ресурсы', 'Дизайн',
              'Смирнова Анна Олеговна', 4990.00, NULL, 'DRAFT', NULL, NULL))
         AS v(title, description, category, instructor, price, max_students, status, start_date, end_date)
         JOIN categories c ON c.name = v.category
         JOIN instructors i ON i.full_name = v.instructor
WHERE NOT EXISTS (SELECT 1 FROM courses co WHERE co.title = v.title);

INSERT INTO course_tags (course_id, tag_id)
SELECT co.id, t.id
FROM (VALUES ('Java с нуля', 'Java'),
             ('Java с нуля', 'Для начинающих'),
             ('Spring Boot и микросервисы', 'Java'),
             ('Spring Boot и микросервисы', 'Spring'),
             ('SQL и PostgreSQL', 'SQL'),
             ('Основы UI/UX', 'Для начинающих'),
             ('Figma для разработчиков', 'Figma')) AS v(course, tag)
         JOIN courses co ON co.title = v.course
         JOIN tags t ON t.name = v.tag
ON CONFLICT DO NOTHING;

-- Программа курсов: модули и уроки
INSERT INTO course_modules (course_id, title, position)
SELECT co.id, v.title, v.position
FROM (VALUES ('Java с нуля', 1, 'Первые шаги'),
             ('Java с нуля', 2, 'Объектно-ориентированное программирование'),
             ('Java с нуля', 3, 'Коллекции и исключения'),
             ('Spring Boot и микросервисы', 1, 'REST на Spring Boot'),
             ('Spring Boot и микросервисы', 2, 'Spring Data JPA'),
             ('Spring Boot и микросервисы', 3, 'Spring Cloud: Config, Eureka, Gateway'),
             ('SQL и PostgreSQL', 1, 'Основы SQL'),
             ('SQL и PostgreSQL', 2, 'Производительность и транзакции'),
             ('Основы UI/UX', 1, 'Исследование пользователей'),
             ('Основы UI/UX', 2, 'Прототипирование'))
         AS v(course, position, title)
         JOIN courses co ON co.title = v.course
ON CONFLICT (course_id, position) DO NOTHING;

INSERT INTO lessons (module_id, title, content, video_url, duration_min, position)
SELECT m.id, v.title, v.content, v.video, v.minutes, v.position
FROM (VALUES ('Java с нуля', 1, 1, 'Установка JDK и IntelliJ IDEA', 'Ставим инструменты и запускаем первую программу', 'video', 25),
             ('Java с нуля', 1, 2, 'Переменные и типы данных', 'int, double, boolean, String и приведение типов', 'video', 40),
             ('Java с нуля', 1, 3, 'Условия и циклы', 'if/else, switch, for, while', 'video', 45),
             ('Java с нуля', 2, 1, 'Классы и объекты', 'Поля, методы, конструкторы', 'video', 50),
             ('Java с нуля', 2, 2, 'Наследование и полиморфизм', 'extends, переопределение, абстрактные классы', 'video', 55),
             ('Java с нуля', 2, 3, 'Интерфейсы', 'Контракты и множественная реализация', NULL, 35),
             ('Java с нуля', 3, 1, 'List, Set, Map', 'Когда какую коллекцию выбрать', 'video', 50),
             ('Java с нуля', 3, 2, 'Исключения', 'try/catch, свои исключения', 'video', 40),
             ('Spring Boot и микросервисы', 1, 1, 'Первый контроллер', '@RestController, @GetMapping, запуск', 'video', 35),
             ('Spring Boot и микросервисы', 1, 2, 'Валидация и ошибки', '@Valid и @RestControllerAdvice', 'video', 45),
             ('Spring Boot и микросервисы', 2, 1, 'Сущности и репозитории', '@Entity, JpaRepository, связи', 'video', 55),
             ('Spring Boot и микросервисы', 2, 2, 'Транзакции', '@Transactional и типичные ошибки', NULL, 30),
             ('Spring Boot и микросервисы', 3, 1, 'Config Server и Eureka', 'Централизованные настройки и реестр сервисов', 'video', 60),
             ('Spring Boot и микросервисы', 3, 2, 'Gateway и Circuit Breaker', 'Маршруты lb:// и Resilience4j', 'video', 60),
             ('SQL и PostgreSQL', 1, 1, 'SELECT и фильтрация', 'WHERE, ORDER BY, LIMIT', 'video', 40),
             ('SQL и PostgreSQL', 1, 2, 'JOIN и группировка', 'INNER/LEFT JOIN, GROUP BY, HAVING', 'video', 55),
             ('SQL и PostgreSQL', 2, 1, 'Индексы', 'B-tree, EXPLAIN ANALYZE', 'video', 45),
             ('SQL и PostgreSQL', 2, 2, 'Транзакции и блокировки', 'Уровни изоляции на примерах', NULL, 50),
             ('Основы UI/UX', 1, 1, 'Интервью с пользователями', 'Как задавать вопросы и не подсказывать ответ', 'video', 40),
             ('Основы UI/UX', 1, 2, 'Пользовательские сценарии', 'Customer Journey Map', NULL, 35),
             ('Основы UI/UX', 2, 1, 'Скетчи и вайрфреймы', 'От бумаги до первого макета', 'video', 45))
         AS v(course, module_pos, position, title, content, video, minutes)
         JOIN courses co ON co.title = v.course
         JOIN course_modules m ON m.course_id = co.id AND m.position = v.module_pos
ON CONFLICT (module_id, position) DO NOTHING;

-- Вебинары
INSERT INTO webinars (course_id, title, starts_at, link)
SELECT co.id, v.title, v.starts_at::timestamp, v.link
FROM (VALUES ('Java с нуля', 'Разбор домашних заданий: циклы', '2026-11-05 19:00', 'https://meet.example.com/java-1'),
             ('Java с нуля', 'Вопрос-ответ по ООП', '2026-11-19 19:00', 'https://meet.example.com/java-2'),
             ('Spring Boot и микросервисы', 'Живой кодинг: свой микросервис за час', '2026-11-12 19:30', 'https://meet.example.com/spring-1'),
             ('SQL и PostgreSQL', 'Оптимизируем медленный запрос', '2026-11-10 19:00', 'https://meet.example.com/sql-1'),
             ('Основы UI/UX', 'Ревью прототипов студентов', '2026-11-16 18:30', 'https://meet.example.com/ux-1'))
         AS v(course, title, starts_at, link)
         JOIN courses co ON co.title = v.course
WHERE NOT EXISTS (SELECT 1 FROM webinars w WHERE w.course_id = co.id AND w.title = v.title);

-- Посты курсов
INSERT INTO course_posts (course_id, title, text, author_name, created_at)
SELECT co.id, v.title, v.text, i.full_name, v.created_at::timestamp
FROM (VALUES ('Java с нуля', 'Добро пожаловать на курс!',
              'Первые уроки уже открыты. Начните с установки JDK — дальше всё пойдёт быстрее. Вопросы задавайте в поддержке.',
              '2026-10-12 10:00'),
             ('Java с нуля', 'Шпаргалка по циклам',
              'Выложили в материалы урока «Условия и циклы» шпаргалку: когда for, а когда while. Пригодится для домашки.',
              '2026-10-18 12:30'),
             ('Spring Boot и микросервисы', 'Старт 2 ноября',
              'До старта осталось немного. Чтобы не тратить время на первом занятии, заранее поставьте JDK 17+ и IntelliJ IDEA.',
              '2026-10-20 09:00'),
             ('SQL и PostgreSQL', 'Тренажёр запросов',
              'Подключили учебную базу с данными магазина — тренируйте JOIN и GROUP BY прямо в браузере.',
              '2026-10-19 15:00'),
             ('Основы UI/UX', 'Набор в группу открыт',
              'Курс стартует 26 октября. Понадобится только Figma (бесплатного тарифа хватит).',
              '2026-10-15 11:00'))
         AS v(course, title, text, created_at)
         JOIN courses co ON co.title = v.course
         JOIN instructors i ON i.id = co.instructor_id
WHERE NOT EXISTS (SELECT 1 FROM course_posts p WHERE p.course_id = co.id AND p.title = v.title);

-- Отзывы (student_id — тестовые студенты из student-service)
INSERT INTO reviews (course_id, student_id, rating, comment, created_at)
SELECT co.id, v.student_id, v.rating, v.comment, v.created_at::timestamp
FROM (VALUES ('Java с нуля', 1, 5, 'Объясняют с самых основ, много практики. Наконец-то понял ООП', '2026-10-17 20:10'),
             ('Java с нуля', 2, 4, 'Хорошая подача, хотелось бы больше задач на коллекции', '2026-10-19 18:45'),
             ('SQL и PostgreSQL', 3, 5, 'После урока про индексы ускорил запрос на работе в десять раз', '2026-10-20 21:00'))
         AS v(course, student_id, rating, comment, created_at)
         JOIN courses co ON co.title = v.course
ON CONFLICT (course_id, student_id) DO NOTHING;

-- Тексты уроков курса «Java с нуля» (Markdown). Обновляются при каждом старте

UPDATE lessons l
SET content = 'Java — язык, на котором написан и этот сайт: все его сервисы работают на Java и Spring Boot. Начнём с того, что поставим инструменты и запустим первую программу.

## Что понадобится

- **JDK** (Java Development Kit) — компилятор и среда выполнения. Берите версию 17 или новее.
- **IntelliJ IDEA Community** — бесплатная среда разработки: подсветка, подсказки, запуск одной кнопкой.

> JRE умеет только запускать готовые программы. Для разработки нужен именно JDK — в нём есть компилятор `javac`.

## Установка

1. Скачайте JDK (например, сборку Temurin) и установите с настройками по умолчанию.
2. Проверьте в терминале: команда `java -version` должна напечатать номер версии.
3. Установите IntelliJ IDEA и создайте проект: **New Project → Java**, в поле JDK выберите установленную версию.

## Первая программа

Создайте файл `Main.java`:

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Привет, Java!");
    }
}
```

Нажмите зелёный треугольник рядом с `main` — внизу появится «Привет, Java!».

## Что здесь написано

- `public class Main` — класс. В Java весь код живёт внутри классов, имя файла совпадает с именем класса.
- `public static void main(String[] args)` — точка входа: с этого метода начинается выполнение.
- `System.out.println(...)` — вывод строки в консоль.

## Как это работает

Компилятор `javac` превращает `Main.java` в байт-код `Main.class`, а виртуальная машина JVM его выполняет. Поэтому один и тот же `.class` запускается и на Windows, и на Linux.

## Попробуйте сами

Измените программу так, чтобы она печатала три строки: ваше имя, город и любимый язык программирования.

## Итог

- Для разработки нужен JDK, для удобства — IntelliJ IDEA.
- Программа начинается с метода `main`.
- Код компилируется в байт-код и выполняется на JVM.'
FROM course_modules m
         JOIN courses c ON c.id = m.course_id
WHERE l.module_id = m.id AND c.title = 'Java с нуля' AND m.position = 1 AND l.position = 1;

UPDATE lessons l
SET content = 'Программа работает с данными: числами, строками, флагами. Чтобы их хранить, нужны переменные, а у каждой переменной в Java есть тип.

## Объявление переменной

```java
int age = 19;
double price = 14990.50;
boolean enrolled = true;
String name = "Алексей";
```

Слева тип, потом имя, потом значение. Java — язык со **строгой типизацией**: в переменную `int` нельзя положить строку, компилятор не даст.

## Основные типы

- `int` — целые числа (до ~2 млрд): возраст, количество уроков.
- `long` — большие целые: id записей в базе. В сервисах сайта id имеют тип `Long`.
- `double` — дробные числа. Для денег в реальных проектах берут `BigDecimal` — у `double` бывают ошибки округления.
- `boolean` — `true` или `false`.
- `char` — один символ, `String` — строка.

## var: тип выводится сам

```java
var lessons = 8;          // int
var title = "Java с нуля"; // String
```

`var` не делает переменную «любой» — тип фиксируется по значению и дальше не меняется.

## Приведение типов

```java
int total = 8;
int done = 3;
int progress = done * 100 / total;      // 37 — деление целых отбрасывает дробную часть
double exact = done * 100.0 / total;    // 37.5
int rounded = (int) Math.round(exact);  // 38
```

Деление двух `int` даёт `int`. Именно так считается прогресс курса на этом сайте: пройдено × 100 / всего уроков.

## Строки

```java
String first = "Анна";
String full = first + " " + "Смирнова";
System.out.println(full.length());        // 13
System.out.println(full.toUpperCase());   // АННА СМИРНОВА
```

> Строки сравнивают методом `equals`, а не `==`: `name.equals("Анна")`. Оператор `==` сравнивает ссылки на объекты.

## Попробуйте сами

Посчитайте прогресс для 5 пройденных уроков из 8 двумя способами — целочисленным делением и через `double`. Почему результаты разные?

## Итог

- У каждой переменной есть тип, менять его нельзя.
- `int` / `long` — целые, `double` — дробные, `boolean` — да/нет, `String` — текст.
- Деление целых отбрасывает дробную часть.'
FROM course_modules m
         JOIN courses c ON c.id = m.course_id
WHERE l.module_id = m.id AND c.title = 'Java с нуля' AND m.position = 1 AND l.position = 2;

UPDATE lessons l
SET content = 'Программа редко выполняется строго сверху вниз: она выбирает, что делать, и повторяет действия. Для этого есть условия и циклы.

## if / else

```java
int progress = 75;

if (progress == 100) {
    System.out.println("Курс пройден, держите сертификат!");
} else if (progress >= 50) {
    System.out.println("Больше половины позади");
} else {
    System.out.println("Продолжайте учиться");
}
```

Операторы сравнения: `==`, `!=`, `>`, `<`, `>=`, `<=`. Логические: `&&` (и), `||` (или), `!` (не).

## switch

Удобен, когда вариантов много и они известны заранее:

```java
String status = "PUBLISHED";

String label = switch (status) {
    case "DRAFT" -> "Черновик";
    case "PUBLISHED" -> "Открыт для записи";
    case "ARCHIVED" -> "В архиве";
    default -> "Неизвестно";
};
```

Такой `switch` со стрелками возвращает значение и не требует `break`.

## Цикл for

Когда известно, сколько раз повторять:

```java
for (int lesson = 1; lesson <= 8; lesson++) {
    System.out.println("Урок " + lesson);
}
```

## Цикл for-each

Для перебора всех элементов массива или коллекции:

```java
String[] modules = {"Первые шаги", "ООП", "Коллекции и исключения"};
for (String module : modules) {
    System.out.println(module);
}
```

## Цикл while

Когда повторять нужно, пока выполняется условие:

```java
int seats = 3;
while (seats > 0) {
    System.out.println("Записан студент, мест осталось: " + (seats - 1));
    seats--;
}
```

> Следите, чтобы условие `while` когда-нибудь стало ложным, иначе цикл будет бесконечным.

## Попробуйте сами

Выведите числа от 1 до 30, но вместо чисел, кратных 3, печатайте «Java», кратных 5 — «Spring», а кратных и 3, и 5 — «JavaSpring».

## Итог

- `if/else` и `switch` выбирают ветку выполнения.
- `for` — известное число повторов, `for-each` — перебор элементов, `while` — пока верно условие.'
FROM course_modules m
         JOIN courses c ON c.id = m.course_id
WHERE l.module_id = m.id AND c.title = 'Java с нуля' AND m.position = 1 AND l.position = 3;

UPDATE lessons l
SET content = 'Java — объектно-ориентированный язык: программа описывает объекты реального мира, у которых есть данные и поведение. Шаблон объекта — **класс**.

## Класс и объект

```java
public class Course {
    String title;
    int lessonsCount;

    void printInfo() {
        System.out.println(title + ": " + lessonsCount + " уроков");
    }
}
```

```java
Course java = new Course();
java.title = "Java с нуля";
java.lessonsCount = 8;
java.printInfo();   // Java с нуля: 8 уроков
```

Класс — чертёж, объект — конкретная вещь по этому чертежу. Оператор `new` создаёт объект.

## Конструктор

Чтобы объект сразу создавался заполненным, пишут конструктор:

```java
public class Course {
    private final String title;
    private int lessonsCount;

    public Course(String title, int lessonsCount) {
        this.title = title;
        this.lessonsCount = lessonsCount;
    }

    public String getTitle() {
        return title;
    }
}
```

`this.title` — поле объекта, `title` — параметр конструктора.

## Инкапсуляция

Поля делают `private`, а доступ дают через методы — геттеры и сеттеры. Так класс сам контролирует свои данные:

```java
public void setLessonsCount(int lessonsCount) {
    if (lessonsCount < 0) {
        throw new IllegalArgumentException("Уроков не может быть меньше нуля");
    }
    this.lessonsCount = lessonsCount;
}
```

> В проектах на Spring геттеры и сеттеры часто генерирует библиотека Lombok: аннотации `@Getter` и `@Setter` над классом. Так написаны сущности этого сайта.

## record — класс для данных

Если объект только хранит данные, его удобно объявить как `record`:

```java
public record CourseDto(Long id, String title, int lessonsCount) {
}
```

Конструктор, геттеры (`title()`), `equals` и `toString` появляются сами. Именно так сервисы сайта описывают ответы API.

## Попробуйте сами

Напишите класс `Student` с полями `firstName`, `lastName` и методом `fullName()`, который возвращает «Фамилия Имя». Создайте двух студентов и выведите их полные имена.

## Итог

- Класс — шаблон, объект — экземпляр, создаётся через `new`.
- Конструктор заполняет объект при создании.
- Поля закрывают `private` и открывают методами — это инкапсуляция.
- `record` — короткая запись класса-данных.'
FROM course_modules m
         JOIN courses c ON c.id = m.course_id
WHERE l.module_id = m.id AND c.title = 'Java с нуля' AND m.position = 2 AND l.position = 1;

UPDATE lessons l
SET content = 'Наследование позволяет создать новый класс на основе существующего, а полиморфизм — работать с разными объектами одинаково.

## Наследование: extends

```java
public class Lesson {
    protected final String title;
    protected final int minutes;

    public Lesson(String title, int minutes) {
        this.title = title;
        this.minutes = minutes;
    }

    public String describe() {
        return title + " (" + minutes + " мин)";
    }
}

public class VideoLesson extends Lesson {
    private final String videoUrl;

    public VideoLesson(String title, int minutes, String videoUrl) {
        super(title, minutes);
        this.videoUrl = videoUrl;
    }
}
```

`VideoLesson` получает поля и методы `Lesson`. Вызов `super(...)` передаёт данные конструктору родителя.

## Переопределение методов

```java
public class VideoLesson extends Lesson {
    // ...
    @Override
    public String describe() {
        return "▶ " + super.describe();
    }
}
```

Аннотация `@Override` просит компилятор проверить, что метод действительно переопределяет родительский.

## Полиморфизм

```java
List<Lesson> program = List.of(
        new Lesson("Пользовательские сценарии", 35),
        new VideoLesson("Интервью с пользователями", 40, "https://...")
);

for (Lesson lesson : program) {
    System.out.println(lesson.describe());
}
```

Переменная имеет тип `Lesson`, а вызывается метод того объекта, который в ней лежит на самом деле. Код перебора не знает про видеоуроки — и не должен.

## Абстрактные классы

Если родитель сам по себе не имеет смысла, его объявляют `abstract`:

```java
public abstract class Notification {
    public abstract String text();
}
```

Создать объект абстрактного класса нельзя, а наследники обязаны реализовать абстрактные методы.

> Наследование — сильная связь. Наследуйте, только когда подкласс действительно «является» родителем: видеоурок является уроком, а курс уроком не является.

## Попробуйте сами

Создайте класс `TextLesson` с полем `wordsCount` и переопределите `describe()`, чтобы он добавлял «📄» и число слов. Положите в список уроки всех трёх видов и выведите их.

## Итог

- `extends` — наследование, `super(...)` — вызов конструктора родителя.
- `@Override` — переопределение метода.
- Полиморфизм: тип переменной общий, поведение — конкретного объекта.'
FROM course_modules m
         JOIN courses c ON c.id = m.course_id
WHERE l.module_id = m.id AND c.title = 'Java с нуля' AND m.position = 2 AND l.position = 2;

UPDATE lessons l
SET content = 'Интерфейс — это контракт: он говорит, что класс **умеет делать**, но не говорит, как. Интерфейсы — основа Spring и всех сервисов этого сайта.

## Объявление и реализация

```java
public interface NotificationSender {
    void send(Long userId, String text);
}

public class ConsoleSender implements NotificationSender {
    @Override
    public void send(Long userId, String text) {
        System.out.println("Пользователю " + userId + ": " + text);
    }
}
```

Класс может реализовывать сразу несколько интерфейсов: `class A implements B, C`.

## Зачем это нужно

Код, который зависит от интерфейса, не зависит от конкретной реализации:

```java
public class EnrollmentService {
    private final NotificationSender sender;

    public EnrollmentService(NotificationSender sender) {
        this.sender = sender;
    }

    public void enroll(Long userId, String course) {
        // ... сохранили запись
        sender.send(userId, "Вы записаны на курс «" + course + "»");
    }
}
```

Сегодня уведомления печатаются в консоль, завтра уходят по сети — `EnrollmentService` менять не придётся.

> Так и устроен сайт: `enrollment-service` вызывает `notification-service` через интерфейс `NotificationClient`, а реализацию для сетевого вызова создаёт библиотека OpenFeign.

## Интерфейсы в Spring

Репозиторий базы данных — тоже интерфейс, реализацию Spring Data пишет сам:

```java
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStatus(CourseStatus status);
}
```

По имени метода `findByStatus` Spring сам строит SQL-запрос.

## default-методы

Интерфейс может содержать метод с реализацией по умолчанию:

```java
public interface NotificationSender {
    void send(Long userId, String text);

    default void sendAll(List<Long> users, String text) {
        users.forEach(user -> send(user, text));
    }
}
```

## Попробуйте сами

Опишите интерфейс `PriceCalculator` с методом `BigDecimal price(BigDecimal base)` и две реализации: без скидки и со скидкой 20 %. Посчитайте цену курса за 14 990 ₽ обоими способами.

## Итог

- Интерфейс — контракт, `implements` — его выполнение.
- Зависимость от интерфейса позволяет менять реализацию без правки кода.
- Spring широко использует интерфейсы: репозитории, Feign-клиенты.'
FROM course_modules m
         JOIN courses c ON c.id = m.course_id
WHERE l.module_id = m.id AND c.title = 'Java с нуля' AND m.position = 2 AND l.position = 3;

UPDATE lessons l
SET content = 'Массив имеет фиксированный размер. В реальных программах данные постоянно добавляются и удаляются, поэтому используют **коллекции**.

## List — упорядоченный список

```java
List<String> lessons = new ArrayList<>();
lessons.add("Классы и объекты");
lessons.add("Интерфейсы");
lessons.add(1, "Наследование");   // вставка по индексу

System.out.println(lessons.get(0));   // Классы и объекты
System.out.println(lessons.size());   // 3
```

Элементы хранятся в порядке добавления, повторы разрешены. Самая частая реализация — `ArrayList`.

## Set — без повторов

```java
Set<String> tags = new HashSet<>();
tags.add("Java");
tags.add("Spring");
tags.add("Java");                 // повтор не добавится
System.out.println(tags.size());  // 2
System.out.println(tags.contains("Spring"));  // true
```

> На сайте у курса набор тегов — это `Set<Tag>`: один и тот же тег не может быть у курса дважды.

## Map — словарь «ключ → значение»

```java
Map<String, Integer> progress = new HashMap<>();
progress.put("Java с нуля", 37);
progress.put("SQL и PostgreSQL", 100);

System.out.println(progress.get("SQL и PostgreSQL"));     // 100
System.out.println(progress.getOrDefault("UI/UX", 0));    // 0

for (Map.Entry<String, Integer> e : progress.entrySet()) {
    System.out.println(e.getKey() + " — " + e.getValue() + "%");
}
```

## Что выбрать

- Нужен порядок и доступ по номеру — `List`.
- Важна уникальность, быстрая проверка «есть ли» — `Set`.
- Нужно искать значение по ключу — `Map`.

## Stream API

Коллекции удобно обрабатывать цепочкой операций:

```java
List<Integer> scores = List.of(45, 90, 72, 100, 30);

List<Integer> passed = scores.stream()
        .filter(s -> s >= 60)
        .sorted()
        .toList();          // [72, 90, 100]

double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
```

Так сервисы сайта превращают сущности из базы в ответы API: `courses.stream().map(CourseDto::from).toList()`.

## Попробуйте сами

Есть список оценок отзывов курса: 5, 4, 5, 3, 5. Посчитайте среднюю оценку через Stream API и выведите только отзывы с оценкой 5.

## Итог

- `List` — порядок и повторы, `Set` — уникальные элементы, `Map` — пары ключ-значение.
- Stream API: `filter`, `map`, `sorted`, `toList`.'
FROM course_modules m
         JOIN courses c ON c.id = m.course_id
WHERE l.module_id = m.id AND c.title = 'Java с нуля' AND m.position = 3 AND l.position = 1;

UPDATE lessons l
SET content = 'Ошибки случаются всегда: нет файла, неверные данные, недоступен сервер. В Java о них сообщают **исключения**.

## try / catch

```java
try {
    int lessons = Integer.parseInt("восемь");
    System.out.println(lessons);
} catch (NumberFormatException e) {
    System.out.println("Это не число: " + e.getMessage());
}
```

Если в блоке `try` бросилось исключение, выполнение переходит в подходящий `catch`, и программа не падает.

## finally

Блок `finally` выполняется всегда — и при успехе, и при ошибке. В нём освобождают ресурсы. Ещё удобнее `try-with-resources`, который закроет ресурс сам:

```java
try (var reader = new BufferedReader(new FileReader("course.txt"))) {
    System.out.println(reader.readLine());
} catch (IOException e) {
    System.out.println("Не удалось прочитать файл");
}
```

## Проверяемые и непроверяемые

- **Проверяемые** (`IOException`) — компилятор требует их обработать или объявить `throws`.
- **Непроверяемые** (`RuntimeException` и наследники: `NullPointerException`, `IllegalArgumentException`) — ошибки программы, обрабатывать не обязательно.

## Свои исключения

```java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

public Course findCourse(Long id) {
    return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Курс с id " + id + " не найден"));
}
```

> Ровно так сделано на этом сайте. Сервис бросает `NotFoundException`, а класс с аннотацией `@RestControllerAdvice` ловит его и превращает в ответ `404` с понятным текстом. Пользователь видит сообщение, а не падение сервера.

## Правила хорошего тона

- Ловите конкретные исключения, а не просто `Exception`.
- Не оставляйте пустой `catch` — хотя бы запишите ошибку в лог.
- Сообщение исключения должно объяснять, что пошло не так.

## Попробуйте сами

Напишите метод `enroll(int freeSeats)`, который бросает своё исключение `NoSeatsException`, если мест 0. Вызовите его в `try/catch` и выведите сообщение. Это последний урок курса — после него вы получите сертификат!

## Итог

- `try/catch` перехватывает ошибку, `finally` выполняется всегда.
- Проверяемые исключения нужно обработать, непроверяемые — ошибки в коде.
- Свои исключения делают ошибки понятными; в Spring их переводят в HTTP-ответы.'
FROM course_modules m
         JOIN courses c ON c.id = m.course_id
WHERE l.module_id = m.id AND c.title = 'Java с нуля' AND m.position = 3 AND l.position = 2;
