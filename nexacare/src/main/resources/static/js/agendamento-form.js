// Formulário de agendamento (novo / remarcar) e comprovante de confirmação.
import { api, sessao, query } from './api.js';
import { abrirModal, esc, erro, fmtData, fmtHora, diaSemana, hojeISO, badgeStatus } from './ui.js';

/**
 * opcoes: { agendamento?, pacienteId?, profissionalId?, data?, hora?, onSalvo? }
 * Com `agendamento` o formulário funciona como edição/remarcação.
 */
export async function abrirFormAgendamento(opcoes = {}) {
  const usuario = sessao.usuario;
  const ehPaciente = usuario.perfil === 'PACIENTE';
  const edicao = opcoes.agendamento;

  let pacientes = [], profissionais, especialidades;
  try {
    [profissionais, especialidades] = await Promise.all([api.get('/profissionais'), api.get('/especialidades')]);
    if (!ehPaciente) pacientes = await api.get('/pacientes');
  } catch (e) { return erro(e); }

  const m = abrirModal({
    titulo: `<i class="bi bi-calendar-plus me-2"></i>${edicao ? 'Remarcar / editar consulta' : 'Novo agendamento'}`,
    tamanho: 'modal-lg',
    corpo: `<form id="formAg" novalidate>
      <div id="agErro" class="alert alert-danger d-none" role="alert"></div>
      <div class="row g-3">
        <div class="col-12">
          <label class="form-label">Paciente *</label>
          ${ehPaciente
            ? `<input class="form-control" value="${esc(usuario.nome)}" disabled>`
            : `<input id="agBuscaPac" class="form-control mb-1" placeholder="Digite para filtrar por nome ou CPF..." autocomplete="off">
               <select id="agPaciente" class="form-select" required></select>`}
          <div class="invalid-feedback">Selecione o paciente.</div>
        </div>
        <div class="col-md-6">
          <label class="form-label" for="agEsp">Especialidade *</label>
          <select id="agEsp" class="form-select" required>
            <option value="">Selecione...</option>
            ${especialidades.map((e) => `<option value="${e.id}">${esc(e.nome)}</option>`).join('')}
          </select>
          <div class="invalid-feedback">Selecione a especialidade.</div>
        </div>
        <div class="col-md-6">
          <label class="form-label" for="agProf">Profissional *</label>
          <select id="agProf" class="form-select" required></select>
          <div class="invalid-feedback">Selecione o profissional.</div>
        </div>
        <div class="col-md-5">
          <label class="form-label" for="agData">Data *</label>
          <input id="agData" type="date" class="form-control" min="${hojeISO()}" required>
          <div class="invalid-feedback">Informe uma data válida (a partir de hoje).</div>
        </div>
        <div class="col-md-7">
          <label class="form-label" for="agHora">Horário disponível *</label>
          <select id="agHora" class="form-select" required disabled><option value="">Escolha profissional e data</option></select>
          <div class="invalid-feedback">Selecione um horário disponível.</div>
          <div id="agHoraInfo" class="form-text"></div>
        </div>
        <div class="col-12">
          <label class="form-label" for="agObs">Observação administrativa</label>
          <textarea id="agObs" class="form-control" rows="2" maxlength="255" placeholder="Ex.: primeira consulta, retorno, preferência de contato"></textarea>
          <div class="form-text">Não registre informações clínicas (diagnósticos, exames, sintomas). <span id="agObsCont">0</span>/255</div>
        </div>
      </div>
    </form>`,
    rodape: `<button class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
             <button id="agSalvar" class="btn btn-primary"><i class="bi bi-check2-circle me-1"></i>${edicao ? 'Salvar alterações' : 'Confirmar agendamento'}</button>`,
  });

  const $ = (id) => m.el.querySelector('#' + id);
  const selPac = $('agPaciente'), selEsp = $('agEsp'), selProf = $('agProf'), inData = $('agData'), selHora = $('agHora');

  function listarPacientes(filtro = '') {
    if (!selPac) return;
    const f = filtro.trim().toLowerCase(), dig = f.replace(/\D/g, '');
    const atual = selPac.value || String(opcoes.pacienteId || edicao?.pacienteId || '');
    const lista = pacientes.filter((p) => p.ativo && (!f || p.nome.toLowerCase().includes(f) || (dig && (p.cpf || '').replace(/\D/g, '').includes(dig))));
    selPac.innerHTML = '<option value="">Selecione...</option>' + lista.map((p) =>
      `<option value="${p.id}" ${String(p.id) === atual ? 'selected' : ''}>${esc(p.nome)}${p.cpf ? ' — ' + esc(p.cpf) : ''}</option>`).join('');
  }
  function listarProfissionais() {
    const esp = selEsp.value;
    const atual = selProf.value || String(opcoes.profissionalId || edicao?.profissionalId || '');
    const lista = profissionais.filter((p) => p.ativo && (!esp || String(p.especialidadeId) === esp));
    selProf.innerHTML = '<option value="">Selecione...</option>' + lista.map((p) =>
      `<option value="${p.id}" ${String(p.id) === atual ? 'selected' : ''}>${esc(p.nome)}</option>`).join('');
  }
  async function carregarHorarios(horaSelecionada) {
    const prof = selProf.value, data = inData.value;
    $('agHoraInfo').textContent = '';
    if (!prof || !data) { selHora.disabled = true; selHora.innerHTML = '<option value="">Escolha profissional e data</option>'; return; }
    try {
      let livres = await api.get('/agendamentos/horarios-livres' + query({ profissionalId: prof, data }));
      livres = livres.map(fmtHora);
      // ao editar, o horário atual da própria consulta continua selecionável
      if (edicao && String(edicao.profissionalId) === prof && edicao.data === data && !livres.includes(fmtHora(edicao.hora))) {
        livres.push(fmtHora(edicao.hora)); livres.sort();
      }
      selHora.disabled = livres.length === 0;
      selHora.innerHTML = livres.length
        ? '<option value="">Selecione...</option>' + livres.map((h) => `<option value="${h}" ${h === horaSelecionada ? 'selected' : ''}>${h}</option>`).join('')
        : '<option value="">Sem horários livres neste dia</option>';
      $('agHoraInfo').textContent = livres.length
        ? `${livres.length} horário(s) livre(s) em ${diaSemana(data)}, ${fmtData(data)}.`
        : 'Não há horários livres. Escolha outra data ou outro profissional.';
    } catch (e) { erro(e); }
  }

  // valores iniciais
  listarPacientes();
  const profInicial = profissionais.find((p) => p.id === (opcoes.profissionalId || edicao?.profissionalId));
  if (profInicial) selEsp.value = String(profInicial.especialidadeId);
  listarProfissionais();
  inData.value = opcoes.data || edicao?.data || '';
  $('agObs').value = edicao?.observacao || '';
  $('agObsCont').textContent = $('agObs').value.length;
  if (selProf.value && inData.value) await carregarHorarios(opcoes.hora ? fmtHora(opcoes.hora) : edicao ? fmtHora(edicao.hora) : '');

  $('agBuscaPac')?.addEventListener('input', (e) => listarPacientes(e.target.value));
  selEsp.addEventListener('change', () => { selProf.value = ''; listarProfissionais(); carregarHorarios(); });
  selProf.addEventListener('change', () => carregarHorarios());
  inData.addEventListener('change', () => carregarHorarios());
  $('agObs').addEventListener('input', (e) => { $('agObsCont').textContent = e.target.value.length; });

  $('agSalvar').addEventListener('click', async () => {
    const boxErro = $('agErro');
    boxErro.classList.add('d-none');
    const campos = [[selPac, !selPac || selPac.value], [selEsp, selEsp.value], [selProf, selProf.value], [inData, inData.value && inData.value >= hojeISO()], [selHora, selHora.value]];
    let ok = true;
    campos.forEach(([el, valido]) => { if (el) { el.classList.toggle('is-invalid', !valido); if (!valido) ok = false; } });
    if (!ok) { boxErro.textContent = 'Preencha todos os campos obrigatórios.'; boxErro.classList.remove('d-none'); return; }

    const dados = {
      pacienteId: selPac ? Number(selPac.value) : null, especialidadeId: Number(selEsp.value),
      profissionalId: Number(selProf.value), data: inData.value, hora: selHora.value, observacao: $('agObs').value,
    };
    const btn = $('agSalvar');
    btn.disabled = true;
    try {
      const salvo = edicao ? await api.put('/agendamentos/' + edicao.id, dados) : await api.post('/agendamentos', dados);
      await mostrarComprovante(m, salvo, !!edicao);
      opcoes.onSalvo?.(salvo);
    } catch (e) {
      boxErro.textContent = e.message; boxErro.classList.remove('d-none'); btn.disabled = false;
      if (e.status === 409) carregarHorarios(); // horário foi ocupado por outra pessoa: atualiza a lista
    }
  });
}

async function mostrarComprovante(m, a, edicao) {
  let mensagem = '';
  try {
    const notas = await api.get('/notificacoes');
    mensagem = notas.find((n) => n.agendamentoId === a.id)?.mensagem || '';
  } catch { /* comprovante segue sem a mensagem simulada */ }

  m.el.querySelector('.modal-title').innerHTML = '<i class="bi bi-check-circle-fill text-success me-2"></i>' + (edicao ? 'Consulta atualizada' : 'Consulta agendada com sucesso!');
  m.corpo.innerHTML = `
    <div class="text-center mb-3"><i class="bi bi-calendar-check text-success" style="font-size:3rem"></i></div>
    <dl class="row detalhe mb-3">
      <dt class="col-sm-4">Paciente</dt><dd class="col-sm-8">${esc(a.pacienteNome)}</dd>
      <dt class="col-sm-4">Profissional</dt><dd class="col-sm-8">${esc(a.profissionalNome)} — ${esc(a.especialidade)}</dd>
      <dt class="col-sm-4">Data e horário</dt><dd class="col-sm-8">${diaSemana(a.data)}, ${fmtData(a.data)} às ${fmtHora(a.hora)}</dd>
      <dt class="col-sm-4">Status</dt><dd class="col-sm-8">${badgeStatus(a.status, a.statusRotulo)}</dd>
    </dl>
    ${mensagem ? `<div class="msg-simulada"><div class="small text-secondary mb-1"><i class="bi bi-chat-dots me-1"></i>Mensagem de confirmação (simulação — nenhum envio real)</div>${esc(mensagem)}</div>` : ''}`;
  m.rodape.innerHTML = `
    <button class="btn btn-outline-secondary" data-bs-dismiss="modal">Fechar</button>
    <a class="btn btn-outline-primary" href="#/consulta/${a.id}" data-fechar><i class="bi bi-eye me-1"></i>Ver detalhes</a>
    ${sessao.usuario.perfil === 'PACIENTE' ? '' : `<a class="btn btn-primary" href="#/agenda?data=${a.data}&prof=${a.profissionalId}" data-fechar><i class="bi bi-calendar3 me-1"></i>Ver na agenda</a>`}`;
}
