/* 服药日视图：首页与历史页共用 */

async function renderDay(container, date, opts) {
  opts = opts || {};
  const day = await Api.get('/api/home?date=' + date);
  if (!day.persons.length) {
    container.innerHTML = '<p class="empty">没有启用的用药人，请先到「用药人」页面添加。</p>';
    return day;
  }
  const refresh = () => renderDay(container, date, opts);

  let html = '';
  day.persons.forEach(p => {
    html += '<div class="person-block"><div class="person-name">' + esc(p.personName);
    if (opts.showPlan) {
      html += p.planId
        ? ' <span class="tag">方案 v' + p.versionNo + '</span> ' + planStatusTag(p.planStatus)
        : ' <span class="tag tag-orange">无方案</span>';
    }
    html += '</div>';

    p.periods.forEach(period => {
      html += '<div class="period-card ' + (period.taken ? 'taken' : '') + '">';
      html += '<div class="period-head"><strong>' + esc(p.personName) + ' · ' + esc(period.label) + '</strong>';
      if (period.taken) {
        html += '<span class="muted"><span class="tag tag-green">已服用 ' + fmtTime(period.takenAt) + '</span> ' +
          '<button class="btn-sm" data-act="edit" data-id="' + period.recordId + '" data-time="' + esc(period.takenAt) + '">改时间</button> ' +
          '<button class="btn-sm btn-danger" data-act="cancel" data-id="' + period.recordId + '">撤销</button></span>';
      } else if (period.items.length) {
        html += '<button class="btn-sm btn-primary" data-act="take" data-person="' + p.personId +
          '" data-period="' + period.period + '">记录服药</button>';
      } else {
        html += '<span class="muted">无用药</span>';
      }
      html += '</div>';

      if (period.items.length) {
        html += '<ul class="items">';
        period.items.forEach(it => {
          html += '<li>' + esc(it.drugName) +
            (it.specification ? ' <span class="muted">' + esc(it.specification) + '</span>' : '') +
            ' · <strong>' + money(it.dose) + ' ' + esc(it.doseUnit) + '</strong>' +
            (it.remark ? ' <span class="muted">(' + esc(it.remark) + ')</span>' : '') + '</li>';
        });
        html += '</ul>';
      } else if (period.taken) {
        html += '<p class="empty">当时方案无该时段明细</p>';
      }
      html += '</div>';
    });
    html += '</div>';
  });
  container.innerHTML = html;

  container.querySelectorAll('[data-act]').forEach(btn => {
    btn.onclick = async () => {
      const act = btn.dataset.act;
      try {
        if (act === 'take') {
          await Api.post('/api/take-records', {
            personId: Number(btn.dataset.person),
            takeDate: date,
            period: btn.dataset.period,
            takenAt: null
          });
          toast('已记录服药', 'success');
        } else if (act === 'cancel') {
          if (!confirm('确定撤销这条服药记录吗？')) return;
          await Api.post('/api/take-records/' + btn.dataset.id + '/cancel');
          toast('已撤销', 'success');
        } else if (act === 'edit') {
          editTakeTime(btn.dataset.id, btn.dataset.time, opts.onChanged || refresh);
          return;
        }
        if (opts.onChanged) opts.onChanged();
        else refresh();
      } catch (e) {
        toast(e.message, 'error');
      }
    };
  });
  return day;
}

function editTakeTime(id, value, onDone) {
  const local = String(value || '').slice(0, 16);
  const dlg = openModal('修改实际服药时间',
    '<div class="field"><label>实际服药时间</label>' +
    '<input type="datetime-local" id="taken-at" value="' + esc(local) + '"></div>' +
    '<div class="row"><button type="button" id="cancel-btn">取消</button>' +
    '<button type="button" class="btn-primary" id="save-btn">保存</button></div>');
  dlg.querySelector('#cancel-btn').onclick = closeModal;
  dlg.querySelector('#save-btn').onclick = async () => {
    const value = dlg.querySelector('#taken-at').value;
    try {
      await Api.put('/api/take-records/' + id, { takenAt: value || null, remark: null });
      toast('已修改', 'success');
      closeModal();
      if (onDone) onDone();
    } catch (e) {
      toast(e.message, 'error');
    }
  };
}
