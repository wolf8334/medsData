/* 公共工具：请求、提示、弹窗、日历、格式化 */

const Api = {
  async request(method, url, body) {
    const opts = { method, headers: {} };
    if (body !== undefined) {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
    let res;
    try {
      res = await fetch(url, opts);
    } catch (e) {
      throw new Error('无法连接服务器');
    }
    let json;
    try {
      json = await res.json();
    } catch (e) {
      throw new Error('服务器返回异常 (' + res.status + ')');
    }
    if (json.code !== 0) {
      throw new Error(json.message || '请求失败');
    }
    return json.data;
  },
  get(url) { return this.request('GET', url); },
  post(url, body) { return this.request('POST', url, body); },
  put(url, body) { return this.request('PUT', url, body); },
  del(url) { return this.request('DELETE', url); }
};

function toast(message, type) {
  let wrap = document.getElementById('toast-wrap');
  if (!wrap) {
    wrap = document.createElement('div');
    wrap.id = 'toast-wrap';
    document.body.appendChild(wrap);
  }
  const el = document.createElement('div');
  el.className = 'toast' + (type ? ' ' + type : '');
  el.textContent = message;
  wrap.appendChild(el);
  setTimeout(() => el.remove(), 2600);
}

function esc(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function pad2(n) { return String(n).padStart(2, '0'); }

function ymd(date) {
  return date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate());
}

function todayStr() { return ymd(new Date()); }

function tomorrowStr() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return ymd(d);
}

function fmtDateTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 16);
}

function fmtTime(value) {
  if (!value) return '';
  const s = String(value);
  return s.length >= 16 ? s.slice(11, 16) : s;
}

function periodLabel(code) {
  return code === 'EARLY' ? '早' : code === 'EVENING' ? '晚' : code;
}

function planStatusTag(status) {
  const map = {
    CURRENT: ['当前', 'tag-green'],
    PENDING: ['待生效', 'tag-orange'],
    HISTORY: ['历史', 'tag-grey']
  };
  const item = map[status] || [status, 'tag-grey'];
  return '<span class="tag ' + item[1] + '">' + item[0] + '</span>';
}

function statusTag(status) {
  return status === 'ENABLED'
    ? '<span class="tag tag-green">启用</span>'
    : '<span class="tag tag-grey">停用</span>';
}

function money(value) {
  if (value === null || value === undefined) return '';
  return Number(value).toString();
}

function hasPack(packUnit, packSize) {
  return !!packUnit && packSize != null && Number(packSize) > 0;
}

function splitStock(qty, packSize) {
  qty = Number(qty || 0);
  const size = packSize != null && Number(packSize) > 0 ? Number(packSize) : 0;
  if (!size || qty < 0) return { packs: 0, loose: qty };
  const packs = Math.floor(qty / size);
  const loose = Math.round((qty - packs * size) * 1000) / 1000;
  return { packs, loose };
}

function formatStock(qty, baseUnit, packUnit, packSize) {
  qty = Number(qty || 0);
  if (!hasPack(packUnit, packSize) || qty < 0) {
    return money(qty) + (baseUnit ? ' ' + baseUnit : '');
  }
  const b = splitStock(qty, packSize);
  const parts = [];
  if (b.packs > 0) parts.push(b.packs + ' ' + packUnit);
  if (b.loose > 0) parts.push(money(b.loose) + ' ' + baseUnit);
  if (!parts.length) parts.push('0 ' + baseUnit);
  return parts.join(' ');
}

function ensureModal() {
  if (document.getElementById('app-modal')) return;
  const dlg = document.createElement('dialog');
  dlg.id = 'app-modal';
  dlg.className = 'modal';
  dlg.innerHTML = '<div class="modal-head"><h3 id="modal-title"></h3>' +
    '<button type="button" class="icon-btn" id="modal-close">✕</button></div>' +
    '<div id="modal-body"></div>';
  document.body.appendChild(dlg);
  dlg.querySelector('#modal-close').onclick = () => dlg.close();
  dlg.addEventListener('click', e => { if (e.target === dlg) dlg.close(); });
}

function openModal(title, bodyHtml) {
  ensureModal();
  const dlg = document.getElementById('app-modal');
  document.getElementById('modal-title').textContent = title;
  document.getElementById('modal-body').innerHTML = bodyHtml;
  dlg.showModal();
  return dlg;
}

function closeModal() {
  const dlg = document.getElementById('app-modal');
  if (dlg) dlg.close();
}

/**
 * 渲染月历。
 * opts: { year, month, selected, marks, onSelect(dateStr), onMonth(delta) }
 * marks: { 'YYYY-MM-DD': { expected, taken, completed } }
 */
function renderMonthCalendar(container, opts) {
  const year = opts.year, month = opts.month;
  const first = new Date(year, month - 1, 1);
  const lastDay = new Date(year, month, 0).getDate();
  const startDow = (first.getDay() + 6) % 7;
  const today = todayStr();
  const marks = opts.marks || {};

  let html = '<div class="cal-head">' +
    '<button type="button" class="btn-sm" data-nav="-1">‹</button>' +
    '<div class="cal-title">' + year + ' 年 ' + month + ' 月</div>' +
    '<button type="button" class="btn-sm" data-nav="1">›</button></div>' +
    '<div class="cal-grid">';
  ['一', '二', '三', '四', '五', '六', '日'].forEach(d => {
    html += '<div class="cal-dow">' + d + '</div>';
  });
  for (let i = 0; i < startDow; i++) html += '<div></div>';
  for (let d = 1; d <= lastDay; d++) {
    const ds = year + '-' + pad2(month) + '-' + pad2(d);
    const m = marks[ds];
    let cls = 'cal-cell';
    if (m && m.expected > 0) {
      if (m.completed) cls += ' done';
      else if (m.taken > 0) cls += ' partial';
      else cls += ' missed';
    }
    if (ds === today) cls += ' today';
    if (ds === opts.selected) cls += ' selected';
    html += '<div class="' + cls + '" data-date="' + ds + '"><span>' + d + '</span>' +
      (m && m.expected > 0 ? '<span class="dot"></span>' : '') + '</div>';
  }
  html += '</div>';
  container.innerHTML = html;

  container.querySelectorAll('[data-date]').forEach(el => {
    el.onclick = () => opts.onSelect && opts.onSelect(el.dataset.date);
  });
  container.querySelectorAll('[data-nav]').forEach(el => {
    el.onclick = () => opts.onMonth && opts.onMonth(parseInt(el.dataset.nav, 10));
  });
}

function marksFromList(list) {
  const map = {};
  (list || []).forEach(item => { map[item.date] = item; });
  return map;
}
