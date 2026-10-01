import { api, query as qs } from '../api.js';
import { abrirModal, esc, estadoVazio, paginar, htmlPaginacao, debounce, confirmar, toast, erro } from '../ui.js';
import { emailValido, telefoneValido, senhaForte, ativarMascaras } from '../validacao.js';

export async function renderizar(raiz, { usuario }) {
  let pagina = 1, lista = [];
  const especialidades = await api.get('/especialidades');

  raiz.innerHTML = `
    <div class="card mb-3"><div class="card-body"><div class="row g-2 align-items-end">
      <div class="col-12 col-md-4"><label class="form-label small mb-1" for="busca">Pesquisar</label>
        <div class="input-group"><span class="input-group-text"><i class="bi bi-search"></i></span>
        <input id="busca" class="form-control" placeholder="Nome, especialidade ou registro"></div></div>
      <div class="col-6 col-md-3"><label class="form-label small mb-1" for="fEsp">Especialidade</label>
        <select id="fEsp" class="form-select"><option value="">Todas</option>${especialidades.map((e) => `<option value="${e.id}">${esc(e.nome)}</option>`).join('')}</select></div>
      <div class="col-6 col-md-auto"><div class="form-check form-switch mb-2"><input class="form-check-input" type="checkbox" id="inativos" checked>
        <label class="form-check-label" for="inativos">Mostrar inativos</label></div></div>
      <div class="col-12 col-md ms-md-auto text-md-end"><button class="btn btn-primary" id="novo"><i class="bi bi-person-plus me-1"></i>Novo profissional</button></div>
    </div></div></div>
    <div class="card"><div class="table-responsive"><table class="table table-hover mb-0">
      <thead><tr><th>Nome</th><th>Especialidade</th><th>Registro</th><th>Contato</th><th>Acesso</th><th>Status</th><th class="text-end">Ações</th></tr></thead>
      <tbody id="corpo"></tbody></table></div><div class="card-footer" id="paginacao"></div></div>`;
  const $ = (s) => raiz.querySelector(s);

  async function carregar() {
    try { lista = await api.get('/profissionais' + qs({ busca: $('#busca').value.trim(), especialidadeId: $('#fEsp').value, incluirInativos: $('#inativos').checked ? 'true' : '' })); } catch (e) { return erro(e); }
    pagina = 1; desenhar();
  }

  function desenhar() {
    const p = paginar(lista, pagina, 10);
    pagina = p.pagina;
    $('#corpo').innerHTML = p.itens.length ? p.itens.map((x) => `<tr class="${x.ativo ? '' : 'text-secondary'}">
        <td class="fw-semibold">${esc(x.nome)}</td><td>${esc(x.especialidade)}</td><td>${esc(x.registro || '—')}</td>
        <td><div>${esc(x.telefone)}</div><div class="small text-secondary">${esc(x.email)}</div></td>
        <td>${x.possuiAcesso ? '<i class="bi bi-key-fill text-success" title="Possui login"></i>' : '<span class="text-secondary" title="Sem login">—</span>'}</td>
        <td>${x.ativo ? '<span class="badge text-bg-success">Ativo</span>' : '<span class="badge text-bg-secondary">Inativo</span>'}</td>
        <td class="text-end text-nowrap">
          <a class="btn btn-sm btn-outline-primary" href="#/agenda?prof=${x.id}" title="Ver agenda"><i class="bi bi-calendar3"></i></a>
          <button class="btn btn-sm btn-outline-secondary" data-editar="${x.id}" title="Editar"><i class="bi bi-pencil"></i></button>
          <button class="btn btn-sm btn-outline-${x.ativo ? 'danger' : 'success'}" data-status="${x.id}" title="${x.ativo ? 'Desativar' : 'Ativar'}"><i class="bi ${x.ativo ? 'bi-toggle-on' : 'bi-toggle-off'}"></i></button>
        </td></tr>`).join('')
      : `<tr><td colspan="7">${estadoVazio('bi-person-badge', 'Nenhum profissional encontrado.')}</td></tr>`;
    $('#paginacao').innerHTML = htmlPaginacao(p, lista.length);
    $('#paginacao').querySelectorAll('[data-pag]').forEach((b) => b.addEventListener('click', () => { pagina = Number(b.dataset.pag); desenhar(); }));
    const achar = (v) => lista.find((x) => x.id === Number(v));
    $('#corpo').querySelectorAll('[data-editar]').forEach((b) => b.addEventListener('click', () => formulario(achar(b.dataset.editar))));
    $('#corpo').querySelectorAll('[data-status]').forEach((b) => b.addEventListener('click', () => alternar(achar(b.dataset.status))));
  }

  async function alternar(p) {
    if (p.ativo) {
      const ok = await confirmar({ titulo: 'Desativar profissional', textoConfirmar: 'Sim, desativar',
        mensagem: `Deseja desativar <strong>${esc(p.nome)}</strong>? Ele deixará de aparecer para novos agendamentos e o acesso ao sistema será bloqueado.` });
      if (!ok) return;
    }
    try { await api.patch(`/profissionais/${p.id}/status?ativo=${!p.ativo}`); toast(p.ativo ? 'Profissional desativado com sucesso.' : 'Profissional ativado com sucesso.'); carregar(); } catch (e) { erro(e); }
  }

  function formulario(p) {
    const edicao = !!p;
    const m = abrirModal({ titulo: `<i class="bi bi-person-badge me-2"></i>${edicao ? 'Editar profissional' : 'Novo profissional'}`, tamanho: 'modal-lg',
      corpo: `<form novalidate><div id="prErro" class="alert alert-danger d-none" role="alert"></div><div class="row g-3">
        <div class="col-md-8"><label class="form-label" for="prNome">Nome *</label><input id="prNome" class="form-control" maxlength="120" required><div class="invalid-feedback">Informe o nome.</div></div>
        <div class="col-md-4"><label class="form-label" for="prReg">Registro profissional</label><input id="prReg" class="form-control" maxlength="30" placeholder="Ex.: CRM-SP 000000"><div class="form-text">Quando aplicável.</div></div>
        <div class="col-md-6"><label class="form-label" for="prEsp">Especialidade *</label><select id="prEsp" class="form-select" required><option value="">Selecione...</option>
          ${especialidades.map((e) => `<option value="${e.id}">${esc(e.nome)}</option>`).join('')}</select><div class="invalid-feedback">Selecione a especialidade.</div></div>
        <div class="col-md-6"><label class="form-label" for="prTel">Telefone *</label><input id="prTel" class="form-control" data-mask="telefone" required><div class="invalid-feedback">Informe telefone com DDD.</div></div>
        <div class="col-md-8"><label class="form-label" for="prEmail">E-mail *</label><input id="prEmail" type="email" class="form-control" maxlength="120" required><div class="invalid-feedback">E-mail inválido.</div></div>
        <div class="col-md-4 d-flex align-items-end"><div class="form-check form-switch mb-2"><input class="form-check-input" type="checkbox" id="prAtivo" checked><label class="form-check-label" for="prAtivo">Profissional ativo</label></div></div>
        ${edicao ? '' : `<div class="col-12"><div class="border rounded p-3">
          <div class="form-check"><input class="form-check-input" type="checkbox" id="prAcesso"><label class="form-check-label" for="prAcesso">Criar acesso ao sistema para este profissional</label></div>
          <div id="prSenhaBox" class="mt-2 d-none"><label class="form-label" for="prSenha">Senha inicial</label><input id="prSenha" type="text" class="form-control" autocomplete="off" placeholder="Mínimo 8 caracteres, com letras e números">
            <div class="invalid-feedback">A senha deve ter no mínimo 8 caracteres, com letras e números.</div></div></div></div>`}
      </div></form>`,
      rodape: `<button class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button><button id="prSalvar" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>Salvar</button>` });
    const f = (id) => m.el.querySelector('#' + id);
    if (edicao) { f('prNome').value = p.nome; f('prReg').value = p.registro || ''; f('prEsp').value = p.especialidadeId; f('prTel').value = p.telefone; f('prEmail').value = p.email; f('prAtivo').checked = p.ativo; f('prAtivo').disabled = true; }
    ativarMascaras(m.el);
    f('prAcesso')?.addEventListener('change', (e) => f('prSenhaBox').classList.toggle('d-none', !e.target.checked));
    f('prSalvar').addEventListener('click', async () => {
      const comAcesso = f('prAcesso')?.checked;
      const regras = [['prNome', f('prNome').value.trim().length >= 3], ['prEsp', f('prEsp').value], ['prTel', telefoneValido(f('prTel').value)], ['prEmail', emailValido(f('prEmail').value)]];
      if (comAcesso) regras.push(['prSenha', senhaForte(f('prSenha').value)]);
      let ok = true;
      regras.forEach(([id, v]) => { f(id).classList.toggle('is-invalid', !v); if (!v) ok = false; });
      const caixa = f('prErro');
      caixa.classList.add('d-none');
      if (!ok) { caixa.textContent = 'Preencha todos os campos obrigatórios corretamente.'; caixa.classList.remove('d-none'); return; }
      const dados = { nome: f('prNome').value, especialidadeId: Number(f('prEsp').value), registro: f('prReg').value, telefone: f('prTel').value,
        email: f('prEmail').value, ativo: f('prAtivo').checked, senhaInicial: comAcesso ? f('prSenha').value : null };
      try {
        if (edicao) await api.put('/profissionais/' + p.id, dados); else await api.post('/profissionais', dados);
        m.fechar(); toast(edicao ? 'Profissional atualizado com sucesso.' : 'Profissional cadastrado com sucesso.'); carregar();
      } catch (e) { caixa.textContent = e.message; caixa.classList.remove('d-none'); }
    });
  }

  $('#busca').addEventListener('input', debounce(carregar, 300));
  $('#fEsp').addEventListener('change', carregar);
  $('#inativos').addEventListener('change', carregar);
  $('#novo').addEventListener('click', () => formulario(null));
  await carregar();
}
