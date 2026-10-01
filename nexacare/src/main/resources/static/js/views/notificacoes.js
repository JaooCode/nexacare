import { api } from '../api.js';
import { esc, fmtDataHora, estadoVazio, toast, erro } from '../ui.js';

const TIPOS = {
  CONFIRMACAO: ['bi-check2-circle', 'Confirmação'], LEMBRETE: ['bi-alarm', 'Lembrete'], ALTERACAO: ['bi-arrow-repeat', 'Alteração'],
  CANCELAMENTO: ['bi-x-circle', 'Cancelamento'], SOLICITACAO_REMARCACAO: ['bi-question-circle', 'Remarcação'],
};

export async function renderizar(raiz, { usuario, atualizarBadges }) {
  const staff = ['ADMINISTRADOR', 'RECEPCIONISTA'].includes(usuario.perfil);
  let filtro = '', lista = [];

  raiz.innerHTML = `
    <div class="alert alert-info d-flex gap-2 align-items-start"><i class="bi bi-info-circle-fill mt-1"></i>
      <div><strong>Simulação:</strong> as mensagens abaixo representam e-mails/WhatsApp que o NexaCare enviaria. Nenhum envio real é feito neste protótipo.</div></div>
    <div class="card"><div class="card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
      <ul class="nav nav-pills" id="abas"></ul>
      <div class="d-flex flex-wrap gap-2">
        ${staff ? `<div class="input-group input-group-sm w-auto"><select id="dias" class="form-select form-select-sm" aria-label="Consultas de quando">
          <option value="1">Amanhã</option><option value="0">Hoje</option><option value="2">Em 2 dias</option><option value="3">Em 3 dias</option></select>
          <button class="btn btn-primary btn-sm" id="gerar"><i class="bi bi-alarm me-1"></i>Gerar lembretes</button></div>` : ''}
        <button class="btn btn-outline-secondary btn-sm" id="lerTodas"><i class="bi bi-check2-all me-1"></i>Marcar todas como lidas</button>
      </div></div>
      <div id="lista"></div></div>`;
  const $ = (s) => raiz.querySelector(s);

  function abas() {
    const conta = (t) => lista.filter((n) => !t || n.tipo === t).length;
    $('#abas').innerHTML = [['', 'Todas'], ...Object.entries(TIPOS).map(([k, v]) => [k, v[1]])].map(([k, r]) =>
      `<li class="nav-item"><button class="nav-link py-1 ${filtro === k ? 'active' : ''}" data-tipo="${k}">${r} <span class="badge text-bg-light">${conta(k)}</span></button></li>`).join('');
    $('#abas').querySelectorAll('[data-tipo]').forEach((b) => b.addEventListener('click', () => { filtro = b.dataset.tipo; desenhar(); }));
  }

  function desenhar() {
    abas();
    const itens = lista.filter((n) => !filtro || n.tipo === filtro);
    $('#lista').innerHTML = itens.length ? itens.map((n) => {
      const [ic, rot] = TIPOS[n.tipo];
      return `<a class="notif ${n.lida ? '' : 'nova'} text-decoration-none text-reset" href="#/consulta/${n.agendamentoId}" data-id="${n.id}">
        <div class="ic ic-${n.tipo}"><i class="bi ${ic}"></i></div>
        <div class="flex-grow-1"><div class="d-flex justify-content-between gap-2"><strong>${rot}</strong><span class="small text-secondary">${fmtDataHora(n.criadaEm)}</span></div>
          <div>${esc(n.mensagem)}</div>
          <div class="small text-secondary mt-1">${usuario.perfil === 'PACIENTE' ? '' : esc(n.pacienteNome) + ' · '}${usuario.perfil === 'PROFISSIONAL' ? '' : esc(n.profissionalNome)}${n.lida ? '' : ' <span class="badge text-bg-danger ms-1">nova</span>'}</div></div></a>`;
    }).join('') : estadoVazio('bi-bell-slash', 'Nenhuma notificação.');
    $('#lista').querySelectorAll('[data-id]').forEach((a) => a.addEventListener('click', async () => {
      try { await api.patch(`/notificacoes/${a.dataset.id}/lida`); atualizarBadges(); } catch { /* navegação continua */ }
    }));
  }

  async function carregar() {
    try { lista = await api.get('/notificacoes'); } catch (e) { return erro(e); }
    desenhar();
  }

  $('#lerTodas').addEventListener('click', async () => {
    try { await api.post('/notificacoes/lidas'); toast('Todas as notificações foram marcadas como lidas.'); atualizarBadges(); carregar(); } catch (e) { erro(e); }
  });
  $('#gerar')?.addEventListener('click', async () => {
    try { const r = await api.post('/notificacoes/lembretes?dias=' + $('#dias').value); toast(r.mensagem, 'info'); atualizarBadges(); carregar(); } catch (e) { erro(e); }
  });
  await carregar();
}
