/* ============================================================
   KSC Search — клиентская логика (прототип, данные-заглушки)

   Бэкенда нет: «Искать» имитирует запрос к серверу с задержкой и
   возвращает заранее заготовленный список. Источник данных позже
   заменят на реальный API без переделки интерфейса — для этого вся
   «сетевая» часть изолирована в функции searchBugs().
   ============================================================ */

'use strict';

/* ─────────────── Справочники фильтров ─────────────── */

// Список итераций (мультивыбор — набор галочек, НЕ диапазон «от-до»).
const ITERATIONS = ['16.1', '16.2', '16.3', '16.10'];

// Категории статуса. Одна категория -> несколько реальных значений state.
// Конкретный маппинг уточнят позже; здесь — рабочая заготовка.
const STATE_CATEGORIES = [
  { id: 'open', label: 'Открытые', states: ['Active', 'New', 'Open', 'Proposed'] },
  { id: 'resolved', label: 'Resolved', states: ['Resolved', 'Fixed'] },
  { id: 'closed', label: 'Закрытые', states: ['Closed', 'Done', 'Removed'] },
];

// Тип Work Item. Пока только Bug, список заложен на расширение.
const WORK_ITEM_TYPES = ['Bug'];

/* ─────────────── Данные-заглушка ─────────────── */
// Формат ориентировочный — набор полей соответствует будущему контракту API.
const MOCK_BUGS = [
  {
    id: 482931,
    title: 'При сохранении политики ошибка 5053',
    url: 'https://hqrndtfs.avp.ru/.../482931',
    similarity: 0.94,
    work_item_type: 'Bug',
    iteration_path: 'Corp-SecurityCenter\\16.3',
    state: 'Active',
    product_version: '16.3.0.123',
    description: 'При попытке сохранить политику безопасности консоль возвращает ошибку 5053. Воспроизводится на чистой инсталляции.',
  },
  {
    id: 482015,
    title: 'Ошибка 5053 при применении политики к группе устройств',
    url: 'https://hqrndtfs.avp.ru/.../482015',
    similarity: 0.88,
    work_item_type: 'Bug',
    iteration_path: 'Corp-SecurityCenter\\16.3',
    state: 'Resolved',
    product_version: '16.3.0.118',
    description: 'Политика не применяется к группе, в логах сервера администрирования код 5053.',
  },
  {
    id: 480774,
    title: 'Сохранение профиля политики завершается таймаутом',
    url: 'https://hqrndtfs.avp.ru/.../480774',
    similarity: 0.79,
    work_item_type: 'Bug',
    iteration_path: 'Corp-SecurityCenter\\16.2',
    state: 'Active',
    product_version: '16.2.0.090',
    description: 'При большом числе настроек сохранение политики занимает >60 секунд и падает по таймауту.',
  },
  {
    id: 481200,
    title: 'Кастомные сертификаты в файле автоответов — консоль не открывается',
    url: 'https://hqrndtfs.avp.ru/.../481200',
    similarity: 0.71,
    work_item_type: 'Bug',
    iteration_path: 'Corp-SecurityCenter\\16.2',
    state: 'Closed',
    product_version: '16.2.0.045',
    description: 'После добавления кастомных сертификатов в файл автоответов веб-консоль перестаёт открываться.',
  },
  {
    id: 479500,
    title: 'Дублирование задач при синхронизации с обновлением 16.1',
    url: 'https://hqrndtfs.avp.ru/.../479500',
    similarity: 0.64,
    work_item_type: 'Bug',
    iteration_path: 'Corp-SecurityCenter\\16.1',
    state: 'Closed',
    product_version: '16.1.0.210',
    description: 'После обновления до 16.1 задачи синхронизации создаются в двух экземплярах.',
  },
  {
    id: 483640,
    title: 'Экспорт отчёта по уязвимостям обрывается на 16.10',
    url: 'https://hqrndtfs.avp.ru/.../483640',
    similarity: 0.52,
    work_item_type: 'Bug',
    iteration_path: 'Corp-SecurityCenter\\16.10',
    state: 'New',
    product_version: '16.10.0.012',
    description: 'Выгрузка отчёта по уязвимостям в PDF прерывается без сообщения об ошибке.',
  },
  {
    id: 478880,
    title: 'Некорректное отображение статуса агента после перезагрузки',
    url: 'https://hqrndtfs.avp.ru/.../478880',
    similarity: 0.41,
    work_item_type: 'Bug',
    iteration_path: 'Corp-SecurityCenter\\16.10',
    state: 'Resolved',
    product_version: '16.10.0.008',
    description: 'После перезагрузки управляемого устройства его статус в консоли «Не в сети» до ручного обновления.',
  },
];

/* ─────────────── Состояние UI ─────────────── */
const selected = {
  iterations: new Set(),
  states: new Set(),
  types: new Set(['Bug']), // Bug выбран по умолчанию (единственный тип)
};

/* ─────────────── DOM ─────────────── */
const els = {
  iterationChips: document.getElementById('iterationChips'),
  stateChips: document.getElementById('stateChips'),
  typeChips: document.getElementById('typeChips'),
  query: document.getElementById('queryInput'),
  queryHint: document.getElementById('queryHint'),
  searchBtn: document.getElementById('searchBtn'),
  resultsBody: document.getElementById('resultsBody'),
  resultsCount: document.getElementById('resultsCount'),
};

/* ─────────────── Утилиты ─────────────── */

// Достаём номер итерации из iteration_path: "Corp-SecurityCenter\16.3" -> "16.3"
function iterationOf(bug) {
  const parts = bug.iteration_path.split('\\');
  return parts[parts.length - 1];
}

// Категория статуса по сырому значению state.
function stateCategory(rawState) {
  const found = STATE_CATEGORIES.find((c) => c.states.includes(rawState));
  return found ? found.id : null;
}

function simLevel(similarity) {
  if (similarity >= 0.8) return 'high';
  if (similarity >= 0.55) return 'mid';
  return 'low';
}

function simColor(level) {
  return { high: 'var(--high)', mid: 'var(--mid)', low: 'var(--low)' }[level];
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

/* ─────────────── Рендер фильтров ─────────────── */

function makeChip(label, isPressed, onToggle, disabled = false) {
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'chip';
  btn.textContent = label;
  btn.setAttribute('aria-pressed', String(isPressed));
  if (disabled) btn.disabled = true;
  btn.addEventListener('click', () => {
    const next = btn.getAttribute('aria-pressed') !== 'true';
    btn.setAttribute('aria-pressed', String(next));
    onToggle(next);
  });
  return btn;
}

function renderFilters() {
  ITERATIONS.forEach((it) => {
    els.iterationChips.appendChild(
      makeChip(it, selected.iterations.has(it), (on) => {
        on ? selected.iterations.add(it) : selected.iterations.delete(it);
      })
    );
  });

  STATE_CATEGORIES.forEach((cat) => {
    els.stateChips.appendChild(
      makeChip(cat.label, selected.states.has(cat.id), (on) => {
        on ? selected.states.add(cat.id) : selected.states.delete(cat.id);
      })
    );
  });

  WORK_ITEM_TYPES.forEach((type) => {
    els.typeChips.appendChild(
      makeChip(type, selected.types.has(type), (on) => {
        on ? selected.types.add(type) : selected.types.delete(type);
      })
    );
  });
}

/* ─────────────── Кнопка / поле запроса ─────────────── */

function syncSearchButton() {
  const hasQuery = els.query.value.trim().length > 0;
  els.searchBtn.disabled = !hasQuery;
  els.queryHint.textContent = hasQuery
    ? 'Готово к поиску'
    : 'Опишите проблему, чтобы начать поиск';
}

/* ─────────────── Имитация запроса к серверу ───────────────
   Здесь позже подключат реальный API. Контракт функции:
   принимает { query, filters }, возвращает Promise<массив багов>.
   Сейчас — задержка 0.5–1с и фильтрация заглушки на клиенте. */

function searchBugs({ filters }) {
  return new Promise((resolve, reject) => {
    const delay = 500 + Math.random() * 500;
    setTimeout(() => {
      // Демонстрационная имитация ошибки сети (~8% запросов),
      // чтобы можно было проверить состояние «Ошибка».
      if (Math.random() < 0.08) {
        reject(new Error('network'));
        return;
      }

      let list = MOCK_BUGS.slice();

      // Фильтр по типу WI.
      if (filters.types.length) {
        list = list.filter((b) => filters.types.includes(b.work_item_type));
      }
      // Фильтр по итерациям (набор галочек).
      if (filters.iterations.length) {
        list = list.filter((b) => filters.iterations.includes(iterationOf(b)));
      }
      // Фильтр по категориям статуса.
      if (filters.states.length) {
        list = list.filter((b) => filters.states.includes(stateCategory(b.state)));
      }

      // Сортировка по похожести (самые похожие сверху).
      list.sort((a, b) => b.similarity - a.similarity);
      resolve(list);
    }, delay);
  });
}

/* ─────────────── Рендер состояний результатов ─────────────── */

function setCount(n) {
  if (n > 0) {
    els.resultsCount.hidden = false;
    els.resultsCount.textContent = `найдено ${n} ${plural(n, 'похожий', 'похожих', 'похожих')}`;
  } else {
    els.resultsCount.hidden = true;
  }
}

function plural(n, one, few, many) {
  const m10 = n % 10;
  const m100 = n % 100;
  if (m10 === 1 && m100 !== 11) return one;
  if (m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20)) return few;
  return many;
}

function showLoading() {
  setCount(0);
  let html = '';
  for (let i = 0; i < 3; i++) {
    html += `
      <div class="skel">
        <div class="skel__circle"></div>
        <div class="skel__lines">
          <div class="skel__line w70"></div>
          <div class="skel__line w40"></div>
          <div class="skel__line w90"></div>
        </div>
      </div>`;
  }
  els.resultsBody.innerHTML = html;
}

function showEmpty() {
  setCount(0);
  els.resultsBody.innerHTML = `
    <div class="state">
      <div class="state__icon">🗂️</div>
      <p class="state__text">Похожих багов не найдено.</p>
      <p class="state__sub">Попробуйте изменить формулировку или ослабить фильтры.</p>
    </div>`;
}

function showError() {
  setCount(0);
  els.resultsBody.innerHTML = `
    <div class="state state--error">
      <div class="state__icon">⚠️</div>
      <p class="state__text">Не удалось выполнить поиск.</p>
      <p class="state__sub">Проверьте подключение и попробуйте ещё раз.</p>
      <button class="btn btn--ghost" type="button" id="retryBtn">Повторить</button>
    </div>`;
  document.getElementById('retryBtn').addEventListener('click', runSearch);
}

function stateTagClass(catId) {
  return catId ? `tag--state-${catId}` : '';
}

function stateTagLabel(rawState) {
  const cat = STATE_CATEGORIES.find((c) => c.states.includes(rawState));
  return cat ? `${cat.label} · ${rawState}` : rawState;
}

function renderBug(bug) {
  const pct = Math.round(bug.similarity * 100);
  const level = simLevel(bug.similarity);
  const color = simColor(level);
  const catId = stateCategory(bug.state);

  const el = document.createElement('article');
  el.className = 'bug';
  el.innerHTML = `
    <div class="bug__sim">
      <div class="bug__ring" style="--pct:${pct}; --col:${color}">
        <span style="color:${color}">${pct}%</span>
      </div>
      <div class="bug__simlabel">похожесть</div>
    </div>
    <div class="bug__main">
      <a class="bug__title" href="${escapeHtml(bug.url)}" target="_blank" rel="noopener noreferrer">
        ${escapeHtml(bug.title)}
        <span class="bug__id">#${bug.id}</span>
        <span class="ext">↗</span>
      </a>
      <div class="bug__meta">
        <span class="tag tag--type">${escapeHtml(bug.work_item_type)}</span>
        <span class="tag">итерация <b>${escapeHtml(iterationOf(bug))}</b></span>
        <span class="tag ${stateTagClass(catId)}">статус <b>${escapeHtml(stateTagLabel(bug.state))}</b></span>
        <span class="tag">версия <b>${escapeHtml(bug.product_version)}</b></span>
      </div>
      ${bug.description ? `<p class="bug__desc">${escapeHtml(bug.description)}</p>` : ''}
    </div>`;
  return el;
}

function showResults(list) {
  if (!list.length) {
    showEmpty();
    return;
  }
  setCount(list.length);
  els.resultsBody.innerHTML = '';
  list.forEach((bug) => els.resultsBody.appendChild(renderBug(bug)));
}

/* ─────────────── Запуск поиска ─────────────── */

let isSearching = false;

async function runSearch() {
  const query = els.query.value.trim();
  if (!query || isSearching) return;

  isSearching = true;
  els.searchBtn.classList.add('is-loading');
  els.searchBtn.disabled = true;
  showLoading();

  const filters = {
    iterations: [...selected.iterations],
    states: [...selected.states],
    types: [...selected.types],
  };

  try {
    const list = await searchBugs({ query, filters });
    showResults(list);
  } catch (err) {
    showError();
  } finally {
    isSearching = false;
    els.searchBtn.classList.remove('is-loading');
    syncSearchButton();
  }
}

/* ─────────────── Инициализация ─────────────── */

function init() {
  renderFilters();
  syncSearchButton();

  els.query.addEventListener('input', syncSearchButton);
  els.searchBtn.addEventListener('click', runSearch);

  // Ctrl/Cmd + Enter — быстрый запуск поиска из поля.
  els.query.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      runSearch();
    }
  });
}

document.addEventListener('DOMContentLoaded', init);
