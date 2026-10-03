const state = { selected: todayStr(), year: 0, month: 0 };

function setSelected(dateStr) {
  state.selected = dateStr;
  const d = new Date(dateStr + 'T00:00:00');
  state.year = d.getFullYear();
  state.month = d.getMonth() + 1;
}
setSelected(state.selected);

async function loadCalendar() {
  try {
    const list = await Api.get('/api/calendar?year=' + state.year + '&month=' + state.month);
    renderMonthCalendar(document.getElementById('calendar'), {
      year: state.year,
      month: state.month,
      selected: state.selected,
      marks: marksFromList(list),
      onSelect: ds => { state.selected = ds; refreshDay(); loadCalendar(); },
      onMonth: changeMonth
    });
  } catch (e) {
    toast(e.message, 'error');
  }
}

function changeMonth(delta) {
  let m = state.month + delta, y = state.year;
  if (m < 1) { m = 12; y--; }
  if (m > 12) { m = 1; y++; }
  state.year = y;
  state.month = m;
  loadCalendar();
}

async function refreshDay() {
  document.getElementById('day-title').textContent =
    state.selected === todayStr() ? '今日用药' : state.selected + ' 用药';
  document.getElementById('day-sub').textContent =
    state.selected === todayStr() ? '' : '(可补录 / 撤销 / 改时间)';
  try {
    await renderDay(document.getElementById('day-view'), state.selected, {});
  } catch (e) {
    document.getElementById('day-view').innerHTML = '<p class="empty">' + esc(e.message) + '</p>';
  }
}

async function loadLowStock() {
  try {
    const list = await Api.get('/api/stocks/low-stock');
    const box = document.getElementById('low-stock');
    if (!list.length) { box.innerHTML = ''; return; }
    box.innerHTML = '<div class="card" style="border-color:#f0d9a8;background:#fdf6e6">' +
      '<strong>库存提醒：</strong> ' +
      list.map(d => esc(d.drugName) + '（剩 ' + formatStock(d.quantity, d.stockUnit, d.packUnit, d.packSize) +
        ' / 最低 ' + formatStock(d.minStock, d.stockUnit, d.packUnit, d.packSize) + '）').join('，') +
      ' <a href="/stock.html">去处理</a></div>';
  } catch (e) { /* 忽略 */ }
}

loadCalendar();
refreshDay();
loadLowStock();
