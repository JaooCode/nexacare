import { api, query as qs } from '../api.js';
import { esc, fmtData, fmtHora, hojeISO, somarDias, badgeStatus, estadoVazio, paginar, htmlPaginacao, debounce, confirmar, toast, erro } from '../ui.js';
import { abrirFormAgendamento } from '../agendamento-form.js';

const PERIODOS = {
  hoje: () => [hojeISO(), hojeISO()],
  semana: () => [hojeISO(), somarDias(hojeISO(), 7)],
  mes: () => [hojeISO(), somarDias(hojeISO(), 30)],
  passadas: () => ['', somarDias(hojeISO(), -1)],
  todas: () => ['', ''],
};

export async function renderizar(raiz, { usuario, atualizarBadges }) {
  const ehPaciente = usuario.perfil === 'PACIENTE';
  const ehProf = usuario.perfil === 'PROFISSIONAL';
  const podeAgendar = ['RECEPCIONISTA', 'PACIENTE'].includes(usuario.perfil);
  const profs = ehProf || ehPaciente ? [] : (await api.get('/profissionais')).filter((p) => p.ativo);
  let pagina = 1, lista = [];

  raiz.innerHTML = `
    <div class="card mb-3"><div class="card-body"><div class="row g-2 align-items-end">
      <div class="col-12 col-xl-3"><label class="form-label small mb-1" for="fBusca">Pesquisar</label>
        <input id="fBusca" class="form-control" placeholder="${ehPaciente ? 'Profissional ou especialidade' : 'Paciente, profissional ou especialidade'}"></div>
      <div class="col-6 col-xl-2"><label class="form-label small mb-1" for="fPeriodo">Período</label>
        <select id="fPeriodo" class="form-select">
          <option value="mes">Próximos 30 dias</option><option value="hoje">Hoje</option><option value="semana">Próximos 7 dias</option>
          <option value="passadas">Passadas</option><option value="todas">Todo o histórico</option></select></div>
      <div class="col-6 col-xl-2"><label class="form-label small mb-1" for="fStatus">Status</label>
        <select id="fStatus" class="form-select">
          <option value="">Ativas e realizadas</option><option value="AGENDADA">Agendada</option><option value="CONFIRMADA">Confirmada</option>
          <option value="REALIZADA">Realizada</option><option value="CANCELADA">Cancelada</option><option value="TODAS">Todas (com canceladas)</option></select></div>
      ${profs.length ? `<div class="col-12 col-xl-3"><label class="form-label small mb-1" for="fProf">Profissional</label>
        <select id="fProf" class="form-select"><option value="">Todos</option>${profs.map((p) => `<option value="${p.id}">${esc(p.nome)}</option>`).join('')}</select></div>` : ''}
      <div class="col-12 col-md ms-md-auto text-md-end">${podeAgendar ? '<button class="btn btn-primary" id="novo"><i class="bi bi-calendar-plus me-1"></i>Novo agendamento</button>' : ''}</div>
    </div></div></div>
    <div class="card"><div class="table-responsive"><table class="table table-hover mb-0">
      <thead><tr><th>Data</th><th>Horário</th>${ehPaciente ? '' : '<th>Paciente</th>'}<th>Profissional</th><th>Especialidade</th><th>Status</th><th class="text-end">Ações</th></tr></thead>
      <tbody id="corpo"></tbody></table></div>
      <div class="card-footer" id="paginacao"></div></div>`;

  const $ = (s) => raiz.querySelector(s);

  async function carregar() {
    const [inicio, fim] = PERIODOS[$('#fPeriodo').value]();
    try {
      lista = await api.get('/agendamentos' + qs({ inicio, fim, status: $('#fStatus').value, busca: $('#fBusca').value.trim(), profissionalId: $('#fProf')?.value }));
    } catch (e) { return erro(e); }
    pagina = 1;
    desenhar();
  }

  function acoes(a) {
    const ativa = a.status === 'AGENDADA' || a.status === 'CONFIRMADA';
    let h = `<a class="btn btn-sm btn-outline-primary" href="#/consulta/${a.id}" title="Detalhes"><i class="bi bi-eye"></i></a>`;
    if (usuario.perfil === 'RECEPCIONISTA' && ativa) h += ` <button class="btn btn-sm btn-outline-secondary" data-remarcar="${a.id}" title="Remarcar"><i class="bi bi-calendar2-week"></i></button>`;
    if (ativa && usuario.perfil !== 'ADMINISTRADOR') h += ` <button class="btn btn-sm btn-outline-danger" data-cancelar="${a.id}" title="Cancelar consulta"><i class="bi bi-x-circle"></i></button>`;
    return h;
  }

  function desenhar() {
    const p = paginar(lista, pagina, 10);
    pagina = p.pagina;
    $('#corpo').innerHTML = p.itens.length ? p.itens.map((a) => `<tr>
        <td>${fmtData(a.data)}</td><td class="fw-semibold">${fmtHora(a.hora)}</td>
        ${ehPaciente ? "" : `<td class="text-nowrap">${esc(a.pacienteNome)}</td>`}
        <td class="text-nowrap">${esc(a.profissionalNome)}</td><td class="text-nowrap">${esc(a.especialidade)}</td>
        <td>${badgeStatus(a.status, a.statusRotulo)}${a.remarcacaoSolicitada ? ' <span class="badge text-bg-warning" title="O paciente pediu remarcação">Remarcar</span>' : ''}</td>
        <td class="text-end text-nowrap">${acoes(a)}</td></tr>`).join('')
      : `<tr><td colspan="7">${estadoVazio('bi-calendar-x', 'Nenhuma consulta encontrada com estes filtros.')}</td></tr>`;
    $('#paginacao').innerHTML = htmlPaginacao(p, lista.length);
    $('#paginacao').querySelectorAll('[data-pag]').forEach((b) => b.addEventListener('click', () => { pagina = Number(b.dataset.pag); desenhar(); }));
    $('#corpo').querySelectorAll('[data-remarcar]').forEach((b) => b.addEventListener('click', () =>
      abrirFormAgendamento({ agendamento: lista.find((x) => x.id === Number(b.dataset.remarcar)), onSalvo: carregar })));
    $('#corpo').querySelectorAll('[data-cancelar]').forEach((b) => b.addEventListener('click', async () => {
      const a = lista.find((x) => x.id === Number(b.dataset.cancelar));
      const ok = await confirmar({ titulo: 'Cancelar consulta', textoConfirmar: 'Sim, cancelar consulta',
        mensagem: `Deseja cancelar a consulta de <strong>${esc(a.pacienteNome)}</strong> com <strong>${esc(a.profissionalNome)}</strong> em ${fmtData(a.data)} às ${fmtHora(a.hora)}? O horário ficará livre.` });
      if (!ok) return;
      try { await api.del('/agendamentos/' + a.id); toast('Consulta cancelada com sucesso.'); atualizarBadges(); carregar(); } catch (e) { erro(e); }
    }));
  }

  $('#fBusca').addEventListener('input', debounce(carregar, 300));
  ['#fPeriodo', '#fStatus', '#fProf'].forEach((s) => $(s)?.addEventListener('change', carregar));
  $('#novo')?.addEventListener('click', () => abrirFormAgendamento({ onSalvo: carregar }));
  await carregar();
}
