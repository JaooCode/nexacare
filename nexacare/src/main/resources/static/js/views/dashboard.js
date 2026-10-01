import { api } from '../api.js';
import { esc, fmtData, fmtHora, diaSemana, hojeISO, badgeStatus, estadoVazio, erro } from '../ui.js';
import { abrirFormAgendamento } from '../agendamento-form.js';

function cartao(icone, valor, rotulo, href) {
  const corpo = `<div class="card stat-card h-100"><div class="card-body d-flex align-items-center gap-3">
      <div class="icon"><i class="bi ${icone}"></i></div>
      <div><div class="valor">${valor}</div><div class="rotulo">${esc(rotulo)}</div></div></div></div>`;
  return `<div class="col-6 col-xl-3">${href ? `<a href="${href}" class="text-decoration-none text-reset">${corpo}</a>` : corpo}</div>`;
}

export async function renderizar(raiz, { usuario }) {
  const d = await api.get('/dashboard');
  const pnome = usuario.nome.split(' ')[0];
  const gestao = ['ADMINISTRADOR', 'RECEPCIONISTA'].includes(usuario.perfil);
  const ehPaciente = usuario.perfil === 'PACIENTE';
  const hoje = hojeISO();

  const cards = [];
  cards.push(cartao('bi-calendar-day', d.consultasHoje, 'Consultas de hoje', '#/consultas'));
  cards.push(cartao('bi-calendar-week', d.consultasSemana, 'Próximos 7 dias', '#/consultas'));
  if (!ehPaciente) cards.push(cartao('bi-clock', d.horariosLivresHoje, 'Horários livres hoje', '#/agenda'));
  if (gestao) {
    cards.push(cartao('bi-people', d.pacientes, 'Pacientes ativos', usuario.perfil === 'RECEPCIONISTA' ? '#/pacientes' : null));
    cards.push(cartao('bi-person-badge', d.profissionais, 'Profissionais ativos', '#/profissionais'));
  }
  cards.push(cartao('bi-check2-circle', d.realizadas, 'Consultas realizadas'));
  cards.push(cartao('bi-x-circle', d.canceladas, 'Consultas canceladas'));
  if (usuario.perfil === 'RECEPCIONISTA') cards.push(cartao('bi-arrow-repeat', d.remarcacoesPendentes, 'Remarcações solicitadas', '#/consultas'));
  if (usuario.perfil === 'ADMINISTRADOR') cards.push(cartao('bi-shield-lock', d.usuarios, 'Usuários do sistema', '#/usuarios'));
  if (ehPaciente) cards.push(cartao('bi-bell', d.notificacoesNaoLidas, 'Notificações novas', '#/notificacoes'));

  const acoes = [];
  if (['RECEPCIONISTA', 'PACIENTE'].includes(usuario.perfil)) acoes.push('<button class="btn btn-primary" id="btnNovo"><i class="bi bi-calendar-plus me-1"></i>Novo agendamento</button>');
  if (usuario.perfil === 'RECEPCIONISTA') acoes.push('<a class="btn btn-outline-primary" href="#/pacientes"><i class="bi bi-person-plus me-1"></i>Cadastrar paciente</a>');
  if (!ehPaciente) acoes.push('<a class="btn btn-outline-primary" href="#/agenda"><i class="bi bi-calendar3 me-1"></i>Abrir agenda</a>');

  raiz.innerHTML = `
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
      <div><h2 class="h4 mb-0">Olá, ${esc(pnome)}!</h2>
        <div class="text-secondary text-capitalize">${diaSemana(hoje)}, ${fmtData(hoje)}</div></div>
      <div class="d-flex gap-2 flex-wrap">${acoes.join('')}</div>
    </div>
    <div class="row g-3 mb-4">${cards.join('')}</div>
    <div class="row g-3">
      ${ehPaciente ? '' : `<div class="col-lg-7">
        <div class="card h-100">
          <div class="card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
            <span><i class="bi bi-calendar-day me-2"></i>Agenda de hoje</span>
            ${gestao ? '<select id="selProf" class="form-select form-select-sm w-auto" aria-label="Profissional"></select>' : ''}
          </div>
          <div class="card-body p-0" id="agendaHoje"></div>
        </div>
      </div>`}
      <div class="${ehPaciente ? 'col-12' : 'col-lg-5'}">
        <div class="card h-100">
          <div class="card-header"><i class="bi bi-arrow-right-circle me-2"></i>Próximas consultas</div>
          <div class="list-group list-group-flush">
            ${d.proximas.length ? d.proximas.map((a) => `
              <a href="#/consulta/${a.id}" class="list-group-item list-group-item-action">
                <div class="d-flex justify-content-between align-items-start gap-2">
                  <div><div class="fw-semibold">${esc(usuario.perfil === 'PACIENTE' ? a.profissionalNome : a.pacienteNome)}</div>
                    <div class="small text-secondary">${['PROFISSIONAL', 'PACIENTE'].includes(usuario.perfil) ? '' : esc(a.profissionalNome) + ' · '}${esc(a.especialidade)}</div></div>
                  <div class="text-end"><div class="fw-semibold">${fmtData(a.data)}</div><div class="small">${fmtHora(a.hora)}</div></div>
                </div>
                <div class="mt-1">${badgeStatus(a.status, a.statusRotulo)}${a.remarcacaoSolicitada ? ' <span class="badge text-bg-warning">Remarcação solicitada</span>' : ''}</div>
              </a>`).join('') : estadoVazio('bi-calendar-x', 'Nenhuma consulta futura.')}
          </div>
        </div>
      </div>
    </div>
    ${gestao && d.taxaCancelamento !== null ? `
    <div class="card mt-3"><div class="card-body">
      <div class="d-flex justify-content-between small mb-1"><span>Taxa de cancelamento</span><strong>${d.taxaCancelamento}%</strong></div>
      <div class="progress" role="progressbar" aria-label="Taxa de cancelamento" aria-valuenow="${d.taxaCancelamento}" aria-valuemin="0" aria-valuemax="100">
        <div class="progress-bar bg-danger" style="width:${d.taxaCancelamento}%"></div></div>
    </div></div>` : ''}`;

  raiz.querySelector('#btnNovo')?.addEventListener('click', () => abrirFormAgendamento({ onSalvo: () => renderizar(raiz, { usuario }) }));

  if (ehPaciente) return;
  const alvo = raiz.querySelector('#agendaHoje');

  async function carregarAgendaHoje(profId) {
    try {
      const g = await api.get(`/agenda/grade?data=${hoje}` + (profId ? `&profissionalId=${profId}` : ''));
      const gp = g.profissionais[0];
      if (!g.diaUtil) { alvo.innerHTML = estadoVazio('bi-moon-stars', 'Hoje não há atendimento (fim de semana).'); return; }
      if (!gp) { alvo.innerHTML = estadoVazio('bi-person-x', 'Nenhum profissional ativo.'); return; }
      alvo.innerHTML = `<div class="table-responsive"><table class="table table-sm mb-0"><tbody>${gp.slots.map((s) => {
        let conteudo;
        if (s.estado === 'OCUPADO') conteudo = `<a href="#/consulta/${s.agendamentoId}" class="text-decoration-none">${esc(s.pacienteNome)}</a> ${badgeStatus(s.status, { AGENDADA: 'Agendada', CONFIRMADA: 'Confirmada' }[s.status])}`;
        else if (s.estado === 'LIVRE') conteudo = '<span class="text-success"><i class="bi bi-check-circle me-1"></i>Horário disponível</span>';
        else conteudo = '<span class="text-secondary">Horário encerrado</span>';
        return `<tr><td class="fw-semibold" style="width:70px">${fmtHora(s.hora)}</td><td>${conteudo}</td></tr>`;
      }).join('')}</tbody></table></div>`;
    } catch (e) { erro(e); }
  }

  const sel = raiz.querySelector('#selProf');
  if (sel) {
    const profs = await api.get('/profissionais');
    sel.innerHTML = profs.filter((p) => p.ativo).map((p) => `<option value="${p.id}">${esc(p.nome)} — ${esc(p.especialidade)}</option>`).join('');
    sel.addEventListener('change', () => carregarAgendaHoje(sel.value));
    await carregarAgendaHoje(sel.value);
  } else {
    await carregarAgendaHoje(null); // profissional: a API já devolve somente a própria agenda
  }
}
