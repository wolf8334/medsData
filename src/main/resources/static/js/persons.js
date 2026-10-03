let persons = [];

async function load() {
  persons = await Api.get('/api/persons');
  const tbody = document.querySelector('#person-table tbody');
  if (!persons.length) {
    tbody.innerHTML = '<tr><td colspan="4" class="empty">还没有用药人，点击「新增用药人」添加。</td></tr>';
    return;
  }
  tbody.innerHTML = persons.map(p => {
    const toggle = p.status === 'ENABLED'
      ? '<button class="btn-sm" data-act="disable" data-id="' + p.id + '">停用</button>'
      : '<button class="btn-sm" data-act="enable" data-id="' + p.id + '">启用</button>';
    return '<tr>' +
      '<td>' + esc(p.name) + '</td>' +
      '<td>' + statusTag(p.status) + '</td>' +
      '<td>' + esc(p.remark) + '</td>' +
      '<td><button class="btn-sm" data-act="edit" data-id="' + p.id + '">编辑</button> ' + toggle + '</td>' +
      '</tr>';
  }).join('');

  tbody.querySelectorAll('[data-act]').forEach(btn => {
    btn.onclick = async () => {
      const person = persons.find(x => x.id === Number(btn.dataset.id));
      if (btn.dataset.act === 'edit') {
        openForm(person);
      } else {
        try {
          await Api.put('/api/persons/' + person.id + '/status?status=' +
            (btn.dataset.act === 'enable' ? 'ENABLED' : 'DISABLED'));
          toast('已更新', 'success');
          load();
        } catch (e) { toast(e.message, 'error'); }
      }
    };
  });
}

function openForm(person) {
  person = person || {};
  const dlg = openModal(person.id ? '编辑用药人' : '新增用药人',
    '<div class="field"><label>姓名 *</label><input id="p-name" value="' + esc(person.name || '') + '"></div>' +
    '<div class="field"><label>状态</label><select id="p-status">' +
      '<option value="ENABLED"' + (person.status === 'DISABLED' ? '' : ' selected') + '>启用</option>' +
      '<option value="DISABLED"' + (person.status === 'DISABLED' ? ' selected' : '') + '>停用</option>' +
    '</select></div>' +
    '<div class="field"><label>备注</label><input id="p-remark" value="' + esc(person.remark || '') + '"></div>' +
    '<div class="row"><button type="button" id="p-cancel">取消</button>' +
    '<button type="button" class="btn-primary" id="p-save">保存</button></div>');

  dlg.querySelector('#p-cancel').onclick = closeModal;
  dlg.querySelector('#p-save').onclick = async () => {
    const body = {
      name: dlg.querySelector('#p-name').value.trim(),
      status: dlg.querySelector('#p-status').value,
      remark: dlg.querySelector('#p-remark').value.trim()
    };
    if (!body.name) { toast('请填写姓名', 'error'); return; }
    try {
      if (person.id) await Api.put('/api/persons/' + person.id, body);
      else await Api.post('/api/persons', body);
      toast('保存成功', 'success');
      closeModal();
      load();
    } catch (e) { toast(e.message, 'error'); }
  };
}

document.getElementById('add-btn').onclick = () => openForm(null);
load();
