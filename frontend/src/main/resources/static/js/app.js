// Сайт онлайн-школы. Все данные — только через api-gateway: http://localhost:8080/api/<сервис>/...
// Gateway по префиксу пути отправляет запрос нужному сервису (адрес сервиса берёт из Eureka).

const API = 'http://localhost:8080/api';

let currentUser = loadUser();   // вошедший пользователь из auth-service: {id, email, roles}
let myStudent = null;           // профиль студента текущего пользователя (student-service)
let courses = [];               // каталог курсов (course-service)
let students = [];              // все студенты (student-service)
let editingStudent = null;      // студент, которого сейчас редактируем на странице «Студенты»
let selectedTicketId = null;    // открытое обращение на странице «Поддержка»

const ENROLLMENT_STATUS = { ACTIVE: 'Учится', COMPLETED: 'Завершён', CANCELLED: 'Отменён' };
const TICKET_STATUS = { OPEN: 'Открыто', IN_PROGRESS: 'В работе', RESOLVED: 'Решено', CLOSED: 'Закрыто' };
const STUDENT_STATUS = { ACTIVE: 'Активен', BLOCKED: 'Заблокирован' };


// ===================== общее =====================

// Запрос к API через gateway. При ошибке выбрасывает Error с понятным текстом.
async function request(method, path, body) {
    const options = { method: method, headers: {} };
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

function loadUser() {
    try {
        return JSON.parse(localStorage.getItem('user'));
    } catch (e) {
        return null;
    }
}

function setUser(user) {
    currentUser = user;
    myStudent = null;
    try {
        if (user) {
            localStorage.setItem('user', JSON.stringify(user));
        } else {
            localStorage.removeItem('user');
        }
    } catch (e) {
        // localStorage может быть недоступен — тогда вход живёт до перезагрузки страницы
    }
    renderUserBox();
}

function renderUserBox() {
    const box = document.getElementById('user-box');
    if (!currentUser) {
        box.innerHTML = '<a href="#login">Войти</a>';
        return;
    }
    box.innerHTML =
        '<a class="bell" href="#notifications" title="Уведомления">🔔<b id="bell-count" class="hidden"></b></a>' +
        '<span class="email">' + esc(currentUser.email) + '</span>' +
        '<button class="small secondary" onclick="logout()">Выйти</button>';
    updateBell();
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
        const user = await request('POST', '/auth/login', {
            email: form.elements.email.value,
            password: form.elements.password.value
        });
        setUser(user);
        form.reset();
        showMessage('Добро пожаловать, ' + user.email + '!');
        location.hash = '#my';
    } catch (e) {
        showMessage(e.message, true);
    }
}

async function register(event) {
    event.preventDefault();
    const form = event.target;
    try {
        const user = await request('POST', '/auth/register', {
            email: form.elements.email.value,
            password: form.elements.password.value
        });
        setUser(user);
        form.reset();
        showMessage('Аккаунт создан. Заполните профиль студента, чтобы записываться на курсы.');
        location.hash = '#my';
    } catch (e) {
        showMessage(e.message, true);
    }
}

function logout() {
    setUser(null);
    location.hash = '#courses';
}

// Профиль студента текущего пользователя: студент, у которого userId = id пользователя
async function loadMyStudent() {
    students = await request('GET', '/students');
    myStudent = null;
    for (const s of students) {
        if (s.userId === currentUser.id) {
            myStudent = s;
        }
    }
}


// ===================== страницы =====================

const PAGES = ['courses', 'my', 'support', 'notifications', 'students', 'verify', 'login'];

function showPage() {
    let name = location.hash.replace('#', '');
    if (!PAGES.includes(name)) {
        name = 'courses';
    }
    for (const page of PAGES) {
        document.getElementById('page-' + page).classList.toggle('hidden', page !== name);
    }
    for (const link of document.querySelectorAll('.nav a')) {
        link.classList.toggle('active', link.dataset.page === name);
    }

    if (name === 'courses') {
        loadCourses();
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
    try {
        courses = await request('GET', '/courses');
    } catch (e) {
        list.innerHTML = '<p class="hint">' + esc(e.message) + '</p>';
        return;
    }
    let html = '';
    for (const c of courses) {
        let tags = '';
        for (const t of c.tags) {
            tags += '<span class="tag">' + esc(t) + '</span>';
        }
        const price = Number(c.price) === 0 ? 'Бесплатно' : Number(c.price).toLocaleString('ru-RU') + ' ₽';
        html +=
            '<div class="card course">' +
            '  <h2>' + esc(c.title) + '</h2>' +
            '  <div class="meta">' + esc(c.categoryName) + ' · ' + esc(c.instructorName) +
            '    · ' + formatDate(c.startDate) + ' — ' + formatDate(c.endDate) + '</div>' +
            '  <p>' + esc(c.description) + '</p>' +
            '  <div class="tags">' + tags + '</div>' +
            '  <div class="bottom">' +
            '    <span class="price">' + price + '</span>' +
            '    <button onclick="enroll(' + c.id + ')">Записаться</button>' +
            '  </div>' +
            '</div>';
    }
    list.innerHTML = html || '<p class="hint">Курсов пока нет.</p>';
}

// Запись: enrollment-service сам проверит студента и курс через Feign,
// выпустит сертификат (certificate-service) и пришлёт уведомление (notification-service)
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
        const course = courses.find(function (c) { return c.id === courseId; });
        showMessage('Вы записаны на курс «' + course.title + '». Сертификат — в разделе «Мои курсы».');
        updateBell();
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
            '<td>' + esc(course ? course.title : 'Курс #' + e.courseId) + '</td>' +
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
window.addEventListener('hashchange', showPage);

renderUserBox();
showPage();
