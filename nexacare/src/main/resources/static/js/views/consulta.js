import { api } from '../api.js';
import { esc, fmtData, fmtHora, fmtDataHora, diaSemana, hojeISO, badgeStatus, confirmar, toast, erro } from '../ui.js';
import { abrirFormAgendamento } from '../agendamento-form.js';

const ICONE_TIPO = {
  CONFIRMACAO: ['bi-check2-circle', 'Confirmação'], LEMBRETE: ['bi-alarm', 'Lembrete'], ALTERACAO: ['bi-arrow-repeat', 'Alteração'],
  CANCELAMENTO: ['bi-x-circle', 'Cancelamento'], SOLICITACAO_REMARCACAO: ['bi-question-circle', 'Pedido de remarcação'],
};

export async function renderizar(raiz, { usuario, parametros, atualizarBadges }) {
  const id = Number(parametros[0]);
  async function desenhar() {
    let a, notas;
    try {
      a = await api.get('/agendamentos/' + id);
      notas = (await api.get('/notificacoes')).filter((n) => n.agendamentoId === id);
    } catch (e) {
      raiz.innerHTML = `<div class="alert alert-danger">${esc(e.message)}</div><a href="#/consultas" class="btn btn-outline-primary">Voltar às consultas</a>`;
      return;
    }
    const ativa = a.status === 'AGENDADA' || a.status === 'CONFIRMADA';
    const p = usuario.perfil;
    const botoes = [];
    if (p === 'RECEPCIONISTA' && a.status === 'AGENDADA') botoes.push(['confirmar', 'success', 'bi-check2-circle', 'Confirmar consulta']);
    if (p === 'PACIENTE' && a.status === 'AGENDADA') botoes.push(['confirmar', 'success', 'bi-check2-circle', 'Confirmar presença']);
    if (p === 'PROFISSIONAL' && ativa && a.data <= hojeISO()) botoes.push(['realizar', 'success', 'bi-clipboard2-check', 'Marcar como realizada']);
    if (p === 'RECEPCIONISTA' && ativa) botoes.push(['remarcar', 'primary', 'bi-calendar2-week', 'Remarcar / editar']);
    if (p === 'PACIENTE' && ativa && !a.remarcacaoSolicitada) botoes.push(['solicitar', 'primary', 'bi-arrow-repeat', 'Solicitar remarcação']);
    if (ativa && p !== 'ADMINISTRADOR') botoes.push(['cancelar', 'outline-danger', 'bi-x-circle', 'Cancelar consulta']);

    raiz.innerHTML = `
      <a href="#/consultas" class="btn btn-link px-0 mb-2"><i class="bi bi-arrow-left me-1"></i>Voltar</a>
      <div class="row g-3">
        <div class="col-lg-7"><div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center"><span><i class="bi bi-clipboard2-pulse me-2"></i>Consulta #${a.id}</span>${badgeStatus(a.status, a.statusRotulo)}</div>
          <div class="card-body">
            ${a.remarcacaoSolicitada ? '<div class="alert alert-warning py-2"><i class="bi bi-exclamation-circle me-1"></i>O paciente solicitou remarcação. A recepção deve definir um novo horário.</div>' : ''}
            <dl class="row detalhe mb-0">
              <dt class="col-sm-4">Paciente</dt><dd class="col-sm-8">${esc(a.pacienteNome)}${p === 'PACIENTE' ? '' : `<div class="small text-secondary"><i class="bi bi-telephone me-1"></i>${esc(a.pacienteTelefone)}</div>`}</dd>
              <dt class="col-sm-4">Profissional</dt><dd class="col-sm-8">${esc(a.profissionalNome)}</dd>
              <dt class="col-sm-4">Especialidade</dt><dd class="col-sm-8">${esc(a.especialidade)}</dd>
              <dt class="col-sm-4">Data</dt><dd class="col-sm-8"><span class="text-capitalize">${diaSemana(a.data)}</span>, ${fmtData(a.data)}</dd>
              <dt class="col-sm-4">Horário</dt><dd class="col-sm-8">${fmtHora(a.hora)}</dd>
              <dt class="col-sm-4">Status</dt><dd class="col-sm-8">${badgeStatus(a.status, a.statusRotulo)}</dd>
              <dt class="col-sm-4">Observação</dt><dd class="col-sm-8">${a.observacao ? esc(a.observacao) : '<span class="text-secondary">—</span>'}</dd>
              <dt class="col-sm-4">Agendada em</dt><dd class="col-sm-8">${fmtDataHora(a.criadoEm)}</dd>
            </dl>
          </div>
          ${botoes.length ? `<div class="card-footer d-flex flex-wrap gap-2">${botoes.map(([acao, cor, ic, rot]) => `<button class="btn btn-${cor}" data-acao="${acao}"><i class="bi ${ic} me-1"></i>${rot}</button>`).join('')}</div>` : ''}
        </div></div>
        <div class="col-lg-5"><div class="card h-100">
          <div class="card-header"><i class="bi bi-chat-dots me-2"></i>Mensagens desta consulta <span class="badge text-bg-secondary ms-1">simulação</span></div>
          <div class="card-body">${notas.length ? notas.map((n) => {
            const [ic, rot] = ICONE_TIPO[n.tipo];
            return `<div class="d-flex gap-2 mb-3"><div class="notif" style="padding:0;border:0"><div class="ic ic-${n.tipo}"><i class="bi ${ic}"></i></div></div>
              <div><div class="small text-secondary">${rot} · ${fmtDataHora(n.criadaEm)}</div><div>${esc(n.mensagem)}</div></div></div>`;
          }).join('') : '<div class="text-secondary">Nenhuma mensagem registrada.</div>'}</div>
        </div></div>
      </div>`;

    const mudarStatus = async (status, msg) => {
      try { await api.patch(`/agendamentos/${id}/status`, { status }); toast(msg); atualizarBadges(); desenhar(); } catch (e) { erro(e); }
    };
    raiz.querySelectorAll('[data-acao]').forEach((b) => b.addEventListener('click', async () => {
      switch (b.dataset.acao) {
        case 'confirmar': return mudarStatus('CONFIRMADA', 'Consulta confirmada com sucesso.');
        case 'realizar': return mudarStatus('REALIZADA', 'Consulta marcada como realizada.');
        case 'remarcar': return abrirFormAgendamento({ agendamento: a, onSalvo: desenhar });
        case 'solicitar': {
          const ok = await confirmar({ titulo: 'Solicitar remarcação', perigo: false, textoConfirmar: 'Enviar solicitação',
            mensagem: 'A recepção será avisada e entrará em contato para definir um novo horário. Deseja continuar?' });
          if (!ok) return;
          try { await api.post(`/agendamentos/${id}/solicitar-remarcacao`); toast('Solicitação de remarcação enviada.'); atualizarBadges(); desenhar(); } catch (e) { erro(e); }
          return;
        }
        case 'cancelar': {
          const ok = await confirmar({ titulo: 'Cancelar consulta', textoConfirmar: 'Sim, cancelar consulta',
            mensagem: `Deseja realmente cancelar a consulta de <strong>${esc(a.pacienteNome)}</strong> em ${fmtData(a.data)} às ${fmtHora(a.hora)}? Esta ação não pode ser desfeita.` });
          if (!ok) return;
          try { await api.del('/agendamentos/' + id); toast('Consulta cancelada com sucesso.'); atualizarBadges(); desenhar(); } catch (e) { erro(e); }
        }
      }
    }));
  }
  await desenhar();
}
