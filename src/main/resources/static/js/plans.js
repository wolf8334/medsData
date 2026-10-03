let personId = null;
let drugs = [];
let cache = {};

async function init() {
  const persons = await Api.get('/api/persons');
  const sel = document.getElementById('person-select');
  const enabled = persons.filter(p => p.status === 'ENABLED');
  const list = enabled.length ? enabled : persons;
  sel.innerHTML = list.map(p => '<option value="' + p.id + '">' + esc(p.name) + '</option>').join('');
  if (!list.length) {
    document.getElementById('current-plan').innerHTML =
      '<div class="card empty">还没有用药人，请先到「用药人」页面添加。</div>';
    return;
  }
  personId = Number(sel.value);
  sel.onchange = () => { personId = Number(sel.value); loadPlans(); };
  document.getElementById('new-plan-btn').onclick = () => openPlanForm({
    personId, action: 'create', title: '新建方案',
    defaultFrom: todayStr(), items: [{ doseUnit: '片' }]
  });
  drugs = (await Api.get('/api/drugs?status=ENABLED')).map(d => ({
    id: d.id, name: d.name, specification: d.specification, stockUnit: d.stockUnit
  }));
  loadPlans();
}

function itemsSummary(items) {
  if (!items || !items.length) return '<span class="empty">无明细</span>';
  const groups = {};
  items.forEach(it => {
    (groups[it.period] = groups[it.period] || []).push(
      it.drugName + ' ' + money(it.dose) + it.doseUnit);
  });
  const parts = [];
  if (groups.EARLY && groups.EARLY.length) parts.push('早：' + groups.EARLY.join('；'));
  if (groups.EVENING && groups.EVENING.length) parts.push('晚：' + groups.EVENING.join('；'));
  return parts.join('　');
}

async function loadPlans() {
  const plans = await Api.get('/api/plans?personId=' + personId);
  cache = {};
  plans.forEach(p => { cache[p.plan.id] = p; });

  const current = plans.find(p => p.plan.status === 'CURRENT');
  renderCurrent(current);

  const others = plans.filter(p => !current || p.plan.id !== current.plan.id);
  const tbody = document.querySelector('#plan-table tbody');
  if (!others.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty">暂无历史方案</td></tr>';
    return;
  }
  tbody.innerHTML = others.map(p => {
    const edit = p.plan.status === 'PENDING'
      ? '<button class="btn-sm" data-act="edit" data-id="' + p.plan.id + '">编辑</button> ' : '';
    return '<tr>' +
      '<td>' + planStatusTag(p.plan.status) + '</td>' +
      '<td>' + (p.plan.effectiveFrom || '') + '</td>' +
      '<td>' + (p.plan.effectiveTo || '至今') + '</td>' +
      '<td>' + itemsSummary(p.items) + '</td>' +
      '<td>' + edit +
      '<button class="btn-sm btn-danger" data-act="delete" data-id="' + p.plan.id + '">删除</button>' +
      '</td></tr>';
  }).join('');

  tbody.querySelectorAll('[data-act]').forEach(btn => {
    btn.onclick = () => {
      const plan = cache[btn.dataset.id];
      if (btn.dataset.act === 'edit') {
        openPlanForm({
          personId, action: 'update', planId: plan.plan.id, title: '编辑方案（待生效）',
          defaultFrom: plan.plan.effectiveFrom, remark: plan.plan.remark, items: copyItems(plan.items)
        });
      } else {
        doDelete(plan.plan.id);
      }
    };
  });
}

function copyItems(items) {
  return (items || []).map(it => ({
    drugId: it.drugId, period: it.period, dose: it.dose,
    doseUnit: it.doseUnit, sortNo: it.sortNo, remark: it.remark
  }));
}

function renderCurrent(current) {
  const box = document.getElementById('current-plan');
  if (!current) {
    box.innerHTML = '<div class="card"><h3>当前方案</h3>' +
      '<p class="empty">当前没有生效中的方案，点击右上角「新建方案」。</p></div>';
    return;
  }
  const plan = current.plan;
  let html = '<div class="card"><div class="row"><h3>当前方案 ' + planStatusTag(plan.status) + '</h3>' +
    '<span><button class="btn-primary btn-sm" data-act="edit">编辑</button> ' +
    '<button class="btn-sm btn-danger" data-act="delete">删除</button></span></div>' +
    '<p class="muted">生效日期：' + plan.effectiveFrom + (plan.remark ? '　备注：' + esc(plan.remark) : '') + '</p>';
  html += renderItems(current.items);
  html += '</div>';
  box.innerHTML = html;
  box.querySelector('[data-act="edit"]').onclick = () => openPlanForm({
    personId, action: 'create', title: '编辑方案',
    defaultFrom: todayStr(), remark: plan.remark, items: copyItems(current.items)
  });
  box.querySelector('[data-act="delete"]').onclick = () => doDelete(plan.id);
}

async function doDelete(id) {
  if (!confirm('确定删除这个方案吗？删除后不可恢复。')) return;
  try {
    await Api.del('/api/plans/' + id);
    toast('已删除', 'success');
    loadPlans();
  } catch (e) {
    toast(e.message, 'error');
  }
}

function renderItems(items) {
  if (!items || !items.length) return '<p class="empty">无用药明细</p>';
  const groups = {};
  items.forEach(it => { (groups[it.period] = groups[it.period] || []).push(it); });
  let html = '';
  ['EARLY', 'EVENING'].forEach(period => {
    if (!groups[period]) return;
    html += '<div class="person-block"><div class="person-name">' + periodLabel(period) + '</div><ul class="items">';
    groups[period].forEach(it => {
      html += '<li>' + esc(it.drugName) +
        (it.specification ? ' <span class="muted">' + esc(it.specification) + '</span>' : '') +
        ' · <strong>' + money(it.dose) + ' ' + esc(it.doseUnit) + '</strong>' +
        (it.remark ? ' <span class="muted">(' + esc(it.remark) + ')</span>' : '') + '</li>';
    });
    html += '</ul></div>';
  });
  return html;
}

function drugOptions(selectedId) {
  let html = '<option value="">选择药品</option>';
  drugs.forEach(d => {
    html += '<option value="' + d.id + '"' + (d.id === selectedId ? ' selected' : '') + '>' +
      esc(d.name) + (d.specification ? ' ' + esc(d.specification) : '') + '</option>';
  });
  return html;
}

function itemRowHtml(item) {
  item = item || {};
  return '<div class="item-row">' +
    '<select class="pf-drug">' + drugOptions(item.drugId) + '</select>' +
    '<select class="pf-period">' +
      '<option value="EARLY"' + (item.period === 'EVENING' ? '' : ' selected') + '>早</option>' +
      '<option value="EVENING"' + (item.period === 'EVENING' ? ' selected' : '') + '>晚</option>' +
    '</select>' +
    '<input class="pf-dose" type="number" step="0.001" min="0" placeholder="剂量" value="' + (item.dose != null ? item.dose : '') + '">' +
    '<input class="pf-unit" placeholder="单位" value="' + esc(item.doseUnit || '片') + '">' +
    '<input class="pf-sort" type="number" placeholder="排序" value="' + (item.sortNo != null ? item.sortNo : 0) + '">' +
    '<button type="button" class="btn-sm btn-danger item-del">删</button>' +
    '</div>';
}

function openPlanForm(opts) {
  if (!drugs.length) {
    toast('请先在「药品」页面添加药品', 'error');
    return;
  }
  const rows = (opts.items && opts.items.length ? opts.items : [{ doseUnit: '片' }]);
  const dlg = openModal(opts.title || '新建方案',
    '<div class="grid2">' +
      '<div class="field"><label>生效日期</label>' +
        '<input type="date" id="pf-from" value="' + esc(opts.defaultFrom || todayStr()) + '"></div>' +
      '<div class="field"><label>备注</label>' +
        '<input id="pf-remark" value="' + esc(opts.remark || '') + '"></div>' +
    '</div>' +
    '<div class="field"><label>用药明细（药品 / 时段 / 剂量 / 单位 / 排序）</label>' +
      '<div class="items-editor" id="pf-items">' + rows.map(itemRowHtml).join('') + '</div>' +
      '<div style="margin-top:8px"><button type="button" class="btn-sm" id="pf-add">+ 添加明细</button></div>' +
    '</div>' +
    '<p class="muted">保存后从生效日期起使用这份方案，原方案停用但仍保留在历史中。</p>' +
    '<div class="row"><button type="button" id="pf-cancel">取消</button>' +
      '<button type="button" class="btn-primary" id="pf-save">保存</button></div>');

  const itemsBox = dlg.querySelector('#pf-items');
  dlg.querySelector('#pf-add').onclick = () => {
    itemsBox.insertAdjacentHTML('beforeend', itemRowHtml({ doseUnit: '片' }));
    bindDelete(itemsBox);
    bindDrugUnits(itemsBox);
  };
  bindDelete(itemsBox);
  bindDrugUnits(itemsBox);
  dlg.querySelector('#pf-cancel').onclick = closeModal;
  dlg.querySelector('#pf-save').onclick = () => savePlan(dlg, opts);
}

function bindDelete(box) {
  box.querySelectorAll('.item-del').forEach(btn => {
    btn.onclick = () => btn.closest('.item-row').remove();
  });
}

function bindDrugUnits(box) {
  box.querySelectorAll('.pf-drug').forEach(sel => {
    sel.onchange = () => {
      const drug = drugs.find(d => d.id === Number(sel.value));
      if (drug && drug.stockUnit) {
        const unitInput = sel.closest('.item-row').querySelector('.pf-unit');
        if (unitInput) unitInput.value = drug.stockUnit;
      }
    };
  });
}

async function savePlan(dlg, opts) {
  const effectiveFrom = dlg.querySelector('#pf-from').value;
  const remark = dlg.querySelector('#pf-remark').value.trim();
  if (!effectiveFrom) { toast('请选择生效日期', 'error'); return; }

  const items = [];
  let bad = false;
  dlg.querySelectorAll('#pf-items .item-row').forEach(row => {
    const drugId = row.querySelector('.pf-drug').value;
    const period = row.querySelector('.pf-period').value;
    const dose = row.querySelector('.pf-dose').value;
    const doseUnit = row.querySelector('.pf-unit').value.trim();
    const sortNo = row.querySelector('.pf-sort').value;
    if (!drugId) { bad = true; return; }
    if (!dose || Number(dose) <= 0) { bad = true; return; }
    if (!doseUnit) { bad = true; return; }
    items.push({
      drugId: Number(drugId), period, dose: Number(dose),
      doseUnit, sortNo: sortNo ? Number(sortNo) : 0, remark: null
    });
  });
  if (bad) { toast('请填写完整的明细（药品、剂量、单位）', 'error'); return; }
  if (!items.length) { toast('至少添加一条明细', 'error'); return; }

  try {
    if (opts.action === 'update') {
      await Api.put('/api/plans/' + opts.planId,
        { personId: opts.personId, effectiveFrom, remark, items });
    } else {
      await Api.post('/api/plans', { personId: opts.personId, effectiveFrom, remark, items });
    }
    toast('保存成功', 'success');
    closeModal();
    loadPlans();
  } catch (e) {
    toast(e.message, 'error');
  }
}

init();
