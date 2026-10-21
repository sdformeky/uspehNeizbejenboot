// Сайт онлайн-школы. Все данные — только через api-gateway: http://localhost:8080/api/<сервис>/...
// Gateway по префиксу пути отправляет запрос нужному сервису (адрес сервиса берёт из Eureka).

const API = 'http://localhost:8080/api';

let token = loadSaved('token');        // JWT от auth-service, отправляем в заголовке Authorization
let currentUser = loadSaved('user');   // вошедший пользователь: {id, email, roles}
let myStudent = null;           // профиль студента текущего пользователя (student-service)
let courses = [];               // каталог курсов (course-service)
let students = [];              // все студенты (student-service)
let editingStudent = null;      // студент, которого сейчас редактируем на странице «Студенты»
let selectedTicketId = null;    // открытое обращение на странице «Поддержка»
let coursePage = null;          // данные открытой страницы курса
let myEnrollment = null;        // запись текущего студента на открытый курс (enrollment-service)
let completedLessons = [];      // id пройденных уроков в этой записи

const ENROLLMENT_STATUS = { ACTIVE: 'Учится', COMPLETED: 'Завершён', CANCELLED: 'Отменён' };
const TICKET_STATUS = { OPEN: 'Открыто', IN_PROGRESS: 'В работе', RESOLVED: 'Решено', CLOSED: 'Закрыто' };
const STUDENT_STATUS = { ACTIVE: 'Активен', BLOCKED: 'Заблокирован' };


// ===================== общее =====================

// Запрос к API через gateway. При ошибке выбрасывает Error с понятным текстом.
// Если пользователь вошёл — добавляем токен: по нему gateway понимает, кто это и что ему можно
async function request(method, path, body) {
    const options = { method: method, headers: {} };
    if (token) {
        options.headers['Authorization'] = 'Bearer ' + token;
    }
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(API + path, options);
    } catch (e) {
        throw new Error('Нет связи с api-gateway (localhost:8080). Он запущен?');
    }

    let data = null;
    const text = await response.text();
    if (text) {
        try {
            data = JSON.parse(text);
        } catch (e) {
            data = null;
        }
    }

    if (!response.ok) {
        // токен истёк или недействителен — выходим, чтобы пользователь вошёл заново
        if (response.status === 401 && token) {
            setSession(null);
        }
        throw new Error(errorText(response.status, data));
    }
    return data;
}

// Текст ошибки из ответа сервиса: у наших сервисов это detail (Problem Details) или message
function errorText(status, data) {
    if (status === 503) {
        return 'Сервис недоступен (503): он выключен или ещё не появился в Eureka — подождите ~30 секунд.';
    }
    if (data && data.errors) {
        return Object.values(data.errors).join('; ');
    }
    if (data && data.detail) {
        return data.detail;
    }
    if (data && data.message) {
        return data.message;
    }
    return 'Ошибка ' + status;
}

function showMessage(text, isError) {
    const box = document.getElementById('message');
    box.textContent = text;
    box.className = isError ? 'message error' : 'message ok';
    window.scrollTo(0, 0);
    clearTimeout(showMessage.timer);
    showMessage.timer = setTimeout(function () { box.className = 'message hidden'; }, 7000);
}

// Экранирование текста перед вставкой в HTML
function esc(value) {
    if (value === null || value === undefined) {
        return '';
    }
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;');
}

function formatDate(value) {
    if (!value) {
        return '—';
    }
    return new Date(value).toLocaleDateString('ru-RU');
}

function formatDateTime(value) {
    if (!value) {
        return '';
    }
    return new Date(value).toLocaleString('ru-RU', { dateStyle: 'short', timeStyle: 'short' });
}

// Пустую строку из формы отправляем как null
function emptyToNull(value) {
    if (value === undefined || value === null || value.trim() === '') {
        return null;
    }
    return value.trim();
}


// ===================== пользователь (auth-service) =====================

function loadSaved(key) {
    try {
        return JSON.parse(localStorage.getItem(key));
    } catch (e) {
        return null;
    }
}

// auth — ответ auth-service на вход/регистрацию: {token, user}; null — выход
function setSession(auth) {
    token = auth ? auth.token : null;
    currentUser = auth ? auth.user : null;
    myStudent = null;
    try {
        if (auth) {
            localStorage.setItem('token', JSON.stringify(auth.token));
            localStorage.setItem('user', JSON.stringify(auth.user));
        } else {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }
    } catch (e) {
        // localStorage может быть недоступен — тогда вход живёт до перезагрузки страницы
    }
    renderUserBox();
}

function hasRole(role) {
    return currentUser !== null && currentUser.roles.includes(role);
}

function isAdmin() {
    return hasRole('ADMIN');
}

// посты курса публикует преподаватель или админ (то же правило проверяет gateway)
function canPost() {
    return hasRole('INSTRUCTOR') || hasRole('ADMIN');
}

function renderUserBox() {
    const box = document.getElementById('user-box');
    // «Студенты» — управление всеми студентами, только для администратора
    document.getElementById('nav-students').classList.toggle('hidden', !isAdmin());
    if (!currentUser) {
        box.innerHTML = '<a href="#login">Войти</a>';
        return;
    }
    box.innerHTML =
        '<a class="bell" href="#notifications" title="Уведомления">🔔<b id="bell-count" class="hidden"></b></a>' +
        '<span class="email">' + esc(currentUser.email) + roleLabel() + '</span>' +
        '<button class="small secondary" onclick="logout()">Выйти</button>';
    updateBell();
}

function roleLabel() {
    if (isAdmin()) {
        return ' (админ)';
    }
    if (hasRole('INSTRUCTOR')) {
        return ' (преподаватель)';
    }
    return '';
}

async function updateBell() {
    if (!currentUser) {
        return;
    }
    const counter = document.getElementById('bell-count');
    try {
        const data = await request('GET', '/notifications/unread-count?userId=' + currentUser.id);
        counter.textContent = data.count;
        counter.classList.toggle('hidden', data.count === 0);
    } catch (e) {
        counter.classList.add('hidden');   // нет уведомлений — не повод ругаться
    }
}

async function login(event) {
    event.preventDefault();
    const form = event.target;
    try {
        const auth = await request('POST', '/auth/login', {
            email: form.elements.email.value,
            password: form.elements.password.value
        });
        setSession(auth);
        form.reset();
        showMessage('Добро пожаловать, ' + auth.user.email + '!');
        location.hash = isAdmin() || canPost() ? '#courses' : '#my';
    } catch (e) {
        showMessage(e.message, true);
    }
}

async function register(event) {
    event.preventDefault();
    const form = event.target;
    try {
        const auth = await request('POST', '/auth/register', {
            email: form.elements.email.value,
            password: form.elements.password.value
        });
        setSession(auth);
        form.reset();
        showMessage('Аккаунт создан. Заполните профиль студента, чтобы записываться на курсы.');
        location.hash = '#my';
    } catch (e) {
        showMessage(e.message, true);
    }
}

function logout() {
    setSession(null);
    location.hash = '#courses';
}

// Профиль студента текущего пользователя: студент, у которого userId = id пользователя
async function loadMyStudent() {
    const list = await request('GET', '/students?userId=' + currentUser.id);
    myStudent = list.length > 0 ? list[0] : null;
}


// ===================== страницы =====================

const PAGES = ['courses', 'course', 'my', 'support', 'notifications', 'students', 'verify', 'login'];

// Адрес страницы — после #: #courses, #my, #course/2 (страница курса с id 2)
function showPage() {
    const parts = location.hash.replace('#', '').split('/');
    let name = parts[0];
    if (!PAGES.includes(name)) {
        name = 'courses';
    }
    for (const page of PAGES) {
        document.getElementById('page-' + page).classList.toggle('hidden', page !== name);
    }
    for (const link of document.querySelectorAll('.nav a')) {
        const active = link.dataset.page === name || (name === 'course' && link.dataset.page === 'courses');
        link.classList.toggle('active', active);
    }

    if (name === 'courses') {
        loadCourses();
    } else if (name === 'course') {
        loadCoursePage(Number(parts[1]));
    } else if (name === 'my') {
        loadMy();
    } else if (name === 'support') {
        loadSupport();
    } else if (name === 'notifications') {
        loadNotifications();
    } else if (name === 'students') {
        loadStudents();
    }
}


// ----- Каталог курсов (course-service) + запись (enrollment-service) -----

async function loadCourses() {
    const list = document.getElementById('courses-list');
    list.innerHTML = skeletonCards(3);
    try {
        courses = await request('GET', '/courses');
    } catch (e) {
        list.innerHTML = '<p class="hint">' + esc(e.message) + '</p>';
        return;
    }
    renderHeroStats();
    fillCategoryFilter();
    renderCourses();
}

// Черновики и архив видит только администратор — остальным они не нужны и записаться на них нельзя
function visibleCourses() {
    return isAdmin() ? courses : courses.filter(function (c) { return c.status === 'PUBLISHED'; });
}

const COVER_ICONS = {
    code: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M8 7l-5 5 5 5M16 7l5 5-5 5M14 4l-4 16"/></svg>',
    db: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"/></svg>',
    design: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 3a9 9 0 1 0 0 18c1.1 0 1.5-.9 1.5-1.6 0-1.4-1.2-1.4-1.2-2.6 0-.9.8-1.6 1.8-1.6H16a5 5 0 0 0 5-5C21 6.5 17 3 12 3z"/><circle cx="7.5" cy="11" r="1"/><circle cx="10" cy="7" r="1"/><circle cx="15" cy="7" r="1"/></svg>',
    other: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 19.5V5a2 2 0 0 1 2-2h14v16H6a2 2 0 0 0-2 2zm0 0A2 2 0 0 0 6 22h14"/></svg>'
};

// Обложка курса по категории: цвет и иконка
function coverKind(category) {
    const name = (category || '').toLowerCase();
    if (name.includes('программ')) return 'code';
    if (name.includes('баз')) return 'db';
    if (name.includes('дизайн')) return 'design';
    return 'other';
}

function animateNumber(el, target) {
    const start = performance.now();
    const step = function (now) {
        const t = Math.min(1, (now - start) / 1100);
        el.textContent = Math.round(target * (1 - Math.pow(1 - t, 3)));
        if (t < 1) {
            requestAnimationFrame(step);
        }
    };
    requestAnimationFrame(step);
}

function launchConfetti() {
    const colors = ['#ffd23f', '#2e9c76', '#ff6b6b', '#4dabf7', '#ffffff'];
    const x = window.innerWidth / 2, y = window.innerHeight / 3;
    for (let i = 0; i < 70; i++) {
        const piece = document.createElement('i');
        piece.className = 'confetti';
        piece.style.left = x + 'px';
        piece.style.top = y + 'px';
        piece.style.background = colors[i % colors.length];
        piece.style.setProperty('--dx', (Math.random() * 700 - 350) + 'px');
        piece.style.setProperty('--dy', (Math.random() * 400 + 120) + 'px');
        piece.style.setProperty('--rot', (Math.random() * 900) + 'deg');
        piece.style.animationDelay = (Math.random() * 0.2) + 's';
        document.body.appendChild(piece);
        setTimeout(function () { piece.remove(); }, 2000);
    }
}

function toggleTheme() {
    const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    try {
        localStorage.setItem('theme', next);
    } catch (e) {
        // без хранилища тема просто не запомнится
    }
}

function renderHeroStats() {
    const published = courses.filter(function (c) { return c.status === 'PUBLISHED'; });
    const categories = new Set(published.map(function (c) { return c.categoryName; }).filter(Boolean));
    const instructors = new Set(published.map(function (c) { return c.instructorName; }).filter(Boolean));
    const free = published.filter(function (c) { return Number(c.price) === 0; }).length;
    const stats = [[published.length, 'курсов'], [categories.size, 'направления'], [instructors.size, 'преподавателя'], [free, 'бесплатно']];
    document.getElementById('hero-stats').innerHTML = stats.map(function (st) {
        return '<div class="stat"><b data-count="' + st[0] + '">0</b><span>' + st[1] + '</span></div>';
    }).join('');
    for (const el of document.querySelectorAll('#hero-stats [data-count]')) {
        animateNumber(el, Number(el.dataset.count));
    }
}

function fillCategoryFilter() {
    const select = document.getElementById('course-category');
    const current = select.value;
    const names = Array.from(new Set(visibleCourses().map(function (c) { return c.categoryName; }).filter(Boolean))).sort();
    select.innerHTML = '<option value="">Все категории</option>' +
        names.map(function (n) { return '<option' + (n === current ? ' selected' : '') + '>' + esc(n) + '</option>'; }).join('');
}

function renderCourses() {
    const list = document.getElementById('courses-list');
    const query = document.getElementById('course-search').value.trim().toLowerCase();
    const category = document.getElementById('course-category').value;
    const onlyFree = document.getElementById('course-free').checked;
    const found = visibleCourses().filter(function (c) {
        const text = (c.title + ' ' + (c.description || '') + ' ' + c.tags.join(' ')).toLowerCase();
        return (!query || text.includes(query))
            && (!category || c.categoryName === category)
            && (!onlyFree || Number(c.price) === 0);
    });
    let html = '';
    for (const c of found) {
        let tags = '';
        for (const t of c.tags) {
            tags += '<span class="tag">' + esc(t) + '</span>';
        }
        const price = Number(c.price) === 0 ? 'Бесплатно' : Number(c.price).toLocaleString('ru-RU') + ' ₽';
        const draft = c.status !== 'PUBLISHED';
        html +=
            '<div class="card course reveal' + (draft ? ' draft' : '') + '">' +
            '  <div class="cover cover-' + coverKind(c.categoryName) + '">' + COVER_ICONS[coverKind(c.categoryName)] +
            esc(c.categoryName || 'Без категории') +
            (draft ? '<span class="badge red">' + (c.status === 'DRAFT' ? 'Черновик' : 'В архиве') + '</span>' : '') + '</div>' +
            '  <h2><a href="#course/' + c.id + '">' + esc(c.title) + '</a></h2>' +
            '  <div class="meta">' + esc(c.instructorName || 'Преподаватель не назначен') +
            (c.startDate ? ' · ' + formatDate(c.startDate) + ' — ' + formatDate(c.endDate) : '') + '</div>' +
            '  <p>' + esc(c.description || '') + '</p>' +
            '  <div class="tags">' + tags + '</div>' +
            '  <div class="bottom">' +
            '    <div><div class="price">' + price + '</div>' +
            (c.maxStudents ? '<div class="hint">до ' + c.maxStudents + ' студентов</div>' : '') + '</div>' +
            '    <div class="actions">' +
            '      <a class="button-link" href="#course/' + c.id + '">Подробнее</a>' +
            (draft ? '' : '<button onclick="enroll(' + c.id + ')">Записаться</button>') +
            '    </div>' +
            '  </div>' +
            '</div>';
    }
    list.innerHTML = html || '<p class="hint">' + (visibleCourses().length === 0
        ? 'Курсов пока нет.'
        : 'Ничего не нашлось — попробуйте изменить поиск или фильтры.') + '</p>';
    list.querySelectorAll('.course.reveal').forEach(function (card, i) {
        setTimeout(function () { card.classList.add('in'); }, 60 + 80 * i);
    });
}

function skeletonCards(count) {
    let html = '';
    for (let i = 0; i < count; i++) {
        html += '<div class="card course skeleton"><div class="line w40"></div><div class="line w80"></div>' +
            '<div class="line w60"></div><div class="line"></div><div class="line w30"></div></div>';
    }
    return html;
}

function loader(text) {
    return '<p class="hint loading"><span class="spinner"></span>' + esc(text || 'Загрузка…') + '</p>';
}

// Запись: enrollment-service сам проверит студента и курс через Feign и пришлёт уведомление (notification-service).
// Сертификат выдаётся позже — когда пройдены все уроки курса
async function enroll(courseId) {
    if (!currentUser) {
        showMessage('Чтобы записаться, войдите или зарегистрируйтесь.', true);
        location.hash = '#login';
        return;
    }
    try {
        // профиль уже загружен после входа — повторно в student-service не ходим,
        // поэтому при выключенном student-service ошибку покажет сам enrollment-service (fallback)
        if (!myStudent) {
            await loadMyStudent();
        }
        if (!myStudent) {
            showMessage('Сначала заполните профиль студента.', true);
            location.hash = '#my';
            return;
        }
        await request('POST', '/enrollments?studentId=' + myStudent.id + '&courseId=' + courseId);
        let title = 'курс';
        const course = courses.find(function (c) { return c.id === courseId; });
        if (course) {
            title = course.title;
        } else if (coursePage) {
            title = coursePage.course.title;
        }
        showMessage('Вы записаны на курс «' + title + '». Отмечайте уроки на странице курса — после последнего выдадим сертификат.');
        updateBell();
        if (coursePage && coursePage.course.id === courseId) {
            await loadMyProgress(courseId);
        }
    } catch (e) {
        showMessage(e.message, true);
    }
}


// ----- Страница курса (course-service): программа, вебинары, посты, отзывы -----

async function loadCoursePage(id) {
    myEnrollment = null;
    completedLessons = [];
    document.getElementById('course-title').innerHTML = '<span class="spinner"></span>Загрузка…';
    for (const block of ['course-meta', 'course-description', 'course-tags', 'course-price', 'course-rating',
        'course-program-summary']) {
        document.getElementById(block).textContent = '';
    }
    document.getElementById('course-program').innerHTML = loader('Загружаем программу…');
    try {
        coursePage = await request('GET', '/courses/' + id + '/page');
    } catch (e) {
        document.getElementById('course-title').textContent = 'Курс не найден';
        showMessage(e.message, true);
        return;
    }
    const c = coursePage.course;
    document.getElementById('course-title').textContent = c.title;
    document.getElementById('course-meta').textContent =
        c.categoryName + ' · ' + formatDate(c.startDate) + ' — ' + formatDate(c.endDate) +
        (c.maxStudents ? ' · до ' + c.maxStudents + ' студентов' : '');
    document.getElementById('course-description').textContent = c.description;
    let tags = '';
    for (const t of c.tags) {
        tags += '<span class="tag">' + esc(t) + '</span>';
    }
    document.getElementById('course-tags').innerHTML = tags;
    document.getElementById('course-price').textContent =
        Number(c.price) === 0 ? 'Бесплатно' : Number(c.price).toLocaleString('ru-RU') + ' ₽';
    document.getElementById('course-rating').textContent = coursePage.averageRating
        ? '★ ' + coursePage.averageRating + ' (' + coursePage.reviews.length + ' отзыв.)'
        : 'Отзывов пока нет';
    document.getElementById('course-instructor').innerHTML =
        '<b>' + esc(c.instructorName || '—') + '</b><br><span class="hint">' + esc(coursePage.instructorBio || '') + '</span>';

    renderProgram();
    renderWebinars();
    renderPosts();
    renderReviews();
    loadMyProgress(c.id);
}

// Прохождение курса (enrollment-service): своя запись на этот курс и пройденные уроки
async function loadMyProgress(courseId) {
    if (!currentUser) {
        return;
    }
    try {
        if (!myStudent) {
            await loadMyStudent();
        }
        if (!myStudent) {
            return;
        }
        const enrollments = await request('GET', '/enrollments?studentId=' + myStudent.id);
        myEnrollment = enrollments.find(function (e) { return e.courseId === courseId && e.status !== 'CANCELLED'; }) || null;
        completedLessons = myEnrollment ? await request('GET', '/enrollments/' + myEnrollment.id + '/lessons') : [];
    } catch (e) {
        myEnrollment = null;
        completedLessons = [];
    }
    renderProgram();
}

async function completeLesson(lessonId) {
    try {
        myEnrollment = await request('POST', '/enrollments/' + myEnrollment.id + '/lessons/' + lessonId + '/complete');
        if (!completedLessons.includes(lessonId)) {
            completedLessons.push(lessonId);
        }
        renderProgram();
        if (myEnrollment.status === 'COMPLETED') {
            launchConfetti();
            showMessage('Курс пройден! Сертификат выпущен — он в разделе «Мои курсы».');
            updateBell();
        }
    } catch (e) {
        showMessage(e.message, true);
    }
}

function renderProgram() {
    const hours = Math.floor(coursePage.totalMinutes / 60);
    const minutes = coursePage.totalMinutes % 60;
    document.getElementById('course-program-summary').textContent = coursePage.modules.length === 0
        ? 'Программа появится ближе к старту.'
        : coursePage.modules.length + ' модул. · ' + coursePage.lessonsCount + ' урок. · ' + hours + ' ч ' + minutes + ' мин';
    const enrollButton = document.getElementById('course-enroll');
    let html = '';
    if (myEnrollment) {
        const completed = myEnrollment.status === 'COMPLETED';
        enrollButton.textContent = completed ? 'Курс пройден' : 'Вы записаны';
        enrollButton.disabled = true;
        html += '<div class="progress"><div class="progress-bar" style="width:' + myEnrollment.progress + '%"></div></div>' +
            '<p class="hint">' + (completed
                ? 'Курс пройден — сертификат в разделе <a href="#my">«Мои курсы»</a>.'
                : 'Ваш прогресс: ' + myEnrollment.progress + '%. Отмечайте пройденные уроки — после последнего выдадим сертификат.') +
            '</p>';
    } else {
        enrollButton.textContent = 'Записаться';
        enrollButton.disabled = false;
    }
    for (const m of coursePage.modules) {
        let lessons = '';
        for (const l of m.lessons) {
            let action = '';
            if (myEnrollment) {
                action = completedLessons.includes(l.id)
                    ? '<span class="badge green">✓ пройден</span>'
                    : (myEnrollment.status === 'ACTIVE'
                        ? '<button class="small secondary" onclick="completeLesson(' + l.id + ')">Пройден</button>'
                        : '');
            }
            lessons +=
                '<div class="lesson">' + (l.hasVideo ? '▶ ' : '📄 ') + esc(l.title) +
                '<span class="lesson-side">' + (l.durationMin ? l.durationMin + ' мин' : '') + action + '</span></div>';
        }
        html += '<details' + (m.position === 1 || myEnrollment ? ' open' : '') + '><summary>Модуль ' + m.position + '. ' +
            esc(m.title) + '</summary>' + lessons + '</details>';
    }
    document.getElementById('course-program').innerHTML = html;
}

function renderWebinars() {
    let html = '';
    for (const w of coursePage.webinars) {
        html += '<li><b>' + esc(w.title) + '</b><div class="meta">' + formatDateTime(w.startsAt) + '</div></li>';
    }
    document.getElementById('course-webinars').innerHTML = html || '<li class="hint">Вебинаров пока нет.</li>';
}

function renderPosts() {
    document.getElementById('post-form').classList.toggle('hidden', !canPost());
    let html = '';
    for (const p of coursePage.posts) {
        html +=
            '<div class="post"><h3>' + esc(p.title) + '</h3>' +
            '<div class="meta">' + esc(p.authorName) + ' · ' + formatDateTime(p.createdAt) + '</div>' +
            '<p>' + esc(p.text) + '</p></div>';
    }
    document.getElementById('course-posts').innerHTML = html || '<p class="hint">Новостей пока нет.</p>';
}

function renderReviews() {
    // форму видит любой вошедший; записан ли он на курс — проверит course-service
    document.getElementById('review-form').classList.toggle('hidden', currentUser === null);
    let html = '';
    for (const r of coursePage.reviews) {
        html +=
            '<div class="review"><b>' + esc(r.studentName) + '</b> <span class="rating">' + '★'.repeat(r.rating) +
            '</span><div class="meta">' + formatDate(r.createdAt) + '</div>' +
            (r.comment ? '<p>' + esc(r.comment) + '</p>' : '') + '</div>';
    }
    document.getElementById('course-reviews').innerHTML = html || '<p class="hint">Отзывов пока нет — будьте первым.</p>';
}

async function addPost(event) {
    event.preventDefault();
    const form = event.target;
    try {
        await request('POST', '/courses/' + coursePage.course.id + '/posts', {
            title: form.elements.title.value,
            text: form.elements.text.value
        });
        form.reset();
        showMessage('Пост опубликован.');
        loadCoursePage(coursePage.course.id);
    } catch (e) {
        showMessage(e.message, true);
    }
}

async function addReview(event) {
    event.preventDefault();
    const form = event.target;
    try {
        await request('POST', '/courses/' + coursePage.course.id + '/reviews', {
            rating: Number(form.elements.rating.value),
            comment: emptyToNull(form.elements.comment.value)
        });
        form.reset();
        showMessage('Спасибо за отзыв!');
        loadCoursePage(coursePage.course.id);
    } catch (e) {
        showMessage(e.message, true);
    }
}


// ----- Мои курсы: профиль (student-service), записи (enrollment-service), сертификаты (certificate-service) -----

async function loadMy() {
    document.getElementById('my-need-login').classList.toggle('hidden', currentUser !== null);
    document.getElementById('my-content').classList.toggle('hidden', currentUser === null);
    if (!currentUser) {
        return;
    }
    try {
        await loadMyStudent();
        renderProfile();
        await renderMyEnrollments();
    } catch (e) {
        showMessage(e.message, true);
    }
}

function renderProfile() {
    const view = document.getElementById('profile-view');
    const form = document.getElementById('profile-form');
    if (!myStudent) {
        view.innerHTML = '';
        form.classList.remove('hidden');
        return;
    }
    form.classList.add('hidden');
    view.innerHTML =
        '<p><b>' + esc(myStudent.lastName) + ' ' + esc(myStudent.firstName) + '</b> · ' +
        esc(currentUser.email) + '</p>' +
        '<p class="hint">Группа: ' + esc(myStudent.groupName || 'без группы') +
        ' · Телефон: ' + esc(myStudent.phone || '—') +
        ' · Дата рождения: ' + formatDate(myStudent.birthDate) + '</p>';
}

async function saveProfile(event) {
    event.preventDefault();
    const form = event.target;
    try {
        await request('POST', '/students', {
            firstName: form.elements.firstName.value,
            lastName: form.elements.lastName.value,
            phone: emptyToNull(form.elements.phone.value),
            birthDate: emptyToNull(form.elements.birthDate.value),
            groupId: null,
            userId: currentUser.id,
            status: 'ACTIVE'
        });
        form.reset();
        showMessage('Профиль сохранён — теперь можно записываться на курсы.');
        loadMy();
    } catch (e) {
        showMessage(e.message, true);
    }
}

async function renderMyEnrollments() {
    const body = document.getElementById('my-enrollments');
    if (!myStudent) {
        body.innerHTML = '<tr><td colspan="5" class="hint">Нет профиля студента.</td></tr>';
        return;
    }
    body.innerHTML = '<tr><td colspan="5">' + loader('Загружаем ваши курсы…') + '</td></tr>';
    if (courses.length === 0) {
        courses = await request('GET', '/courses');
    }
    const enrollments = await request('GET', '/enrollments?studentId=' + myStudent.id);
    if (enrollments.length === 0) {
        body.innerHTML = '<tr><td colspan="5" class="hint">Вы ещё не записаны ни на один курс — <a href="#courses">к каталогу</a>.</td></tr>';
        return;
    }

    let html = '';
    for (const e of enrollments) {
        const course = courses.find(function (c) { return c.id === e.courseId; });
        // сертификат по записи; если certificate-service выключен — просто не показываем номер
        let certificate = '—';
        try {
            const list = await request('GET', '/certificates?enrollmentId=' + e.id);
            if (list.length > 0) {
                certificate = '<a href="#verify" onclick="prefillVerify(\'' + esc(list[0].number) + '\')">' +
                    esc(list[0].number) + '</a>';
            }
        } catch (err) {
            certificate = '<span class="hint">недоступен</span>';
        }
        html +=
            '<tr>' +
            '<td><a href="#course/' + e.courseId + '">' + esc(course ? course.title : 'Курс #' + e.courseId) + '</a></td>' +
            '<td><span class="badge green">' + esc(ENROLLMENT_STATUS[e.status] || e.status) + '</span></td>' +
            '<td>' + e.progress + '%</td>' +
            '<td>' + formatDate(e.enrolledAt) + '</td>' +
            '<td>' + certificate + '</td>' +
            '</tr>';
    }
    body.innerHTML = html;
}


// ----- Поддержка (support-service) -----

async function loadSupport() {
    try {
        const faq = await request('GET', '/support/faq');
        let html = '';
        for (const f of faq) {
            html += '<details><summary>' + esc(f.title) + '</summary><p>' + esc(f.body) + '</p></details>';
        }
        document.getElementById('faq-list').innerHTML = html || '<p class="hint">Пока пусто.</p>';
    } catch (e) {
        document.getElementById('faq-list').innerHTML = '<p class="hint">' + esc(e.message) + '</p>';
    }

    document.getElementById('support-need-login').classList.toggle('hidden', currentUser !== null);
    document.getElementById('support-content').classList.toggle('hidden', currentUser === null);
    if (currentUser) {
        await loadTickets();
    }
}

async function loadTickets() {
    const list = document.getElementById('tickets-list');
    try {
        const tickets = await request('GET', '/support/tickets?authorId=' + currentUser.id);
        let html = '';
        for (const t of tickets) {
            const selected = t.id === selectedTicketId ? ' selected' : '';
            html +=
                '<li class="clickable' + selected + '" onclick="openTicket(' + t.id + ')">' +
                '<b>' + esc(t.subject) + '</b> ' + ticketBadge(t.status) +
                '<div class="meta">№' + t.id + ' · ' + formatDateTime(t.createdAt) + '</div>' +
                '</li>';
        }
        list.innerHTML = html || '<li class="hint">Обращений нет.</li>';
    } catch (e) {
        list.innerHTML = '<li class="hint">' + esc(e.message) + '</li>';
    }
}

function ticketBadge(status) {
    let color = '';
    if (status === 'RESOLVED' || status === 'CLOSED') {
        color = ' green';
    }
    return '<span class="badge' + color + '">' + esc(TICKET_STATUS[status] || status) + '</span>';
}

async function createTicket(event) {
    event.preventDefault();
    const form = event.target;
    try {
        const ticket = await request('POST', '/support/tickets', {
            authorId: currentUser.id,
            subject: form.elements.subject.value,
            category: form.elements.category.value,
            priority: 'MEDIUM',
            text: form.elements.text.value
        });
        form.reset();
        showMessage('Обращение №' + ticket.id + ' отправлено.');
        await openTicket(ticket.id);
    } catch (e) {
        showMessage(e.message, true);
    }
}

async function openTicket(id) {
    selectedTicketId = id;
    try {
        const ticket = await request('GET', '/support/tickets/' + id);
        const messages = await request('GET', '/support/tickets/' + id + '/messages');
        document.getElementById('ticket-title').innerHTML = esc(ticket.subject) + ' ' + ticketBadge(ticket.status);
        document.getElementById('ticket-hint').classList.add('hidden');

        let html = '';
        for (const m of messages) {
            const mine = m.authorId === currentUser.id;
            html +=
                '<div class="msg' + (mine ? ' mine' : '') + '">' +
                '<div class="meta">' + (mine ? 'Вы' : 'Поддержка') + ' · ' + formatDateTime(m.createdAt) + '</div>' +
                esc(m.text) +
                '</div>';
        }
        document.getElementById('ticket-messages').innerHTML = html;
        document.getElementById('reply-form').classList.toggle('hidden', ticket.status === 'CLOSED');
        await loadTickets();
    } catch (e) {
        showMessage(e.message, true);
    }
}

async function sendReply(event) {
    event.preventDefault();
    const form = event.target;
    try {
        await request('POST', '/support/tickets/' + selectedTicketId + '/messages', {
            authorId: currentUser.id,
            text: form.elements.text.value
        });
        form.reset();
        await openTicket(selectedTicketId);
    } catch (e) {
        showMessage(e.message, true);
    }
}

async function resolveTicket() {
    try {
        await request('PATCH', '/support/tickets/' + selectedTicketId + '/status', { status: 'RESOLVED' });
        showMessage('Отлично, обращение отмечено решённым.');
        await openTicket(selectedTicketId);
    } catch (e) {
        showMessage(e.message, true);
    }
}


// ----- Уведомления (notification-service) -----

async function loadNotifications() {
    const list = document.getElementById('notifications-list');
    if (!currentUser) {
        list.innerHTML = '<li>Чтобы видеть уведомления, <a href="#login">войдите</a>.</li>';
        return;
    }
    try {
        const notifications = await request('GET', '/notifications?userId=' + currentUser.id);
        let html = '';
        for (const n of notifications) {
            let button = '';
            if (!n.read) {
                button = ' <button class="small secondary" onclick="markRead(' + n.id + ')">Прочитано</button>';
            }
            html +=
                '<li class="' + (n.read ? '' : 'unread') + '">' +
                esc(n.text) + button +
                '<div class="meta">' + formatDateTime(n.createdAt) + '</div>' +
                '</li>';
        }
        list.innerHTML = html || '<li class="hint">Уведомлений нет.</li>';
    } catch (e) {
        list.innerHTML = '<li class="hint">' + esc(e.message) + '</li>';
    }
}

async function markRead(id) {
    try {
        await request('PATCH', '/notifications/' + id + '/read');
        await loadNotifications();
        updateBell();
    } catch (e) {
        showMessage(e.message, true);
    }
}


// ----- Студенты (student-service, полный CRUD) -----

async function loadStudents() {
    const body = document.getElementById('students-list');
    try {
        students = await request('GET', '/students');
    } catch (e) {
        body.innerHTML = '<tr><td colspan="7" class="hint">' + esc(e.message) + '</td></tr>';
        return;
    }
    let html = '';
    for (const s of students) {
        const blocked = s.status === 'BLOCKED' ? ' red' : ' green';
        html +=
            '<tr>' +
            '<td>' + s.id + '</td>' +
            '<td>' + esc(s.lastName) + ' ' + esc(s.firstName) + '</td>' +
            '<td>' + esc(s.groupName || '—') + '</td>' +
            '<td>' + esc(s.phone || '—') + '</td>' +
            '<td>' + formatDate(s.birthDate) + '</td>' +
            '<td><span class="badge' + blocked + '">' + esc(STUDENT_STATUS[s.status] || s.status) + '</span></td>' +
            '<td>' +
            '  <button class="small secondary" onclick="editStudent(' + s.id + ')">Изменить</button> ' +
            '  <button class="small danger" onclick="deleteStudent(' + s.id + ')">Удалить</button>' +
            '</td>' +
            '</tr>';
    }
    body.innerHTML = html;
    fillGroups();
}

// Отдельного эндпоинта для групп нет — собираем их из списка студентов
function fillGroups() {
    const select = document.getElementById('group-select');
    const current = select.value;
    let html = '<option value="">Без группы</option>';
    const seen = [];
    for (const s of students) {
        if (s.groupId && !seen.includes(s.groupId)) {
            seen.push(s.groupId);
            html += '<option value="' + s.groupId + '">' + esc(s.groupName) + '</option>';
        }
    }
    select.innerHTML = html;
    select.value = current;
}

function editStudent(id) {
    editingStudent = students.find(function (s) { return s.id === id; });
    const form = document.getElementById('student-form');
    form.elements.firstName.value = editingStudent.firstName;
    form.elements.lastName.value = editingStudent.lastName;
    form.elements.phone.value = editingStudent.phone || '';
    form.elements.birthDate.value = editingStudent.birthDate || '';
    form.elements.groupId.value = editingStudent.groupId || '';
    form.elements.status.value = editingStudent.status;
    document.getElementById('student-form-title').textContent =
        'Изменить: ' + editingStudent.lastName + ' ' + editingStudent.firstName;
    document.getElementById('student-cancel').classList.remove('hidden');
    form.scrollIntoView({ behavior: 'smooth' });
}

function cancelEdit() {
    editingStudent = null;
    document.getElementById('student-form').reset();
    document.getElementById('student-form-title').textContent = 'Новый студент';
    document.getElementById('student-cancel').classList.add('hidden');
}

async function saveStudent(event) {
    event.preventDefault();
    const form = event.target;
    const body = {
        firstName: form.elements.firstName.value,
        lastName: form.elements.lastName.value,
        phone: emptyToNull(form.elements.phone.value),
        birthDate: emptyToNull(form.elements.birthDate.value),
        groupId: form.elements.groupId.value ? Number(form.elements.groupId.value) : null,
        // PUT заменяет все поля — привязку к пользователю сайта сохраняем
        userId: editingStudent ? editingStudent.userId : null,
        status: form.elements.status.value
    };
    try {
        if (editingStudent) {
            await request('PUT', '/students/' + editingStudent.id, body);
            showMessage('Студент обновлён.');
        } else {
            await request('POST', '/students', body);
            showMessage('Студент добавлен.');
        }
        cancelEdit();
        loadStudents();
    } catch (e) {
        showMessage(e.message, true);
    }
}

async function deleteStudent(id) {
    const s = students.find(function (x) { return x.id === id; });
    if (!confirm('Удалить студента ' + s.lastName + ' ' + s.firstName + '?')) {
        return;
    }
    try {
        await request('DELETE', '/students/' + id);
        showMessage('Студент удалён.');
        loadStudents();
    } catch (e) {
        showMessage(e.message, true);
    }
}


// ----- Проверка сертификата (certificate-service) -----

function prefillVerify(number) {
    document.getElementById('verify-form').elements.number.value = number;
    setTimeout(function () { checkCertificate(); }, 0);
}

async function checkCertificate(event) {
    if (event) {
        event.preventDefault();
    }
    const number = document.getElementById('verify-form').elements.number.value.trim();
    const result = document.getElementById('verify-result');
    if (!number) {
        return;
    }
    try {
        const c = await request('GET', '/certificates/' + encodeURIComponent(number));
        result.innerHTML =
            '<p><span class="badge green">Действителен</span></p>' +
            '<p>Сертификат <b>' + esc(c.number) + '</b> выдан ' + formatDate(c.issuedAt) +
            ' по записи на курс №' + c.enrollmentId + ' (шаблон «' + esc(c.templateName) + '»).</p>';
    } catch (e) {
        result.innerHTML = '<p><span class="badge red">Не найден</span></p><p>' + esc(e.message) + '</p>';
    }
}


// ===================== старт =====================

document.getElementById('login-form').addEventListener('submit', login);
document.getElementById('register-form').addEventListener('submit', register);
document.getElementById('profile-form').addEventListener('submit', saveProfile);
document.getElementById('ticket-form').addEventListener('submit', createTicket);
document.getElementById('reply-form').addEventListener('submit', sendReply);
document.getElementById('close-ticket').addEventListener('click', resolveTicket);
document.getElementById('student-form').addEventListener('submit', saveStudent);
document.getElementById('student-cancel').addEventListener('click', cancelEdit);
document.getElementById('verify-form').addEventListener('submit', checkCertificate);
document.getElementById('post-form').addEventListener('submit', addPost);
document.getElementById('review-form').addEventListener('submit', addReview);
document.getElementById('course-enroll').addEventListener('click', function () { enroll(coursePage.course.id); });
document.getElementById('theme-toggle').addEventListener('click', toggleTheme);
document.getElementById('course-search').addEventListener('input', renderCourses);
document.getElementById('course-category').addEventListener('change', renderCourses);
document.getElementById('course-free').addEventListener('change', renderCourses);
window.addEventListener('hashchange', showPage);

renderUserBox();
showPage();
