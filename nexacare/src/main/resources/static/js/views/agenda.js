import { api } from '../api.js';
import { esc, fmtData, fmtHora, diaSemana, hojeISO, somarDias, badgeStatus, estadoVazio, erro } from '../ui.js';
import { abrirFormAgendamento } from '../agendamento-form.js';

export async function renderizar(raiz, { usuario, query }) {
  const podeAgendar = usuario.perfil === 'RECEPCIONISTA';
  const ehProf = usuario.perfil === 'PROFISSIONAL';
  let data = query.get('data') || hojeISO();
  let profId = query.get('prof') || '';

  const profs = ehProf ? [] : (await api.get('/profissionais')).filter((p) => p.ativo);

  raiz.innerHTML = `
    <div class="card mb-3"><div class="card-body">
      <div class="row g-2 align-items-end">
        <div class="col-12 col-md-auto">
          <label class="form-label mb-1 small" for="agData">Data</label>
          <div class="input-group">
            <button class="btn btn-outline-secondary" id="ontem" aria-label="Dia anterior"><i class="bi bi-chevron-left"></i></button>
            <input type="date" id="agData" class="form-control" value="${data}">
            <button class="btn btn-outline-secondary" id="amanha" aria-label="Próximo dia"><i class="bi bi-chevron-right"></i></button>
            <button class="btn btn-outline-primary" id="hoje">Hoje</button>
          </div>
        </div>
        ${ehProf ? '' : `<div class="col-12 col-md-4">
          <label class="form-label mb-1 small" for="agProf">Profissional</label>
          <select id="agProf" class="form-select"><option value="">Todos os profissionais</option>
            ${profs.map((p) => `<option value="${p.id}" ${String(p.id) === profId ? 'selected' : ''}>${esc(p.nome)} — ${esc(p.especialidade)}</option>`).join('')}
          </select></div>`}
        <div class="col-12 col-md ms-md-auto text-md-end">
          ${podeAgendar ? '<button class="btn btn-primary" id="novo"><i class="bi bi-calendar-plus me-1"></i>Novo agendamento</button>' : ''}
        </div>
      </div>
    </div></div>
    <div class="legenda mb-2">
      <span><i style="background:#ecfdf5;border:1px dashed #6ee7b7"></i>Livre</span>
      <span><i style="background:#eff6ff;border:1px solid #bfdbfe"></i>Ocupado (agendada)</span>
      <span><i style="background:#ecfdf5;border:1px solid #a7f3d0"></i>Ocupado (confirmada)</span>
      <span><i style="background:#f3f4f6;border:1px solid #e5e7eb"></i>Encerrado</span>
    </div>
    <div class="card"><div class="card-header" id="tituloDia"></div><div id="grade" class="card-body p-0"></div></div>`;

  const $ = (s) => raiz.querySelector(s);

  async function carregar() {
    $('#tituloDia').innerHTML = `<i class="bi bi-calendar3 me-2"></i><span class="text-capitalize">${diaSemana(data)}</span>, ${fmtData(data)}`;
    history.replaceState(null, '', `#/agenda?data=${data}${profId ? '&prof=' + profId : ''}`);
    try {
      const g = await api.get(`/agenda/grade?data=${data}` + (profId ? `&profissionalId=${profId}` : ''));
      const alvo = $('#grade');
      if (!g.diaUtil) { alvo.innerHTML = estadoVazio('bi-moon-stars', 'Sem atendimento aos sábados e domingos.'); return; }
      if (!g.profissionais.length) { alvo.innerHTML = estadoVazio('bi-person-x', 'Nenhum profissional ativo para exibir.'); return; }

      const horas = g.profissionais[0].slots.map((s) => s.hora);
      const celula = (gp, i) => {
        const s = gp.slots[i];
        if (s.estado === 'LIVRE') {
          return podeAgendar
            ? `<button class="slot slot-livre" data-prof="${gp.profissionalId}" data-hora="${fmtHora(s.hora)}"><span><i class="bi bi-plus-circle me-1"></i>Livre</span></button>`
            : '<div class="slot slot-livre"><span>Livre</span></div>';
        }
        if (s.estado === 'INDISPONIVEL') return '<div class="slot slot-passado">—</div>';
        if (!s.agendamentoId) return '<div class="slot slot-ocupado sem-detalhe"><span><i class="bi bi-lock me-1"></i>Ocupado</span></div>';
        return `<a class="slot slot-ocupado st-${esc(s.status)}" href="#/consulta/${s.agendamentoId}" title="Ver detalhes">
          <span class="text-truncate">${esc(s.pacienteNome)}</span>${badgeStatus(s.status, s.status === 'CONFIRMADA' ? 'Confirmada' : 'Agendada')}</a>`;
      };
      alvo.innerHTML = `<div class="table-responsive"><table class="table table-bordered grade mb-0">
        <thead><tr><th>Horário</th>${g.profissionais.map((p) => `<th class="prof">${esc(p.nome)}<div class="fw-normal text-secondary text-capitalize small">${esc(p.especialidade)}</div></th>`).join('')}</tr></thead>
        <tbody>${horas.map((h, i) => `<tr><td class="hora">${fmtHora(h)}</td>${g.profissionais.map((gp) => `<td>${celula(gp, i)}</td>`).join('')}</tr>`).join('')}</tbody>
      </table></div>`;

      alvo.querySelectorAll('button.slot-livre').forEach((b) => b.addEventListener('click', () =>
        abrirFormAgendamento({ profissionalId: Number(b.dataset.prof), data, hora: b.dataset.hora, onSalvo: carregar })));
    } catch (e) { erro(e); }
  }

  const mudarData = (nova) => { data = nova; $('#agData').value = nova; carregar(); };
  $('#agData').addEventListener('change', (e) => e.target.value && mudarData(e.target.value));
  $('#ontem').addEventListener('click', () => mudarData(somarDias(data, -1)));
  $('#amanha').addEventListener('click', () => mudarData(somarDias(data, 1)));
  $('#hoje').addEventListener('click', () => mudarData(hojeISO()));
  $('#agProf')?.addEventListener('change', (e) => { profId = e.target.value; carregar(); });
  $('#novo')?.addEventListener('click', () => abrirFormAgendamento({ profissionalId: profId ? Number(profId) : undefined, data, onSalvo: carregar }));
  await carregar();
}
