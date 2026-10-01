import { api } from '../api.js';
import { abrirModal, esc, confirmar, toast, erro } from '../ui.js';
import { emailValido, senhaForte } from '../validacao.js';

const PERFIS = { ADMINISTRADOR: 'Administrador', RECEPCIONISTA: 'Recepcionista', PROFISSIONAL: 'Profissional', PACIENTE: 'Paciente' };
const COR = { ADMINISTRADOR: 'dark', RECEPCIONISTA: 'primary', PROFISSIONAL: 'success', PACIENTE: 'secondary' };

// Matriz exibida na tela para explicar as permissões implementadas na API.
const MATRIZ = [
  ['Ver dashboard e indicadores', 1, 1, 1, 1], ['Ver agenda', 1, 1, 'própria', 0], ['Criar / remarcar agendamentos', 0, 1, 0, 'só para si*'],
  ['Cancelar consultas', 0, 1, 'próprias', 'próprias'], ['Marcar consulta como realizada', 0, 0, 1, 0], ['Cadastrar / editar pacientes', 0, 1, 0, 0],
  ['Ver dados de pacientes', 0, 'completos', 'mínimos', 0], ['Cadastrar / desativar profissionais', 1, 1, 0, 0], ['Usuários e permissões', 1, 0, 0, 0],
  ['Configurações do sistema', 1, 0, 0, 0], ['Notificações e lembretes', 1, 1, 'próprias', 'próprias'],
];
const celula = (v) => v === 1 ? '<i class="bi bi-check-lg text-success"></i>' : v === 0 ? '<i class="bi bi-dash text-secondary"></i>' : `<span class="small">${esc(v)}</span>`;

export async function renderizar(raiz, { usuario }) {
  let lista = [];
  raiz.innerHTML = `
    <div class="card mb-3"><div class="card-header d-flex justify-content-between align-items-center"><span><i class="bi bi-people me-2"></i>Usuários do sistema</span>
      <button class="btn btn-primary btn-sm" id="novo"><i class="bi bi-person-plus me-1"></i>Novo usuário</button></div>
      <div class="table-responsive"><table class="table table-hover mb-0"><thead><tr><th>Nome</th><th>E-mail</th><th>Perfil</th><th>Vínculo</th><th>Status</th><th class="text-end">Ações</th></tr></thead><tbody id="corpo"></tbody></table></div></div>
    <div class="card"><div class="card-header"><i class="bi bi-shield-lock me-2"></i>Permissões por perfil</div>
      <div class="table-responsive"><table class="table table-sm mb-0 text-center align-middle"><thead><tr><th class="text-start">Funcionalidade</th>
        ${Object.values(PERFIS).map((p) => `<th>${p}</th>`).join('')}</tr></thead>
        <tbody>${MATRIZ.map(([f, ...v]) => `<tr><td class="text-start">${f}</td>${v.map((x) => `<td>${celula(x)}</td>`).join('')}</tr>`).join('')}</tbody></table></div>
      <div class="card-footer small text-secondary">* O paciente agenda apenas para o próprio cadastro. As permissões são verificadas na API (não apenas escondidas na tela).</div></div>`;
  const $ = (s) => raiz.querySelector(s);

  function desenhar() {
    $('#corpo').innerHTML = lista.map((u) => {
      const vinculado = u.profissionalId || u.pacienteId;
      const eu = u.id === usuario.id;
      return `<tr class="${u.ativo ? '' : 'text-secondary'}"><td class="fw-semibold">${esc(u.nome)}${eu ? ' <span class="badge text-bg-info">você</span>' : ''}</td><td>${esc(u.email)}</td>
        <td>${vinculado || eu ? `<span class="badge text-bg-${COR[u.perfil]}">${PERFIS[u.perfil]}</span>`
          : `<select class="form-select form-select-sm w-auto" data-perfil="${u.id}" aria-label="Perfil de ${esc(u.nome)}">
             ${['ADMINISTRADOR', 'RECEPCIONISTA'].map((p) => `<option value="${p}" ${u.perfil === p ? 'selected' : ''}>${PERFIS[p]}</option>`).join('')}</select>`}</td>
        <td class="small">${u.profissionalId ? 'Profissional #' + u.profissionalId : u.pacienteId ? 'Paciente #' + u.pacienteId : '—'}</td>
        <td>${u.ativo ? '<span class="badge text-bg-success">Ativo</span>' : '<span class="badge text-bg-secondary">Desativado</span>'}</td>
        <td class="text-end">${eu ? '' : `<button class="btn btn-sm btn-outline-${u.ativo ? 'danger' : 'success'}" data-ativo="${u.id}">${u.ativo ? 'Desativar' : 'Reativar'}</button>`}</td></tr>`;
    }).join('');
    $('#corpo').querySelectorAll('[data-perfil]').forEach((s) => s.addEventListener('change', async () => {
      try { await api.patch(`/usuarios/${s.dataset.perfil}/perfil`, { perfil: s.value }); toast('Perfil atualizado com sucesso.'); carregar(); } catch (e) { erro(e); carregar(); }
    }));
    $('#corpo').querySelectorAll('[data-ativo]').forEach((b) => b.addEventListener('click', async () => {
      const u = lista.find((x) => x.id === Number(b.dataset.ativo));
      if (u.ativo) {
        const ok = await confirmar({ titulo: 'Desativar usuário', textoConfirmar: 'Sim, desativar', mensagem: `<strong>${esc(u.nome)}</strong> perderá o acesso ao sistema imediatamente. Deseja continuar?` });
        if (!ok) return;
      }
      try { await api.patch(`/usuarios/${u.id}/ativo?valor=${!u.ativo}`); toast(u.ativo ? 'Usuário desativado com sucesso.' : 'Usuário reativado com sucesso.'); carregar(); } catch (e) { erro(e); }
    }));
  }

  async function carregar() { try { lista = await api.get('/usuarios'); desenhar(); } catch (e) { erro(e); } }

  $('#novo').addEventListener('click', () => {
    const m = abrirModal({ titulo: '<i class="bi bi-person-plus me-2"></i>Novo usuário',
      corpo: `<form novalidate><div id="uErro" class="alert alert-danger d-none" role="alert"></div><div class="row g-3">
        <div class="col-12"><label class="form-label" for="uNome">Nome *</label><input id="uNome" class="form-control" maxlength="120"><div class="invalid-feedback">Informe o nome.</div></div>
        <div class="col-12"><label class="form-label" for="uEmail">E-mail *</label><input id="uEmail" type="email" class="form-control" maxlength="120"><div class="invalid-feedback">E-mail inválido.</div></div>
        <div class="col-md-6"><label class="form-label" for="uPerfil">Perfil *</label><select id="uPerfil" class="form-select"><option value="RECEPCIONISTA">Recepcionista</option><option value="ADMINISTRADOR">Administrador</option></select></div>
        <div class="col-md-6"><label class="form-label" for="uSenha">Senha inicial *</label><input id="uSenha" class="form-control" autocomplete="off"><div class="invalid-feedback">Mínimo 8 caracteres, com letras e números.</div></div>
        <div class="col-12 small text-secondary">Profissionais e pacientes recebem acesso em seus próprios cadastros.</div></div></form>`,
      rodape: '<button class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button><button id="uSalvar" class="btn btn-primary">Criar usuário</button>' });
    const f = (id) => m.el.querySelector('#' + id);
    f('uSalvar').addEventListener('click', async () => {
      const regras = [['uNome', f('uNome').value.trim().length >= 3], ['uEmail', emailValido(f('uEmail').value)], ['uSenha', senhaForte(f('uSenha').value)]];
      let ok = true;
      regras.forEach(([id, v]) => { f(id).classList.toggle('is-invalid', !v); if (!v) ok = false; });
      if (!ok) return;
      try {
        await api.post('/usuarios', { nome: f('uNome').value, email: f('uEmail').value, senha: f('uSenha').value, perfil: f('uPerfil').value });
        m.fechar(); toast('Usuário criado com sucesso.'); carregar();
      } catch (e) { f('uErro').textContent = e.message; f('uErro').classList.remove('d-none'); }
    });
  });
  await carregar();
}
