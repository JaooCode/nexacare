import { api, query as qs } from '../api.js';
import { abrirModal, esc, fmtData, fmtHora, idade, hojeISO, badgeStatus, estadoVazio, paginar, htmlPaginacao, debounce, confirmar, toast, erro } from '../ui.js';
import { cpfValido, emailValido, telefoneValido, ativarMascaras } from '../validacao.js';

export async function renderizar(raiz, { usuario }) {
  const recepcao = usuario.perfil === 'RECEPCIONISTA';
  let pagina = 1, lista = [];

  raiz.innerHTML = `
    <div class="card mb-3"><div class="card-body"><div class="row g-2 align-items-end">
      <div class="col-12 col-md-5"><label class="form-label small mb-1" for="busca">Pesquisar</label>
        <div class="input-group"><span class="input-group-text"><i class="bi bi-search"></i></span>
        <input id="busca" class="form-control" placeholder="Nome${recepcao ? ', CPF, e-mail ou telefone' : ' ou CPF'}"></div></div>
      ${recepcao ? `<div class="col-6 col-md-auto"><div class="form-check form-switch mb-2"><input class="form-check-input" type="checkbox" id="inativos">
        <label class="form-check-label" for="inativos">Mostrar desativados</label></div></div>` : ''}
      <div class="col-12 col-md ms-md-auto text-md-end">${recepcao ? '<button class="btn btn-primary" id="novo"><i class="bi bi-person-plus me-1"></i>Novo paciente</button>' : ''}</div>
    </div></div></div>
    ${usuario.perfil === 'PROFISSIONAL' ? '<div class="aviso-lgpd mb-3"><i class="bi bi-shield-check me-1"></i>Por segurança e LGPD, você vê apenas pacientes que já têm consulta com você e somente os dados necessários ao atendimento.</div>' : ''}
    <div class="card"><div class="table-responsive"><table class="table table-hover mb-0">
      <thead><tr><th>Nome</th>${recepcao ? '<th>CPF</th>' : ''}<th>Nascimento</th><th>Telefone</th>${recepcao ? '<th>E-mail</th>' : ''}<th>Status</th><th class="text-end">Ações</th></tr></thead>
      <tbody id="corpo"></tbody></table></div><div class="card-footer" id="paginacao"></div></div>`;
  const $ = (s) => raiz.querySelector(s);

  async function carregar() {
    try { lista = await api.get('/pacientes' + qs({ busca: $('#busca').value.trim(), incluirInativos: $('#inativos')?.checked ? 'true' : '' })); } catch (e) { return erro(e); }
    pagina = 1; desenhar();
  }

  function desenhar() {
    const p = paginar(lista, pagina, 10);
    pagina = p.pagina;
    $('#corpo').innerHTML = p.itens.length ? p.itens.map((x) => `<tr class="${x.ativo ? '' : 'text-secondary'}">
        <td class="fw-semibold">${esc(x.nome)}</td>${recepcao ? `<td>${esc(x.cpf)}</td>` : ''}
        <td>${fmtData(x.dataNascimento)} <span class="small text-secondary">(${idade(x.dataNascimento)} anos)</span></td><td>${esc(x.telefone)}</td>
        ${recepcao ? `<td>${esc(x.email)}</td>` : ''}
        <td>${x.ativo ? '<span class="badge text-bg-success">Ativo</span>' : '<span class="badge text-bg-secondary">Desativado</span>'}</td>
        <td class="text-end text-nowrap">
          <button class="btn btn-sm btn-outline-primary" data-ver="${x.id}" title="Visualizar"><i class="bi bi-eye"></i></button>
          ${recepcao ? `<button class="btn btn-sm btn-outline-secondary" data-editar="${x.id}" title="Editar"><i class="bi bi-pencil"></i></button>
          <button class="btn btn-sm btn-outline-${x.ativo ? 'danger' : 'success'}" data-status="${x.id}" title="${x.ativo ? 'Desativar' : 'Reativar'}"><i class="bi ${x.ativo ? 'bi-person-x' : 'bi-person-check'}"></i></button>` : ''}
        </td></tr>`).join('')
      : `<tr><td colspan="7">${estadoVazio('bi-people', 'Nenhum paciente encontrado.')}</td></tr>`;
    $('#paginacao').innerHTML = htmlPaginacao(p, lista.length);
    $('#paginacao').querySelectorAll('[data-pag]').forEach((b) => b.addEventListener('click', () => { pagina = Number(b.dataset.pag); desenhar(); }));
    const achar = (v) => lista.find((x) => x.id === Number(v));
    $('#corpo').querySelectorAll('[data-ver]').forEach((b) => b.addEventListener('click', () => visualizar(achar(b.dataset.ver))));
    $('#corpo').querySelectorAll('[data-editar]').forEach((b) => b.addEventListener('click', () => formulario(achar(b.dataset.editar))));
    $('#corpo').querySelectorAll('[data-status]').forEach((b) => b.addEventListener('click', () => alternarStatus(achar(b.dataset.status))));
  }

  async function alternarStatus(p) {
    const desativar = p.ativo;
    if (desativar) {
      const ok = await confirmar({ titulo: 'Desativar paciente', textoConfirmar: 'Sim, desativar',
        mensagem: `Deseja desativar o cadastro de <strong>${esc(p.nome)}</strong>? O histórico de consultas é preservado, mas não será possível criar novos agendamentos.` });
      if (!ok) return;
    }
    try { await api.patch(`/pacientes/${p.id}/status?ativo=${!desativar}`); toast(desativar ? 'Paciente desativado com sucesso.' : 'Paciente reativado com sucesso.'); carregar(); } catch (e) { erro(e); }
  }

  async function visualizar(p) {
    let consultas = [];
    try { consultas = await api.get('/agendamentos' + qs({ pacienteId: p.id, status: 'TODAS' })); } catch { /* histórico é complementar */ }
    abrirModal({ titulo: `<i class="bi bi-person me-2"></i>${esc(p.nome)}`, tamanho: 'modal-lg',
      corpo: `<dl class="row detalhe">
        ${recepcao ? `<dt class="col-sm-3">CPF</dt><dd class="col-sm-9">${esc(p.cpf)}</dd>` : ''}
        <dt class="col-sm-3">Nascimento</dt><dd class="col-sm-9">${fmtData(p.dataNascimento)} (${idade(p.dataNascimento)} anos)</dd>
        <dt class="col-sm-3">Telefone</dt><dd class="col-sm-9">${esc(p.telefone)}</dd>
        ${recepcao ? `<dt class="col-sm-3">E-mail</dt><dd class="col-sm-9">${esc(p.email)}</dd>` : ''}
        <dt class="col-sm-3">Observação</dt><dd class="col-sm-9">${p.observacao ? esc(p.observacao) : '—'}</dd></dl>
        <h3 class="h6">Histórico de consultas</h3>
        ${consultas.length ? `<div class="table-responsive"><table class="table table-sm"><tbody>${consultas.map((c) => `<tr>
          <td>${fmtData(c.data)} ${fmtHora(c.hora)}</td><td>${esc(c.profissionalNome)}</td><td>${badgeStatus(c.status, c.statusRotulo)}</td>
          <td><a href="#/consulta/${c.id}" data-fechar>detalhes</a></td></tr>`).join('')}</tbody></table></div>` : '<div class="text-secondary">Sem consultas.</div>'}`,
      rodape: '<button class="btn btn-outline-secondary" data-bs-dismiss="modal">Fechar</button>' });
  }

  function formulario(p) {
    const edicao = !!p;
    const m = abrirModal({ titulo: `<i class="bi bi-person-plus me-2"></i>${edicao ? 'Editar paciente' : 'Novo paciente'}`, tamanho: 'modal-lg',
      corpo: `<form id="formPac" novalidate>
        <div id="pacErro" class="alert alert-danger d-none" role="alert"></div>
        <div class="aviso-lgpd mb-3"><i class="bi bi-shield-check me-1"></i><strong>Minimização de dados (LGPD):</strong> colete somente o necessário para agendar e contatar o paciente. Não registre informações clínicas.</div>
        <div class="row g-3">
          <div class="col-12"><label class="form-label" for="pNome">Nome completo *</label><input id="pNome" class="form-control" maxlength="120" required>
            <div class="invalid-feedback">Informe o nome completo.</div></div>
          <div class="col-md-6"><label class="form-label" for="pCpf">CPF *</label><input id="pCpf" class="form-control" data-mask="cpf" inputmode="numeric" placeholder="000.000.000-00" required>
            <div class="invalid-feedback">CPF inválido.</div></div>
          <div class="col-md-6"><label class="form-label" for="pNasc">Data de nascimento *</label><input id="pNasc" type="date" class="form-control" max="${hojeISO()}" min="1900-01-01" required>
            <div class="invalid-feedback">Informe uma data de nascimento válida.</div></div>
          <div class="col-md-6"><label class="form-label" for="pTel">Telefone *</label><input id="pTel" class="form-control" data-mask="telefone" inputmode="tel" placeholder="(00) 00000-0000" required>
            <div class="invalid-feedback">Informe telefone com DDD.</div></div>
          <div class="col-md-6"><label class="form-label" for="pEmail">E-mail *</label><input id="pEmail" type="email" class="form-control" maxlength="120" required>
            <div class="invalid-feedback">E-mail inválido.</div></div>
          <div class="col-12"><label class="form-label" for="pObs">Observações administrativas</label><textarea id="pObs" class="form-control" rows="2" maxlength="255" placeholder="Ex.: prefere contato por telefone; necessita de acessibilidade"></textarea></div>
        </div></form>`,
      rodape: `<button class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button><button id="pSalvar" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>Salvar</button>` });
    const f = (id) => m.el.querySelector('#' + id);
    if (edicao) {
      f('pNome').value = p.nome; f('pCpf').value = p.cpf || ''; f('pNasc').value = p.dataNascimento;
      f('pTel').value = p.telefone; f('pEmail').value = p.email || ''; f('pObs').value = p.observacao || '';
    }
    ativarMascaras(m.el);
    f('pSalvar').addEventListener('click', async () => {
      const regras = [['pNome', f('pNome').value.trim().length >= 3], ['pCpf', cpfValido(f('pCpf').value)], ['pNasc', f('pNasc').value && f('pNasc').value < hojeISO()],
        ['pTel', telefoneValido(f('pTel').value)], ['pEmail', emailValido(f('pEmail').value)]];
      let ok = true;
      regras.forEach(([id, valido]) => { f(id).classList.toggle('is-invalid', !valido); if (!valido) ok = false; });
      const caixa = f('pacErro');
      caixa.classList.add('d-none');
      if (!ok) { caixa.textContent = 'Preencha todos os campos obrigatórios corretamente.'; caixa.classList.remove('d-none'); return; }
      const dados = { nome: f('pNome').value, cpf: f('pCpf').value, dataNascimento: f('pNasc').value, telefone: f('pTel').value, email: f('pEmail').value, observacao: f('pObs').value };
      try {
        if (edicao) await api.put('/pacientes/' + p.id, dados); else await api.post('/pacientes', dados);
        m.fechar(); toast(edicao ? 'Paciente atualizado com sucesso.' : 'Paciente cadastrado com sucesso.'); carregar();
      } catch (e) { caixa.textContent = e.message; caixa.classList.remove('d-none'); }
    });
  }

  $('#busca').addEventListener('input', debounce(carregar, 300));
  $('#inativos')?.addEventListener('change', carregar);
  $('#novo')?.addEventListener('click', () => formulario(null));
  await carregar();
}
