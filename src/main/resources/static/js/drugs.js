let drugList = [];

async function load() {
  const [drugs, stocks] = await Promise.all([
    Api.get('/api/drugs'),
    Api.get('/api/stocks')
  ]);
  drugList = drugs;
  const stockMap = {};
  stocks.forEach(s => { stockMap[s.drugId] = s; });

  const tbody = document.querySelector('#drug-table tbody');
  if (!drugs.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="empty">还没有药品，点击「新增药品」添加。</td></tr>';
    return;
  }
  tbody.innerHTML = drugs.map(d => {
    const s = stockMap[d.id] || { quantity: 0, packUnit: d.packUnit, packSize: d.packSize };
    const qty = formatStock(s.quantity, d.stockUnit, d.packUnit, d.packSize);
    const low = d.minStock != null && Number(s.quantity) < Number(d.minStock);
    const qtyHtml = low ? '<span class="tag tag-red">' + qty + '</span>' : qty;
    const status = d.status === 'ENABLED'
      ? '<button class="btn-sm" data-act="disable" data-id="' + d.id + '">停用</button>'
      : '<button class="btn-sm" data-act="enable" data-id="' + d.id + '">启用</button>';
    return '<tr>' +
      '<td>' + esc(d.name) + '</td>' +
      '<td>' + esc(d.specification) + '</td>' +
      '<td>' + esc(d.stockUnit) + '</td>' +
      '<td>' + qtyHtml + '</td>' +
      '<td>' + (d.minStock != null ? formatStock(d.minStock, d.stockUnit, d.packUnit, d.packSize) : '-') + '</td>' +
      '<td>' + statusTag(d.status) + '</td>' +
      '<td><button class="btn-sm" data-act="edit" data-id="' + d.id + '">编辑</button> ' + status + '</td>' +
      '</tr>';
  }).join('');

  tbody.querySelectorAll('[data-act]').forEach(btn => {
    btn.onclick = async () => {
      const drug = drugList.find(x => x.id === Number(btn.dataset.id));
      if (btn.dataset.act === 'edit') {
        openForm(drug);
      } else {
        try {
          await Api.put('/api/drugs/' + drug.id + '/status?status=' +
            (btn.dataset.act === 'enable' ? 'ENABLED' : 'DISABLED'));
          toast('已更新', 'success');
          load();
        } catch (e) { toast(e.message, 'error'); }
      }
    };
  });
}

function openForm(drug) {
  drug = drug || {};
  const dlg = openModal(drug.id ? '编辑药品' : '新增药品',
    '<div class="grid2">' +
      '<div class="field"><label>药品名称 *</label><input id="f-name" value="' + esc(drug.name || '') + '"></div>' +
      '<div class="field"><label>规格</label><input id="f-spec" placeholder="如 50mg" value="' + esc(drug.specification || '') + '"></div>' +
    '</div>' +
    '<div class="grid3">' +
      '<div class="field"><label>最小单位 *</label><input id="f-unit" list="unit-list" value="' + esc(drug.stockUnit || '粒') + '"></div>' +
      '<div class="field"><label>包装单位</label><input id="f-pack-unit" list="unit-list" placeholder="如 板" value="' + esc(drug.packUnit || '') + '"></div>' +
      '<div class="field"><label>每包装数量</label><input id="f-pack-size" type="number" step="0.001" min="0" placeholder="如 10" value="' + (drug.packSize != null ? drug.packSize : '') + '"></div>' +
    '</div>' +
    '<div class="grid2">' +
      '<div class="field"><label>最低库存（按最小单位）</label><input id="f-min" type="number" step="0.001" min="0" value="' + (drug.minStock != null ? drug.minStock : '') + '"></div>' +
      '<div class="field"><label>状态</label><select id="f-status">' +
        '<option value="ENABLED"' + (drug.status === 'DISABLED' ? '' : ' selected') + '>启用</option>' +
        '<option value="DISABLED"' + (drug.status === 'DISABLED' ? ' selected' : '') + '>停用</option>' +
      '</select></div>' +
    '</div>' +
    '<div class="field"><label>备注</label><input id="f-remark" value="' + esc(drug.remark || '') + '"></div>' +
    '<p class="muted">如需按「板 + 粒」计数，请填写包装单位和每包装数量（如 每板 10 粒）。</p>' +
    '<div class="row"><button type="button" id="f-cancel">取消</button>' +
    '<button type="button" class="btn-primary" id="f-save">保存</button></div>');

  dlg.querySelector('#f-cancel').onclick = closeModal;
  dlg.querySelector('#f-save').onclick = async () => {
    const packSizeVal = dlg.querySelector('#f-pack-size').value;
    const body = {
      name: dlg.querySelector('#f-name').value.trim(),
      specification: dlg.querySelector('#f-spec').value.trim(),
      stockUnit: dlg.querySelector('#f-unit').value.trim(),
      packUnit: dlg.querySelector('#f-pack-unit').value.trim() || null,
      packSize: packSizeVal ? Number(packSizeVal) : null,
      minStock: dlg.querySelector('#f-min').value ? Number(dlg.querySelector('#f-min').value) : null,
      status: dlg.querySelector('#f-status').value,
      remark: dlg.querySelector('#f-remark').value.trim()
    };
    if (!body.name) { toast('请填写药品名称', 'error'); return; }
    if (!body.stockUnit) { toast('请填写最小单位', 'error'); return; }
    if (body.packSize != null && body.packSize <= 0) { toast('每包装数量必须大于 0', 'error'); return; }
    if (body.packSize != null && !body.packUnit) { toast('填写每包装数量时需同时填写包装单位', 'error'); return; }
    try {
      if (drug.id) await Api.put('/api/drugs/' + drug.id, body);
      else await Api.post('/api/drugs', body);
      toast('保存成功', 'success');
      closeModal();
      load();
    } catch (e) { toast(e.message, 'error'); }
  };
}

document.getElementById('add-btn').onclick = () => openForm(null);
load();
