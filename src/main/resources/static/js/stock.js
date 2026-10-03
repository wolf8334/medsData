let stocks = [];
let drugMap = {};

const CHANGE_LABEL = { PURCHASE: '入库', OUT: '减少', ADJUST: '调整', USE: '服药扣减', REVERT: '撤销回补' };

async function load() {
  stocks = await Api.get('/api/stocks');
  const drugs = await Api.get('/api/drugs');
  drugMap = {};
  drugs.forEach(d => { drugMap[d.id] = d; });

  const tbody = document.querySelector('#stock-table tbody');
  if (!stocks.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty">还没有药品，请先到「药品」页面添加。</td></tr>';
  } else {
    tbody.innerHTML = stocks.map(s => {
      const qty = formatStock(s.quantity, s.stockUnit, s.packUnit, s.packSize);
      const qtyHtml = s.low ? '<span class="tag tag-red">' + qty + ' 偏低</span>' : qty;
      return '<tr>' +
        '<td>' + esc(s.drugName) + '</td>' +
        '<td>' + esc(s.specification) + '</td>' +
        '<td>' + qtyHtml + '</td>' +
        '<td>' + (s.minStock != null ? formatStock(s.minStock, s.stockUnit, s.packUnit, s.packSize) : '-') + '</td>' +
        '<td>' +
          '<button class="btn-sm" data-act="PURCHASE" data-id="' + s.drugId + '">入库</button> ' +
          '<button class="btn-sm" data-act="OUT" data-id="' + s.drugId + '">减少</button> ' +
          '<button class="btn-sm" data-act="ADJUST" data-id="' + s.drugId + '">调整</button>' +
        '</td></tr>';
    }).join('');
  }

  tbody.querySelectorAll('[data-act]').forEach(btn => {
    btn.onclick = () => openOp(Number(btn.dataset.id), btn.dataset.act);
  });

  const sel = document.getElementById('log-drug');
  const current = sel.value;
  sel.innerHTML = '<option value="">全部药品</option>' +
    drugs.map(d => '<option value="' + d.id + '">' + esc(d.name) + '</option>').join('');
  sel.value = current;
  sel.onchange = loadLogs;

  await loadLogs();
}

async function loadLogs() {
  const drugId = document.getElementById('log-drug').value;
  const logs = await Api.get('/api/stocks/logs' + (drugId ? '?drugId=' + drugId : ''));
  const tbody = document.querySelector('#log-table tbody');
  if (!logs.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="empty">暂无流水</td></tr>';
    return;
  }
  tbody.innerHTML = logs.map(l => {
    const d = drugMap[l.drugId] || {};
    const sign = Number(l.changeQty) > 0 ? '+' : '';
    return '<tr>' +
      '<td>' + fmtDateTime(l.createdAt) + '</td>' +
      '<td>' + esc(d.name || ('#' + l.drugId)) + '</td>' +
      '<td>' + (CHANGE_LABEL[l.changeType] || l.changeType) + '</td>' +
      '<td>' + sign + formatStock(l.changeQty, d.stockUnit, d.packUnit, d.packSize) + '</td>' +
      '<td>' + formatStock(l.beforeQty, d.stockUnit, d.packUnit, d.packSize) + '</td>' +
      '<td>' + formatStock(l.afterQty, d.stockUnit, d.packUnit, d.packSize) + '</td>' +
      '<td>' + esc(l.remark) + '</td>' +
      '</tr>';
  }).join('');
}

function openOp(drugId, type) {
  const stock = stocks.find(s => s.drugId === drugId) || {};
  const isAdjust = type === 'ADJUST';
  const label = CHANGE_LABEL[type] || type;
  const base = stock.stockUnit || '';
  const hasPackInfo = hasPack(stock.packUnit, stock.packSize);

  let qtyFields;
  if (hasPackInfo) {
    const b = isAdjust ? splitStock(stock.quantity, stock.packSize) : { packs: 0, loose: 0 };
    qtyFields =
      '<div class="grid2">' +
        '<div class="field"><label>' + (isAdjust ? '调整后整包装数' : label + '整包装数') + '</label>' +
          '<input id="op-packs" type="number" step="1" min="0" value="' + b.packs + '"></div>' +
        '<div class="field"><label>' + (isAdjust ? '调整后零散' + base + '数' : label + '零散' + base + '数') + '</label>' +
          '<input id="op-loose" type="number" step="0.001" min="0" value="' + b.loose + '"></div>' +
      '</div>' +
      '<p class="muted">1 ' + esc(stock.packUnit) + ' = ' + money(stock.packSize) + ' ' + esc(base) + '</p>';
  } else {
    qtyFields =
      '<div class="field"><label>' + (isAdjust ? '调整后库存' : label + '数量') + '（' + esc(base) + '）*</label>' +
        '<input id="op-qty" type="number" step="0.001" min="0" value="' + (isAdjust ? money(stock.quantity) : '') + '"></div>';
  }

  const dlg = openModal(label + ' - ' + (stock.drugName || ''),
    '<p class="muted">当前库存：' + formatStock(stock.quantity, base, stock.packUnit, stock.packSize) + '</p>' +
    qtyFields +
    '<div class="field"><label>备注</label><input id="op-remark" placeholder="如 购药、过期丢弃"></div>' +
    '<div class="row"><button type="button" id="op-cancel">取消</button>' +
    '<button type="button" class="btn-primary" id="op-save">保存</button></div>');

  dlg.querySelector('#op-cancel').onclick = closeModal;
  dlg.querySelector('#op-save').onclick = async () => {
    let qty;
    if (hasPackInfo) {
      const packs = Number(dlg.querySelector('#op-packs').value || 0);
      const loose = Number(dlg.querySelector('#op-loose').value || 0);
      if (packs < 0 || loose < 0) { toast('数量不能为负', 'error'); return; }
      qty = packs * Number(stock.packSize) + loose;
    } else {
      const v = dlg.querySelector('#op-qty').value;
      if (v === '' || Number(v) < 0) { toast('请输入正确的数量', 'error'); return; }
      qty = Number(v);
    }
    try {
      await Api.post('/api/stocks/change', {
        drugId, quantity: qty, changeType: type,
        remark: dlg.querySelector('#op-remark').value.trim()
      });
      toast('操作成功', 'success');
      closeModal();
      load();
    } catch (e) { toast(e.message, 'error'); }
  };
}

load();
