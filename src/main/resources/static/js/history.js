const state = { selected: todayStr(), year: 0, month: 0 };

function setSelected(dateStr) {
  state.selected = dateStr;
  const d = new Date(dateStr + 'T00:00:00');
  state.year = d.getFullYear();
  state.month = d.getMonth() + 1;
}
setSelected(state.selected);

async function loadCalendar() {
  const list = await Api.get('/api/calendar?year=' + state.year + '&month=' + state.month);
  renderMonthCalendar(document.getElementById('calendar'), {
    year: state.year,
    month: state.month,
    selected: state.selected,
    marks: marksFromList(list),
    onSelect: ds => { state.selected = ds; refreshAll(); },
    onMonth: changeMonth
  });
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
  document.getElementById('day-title').textContent = state.selected + ' 服药情况';
  try {
    await renderDay(document.getElementById('day-view'), state.selected, {
      showPlan: true,
      onChanged: refreshAll
    });
  } catch (e) {
    document.getElementById('day-view').innerHTML = '<p class="empty">' + esc(e.message) + '</p>';
  }
}

async function loadRecords() {
  const rows = await Api.get('/api/take-records?date=' + state.selected);
  const persons = await Api.get('/api/persons');
  const nameMap = {};
  persons.forEach(p => { nameMap[p.id] = p.name; });
  const tbody = document.querySelector('#records-table tbody');
  if (!rows.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty">当天没有记录</td></tr>';
    return;
  }
  tbody.innerHTML = rows.map(r => {
    const st = r.status === 'TAKEN'
      ? '<span class="tag tag-green">已服用</span>'
      : '<span class="tag tag-grey">已撤销</span>';
    return '<tr><td>' + esc(nameMap[r.personId] || ('#' + r.personId)) + '</td><td>' + esc(periodLabel(r.period)) +
      '</td><td>' + st + '</td><td>' + fmtDateTime(r.takenAt) + '</td><td>方案 #' + r.planId +
      '</td><td>' + esc(r.remark) + '</td></tr>';
  }).join('');
}

async function refreshAll() {
  await Promise.all([refreshDay(), loadRecords()]);
  await loadCalendar();
}

refreshAll();
