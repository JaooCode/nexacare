import { api, sessao } from '../api.js';
import { esc, aplicarTema, toast, erro } from '../ui.js';
import { senhaForte } from '../validacao.js';

const ROTULO = { ADMINISTRADOR: 'Administrador', RECEPCIONISTA: 'Recepcionista', PROFISSIONAL: 'Profissional de saúde', PACIENTE: 'Paciente' };

export async function renderizar(raiz, { usuario }) {
  const admin = usuario.perfil === 'ADMINISTRADOR';
  let me = await api.get('/auth/me');

  raiz.innerHTML = `
    <ul class="nav nav-tabs mb-3" role="tablist">
      <li class="nav-item"><button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tDados" type="button">Meus dados</button></li>
      <li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tPref" type="button">Preferências</button></li>
      <li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tSeg" type="button">Senha</button></li>
      ${admin ? '<li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tSis" type="button">Sistema</button></li>' : ''}
      <li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tPriv" type="button">Privacidade</button></li>
    </ul>
    <div class="tab-content">
      <div class="tab-pane fade show active" id="tDados"><div class="card"><div class="card-body"><form id="fDados" class="row g-3" style="max-width:640px" novalidate>
        <div class="col-12"><label class="form-label" for="cNome">Nome</label><input id="cNome" class="form-control" maxlength="120" value="${esc(me.nome)}"><div class="invalid-feedback">Informe o nome.</div></div>
        <div class="col-md-8"><label class="form-label" for="cEmail">E-mail (login)</label><input id="cEmail" class="form-control" value="${esc(me.email)}" disabled></div>
        <div class="col-md-4"><label class="form-label" for="cPerfil">Perfil</label><input id="cPerfil" class="form-control" value="${esc(ROTULO[me.perfil])}" disabled></div>
        <div class="col-12"><button class="btn btn-primary" type="submit"><i class="bi bi-check2 me-1"></i>Salvar dados</button></div></form></div></div></div>

      <div class="tab-pane fade" id="tPref"><div class="card"><div class="card-body"><form id="fPref" style="max-width:520px">
        <div class="form-check form-switch mb-3"><input class="form-check-input" type="checkbox" id="cNotif" ${me.notificacoesAtivas ? 'checked' : ''}>
          <label class="form-check-label" for="cNotif">Receber notificações e lembretes de consultas (simulado)</label></div>
        <div class="mb-3"><label class="form-label" for="cTema">Tema da interface</label>
          <select id="cTema" class="form-select"><option value="claro" ${me.tema === 'claro' ? 'selected' : ''}>Claro</option><option value="escuro" ${me.tema === 'escuro' ? 'selected' : ''}>Escuro</option></select></div>
        <button class="btn btn-primary" type="submit"><i class="bi bi-check2 me-1"></i>Salvar preferências</button></form></div></div></div>

      <div class="tab-pane fade" id="tSeg"><div class="card"><div class="card-body"><form id="fSenha" class="row g-3" style="max-width:520px" novalidate>
        <div class="col-12"><label class="form-label" for="sAtual">Senha atual</label><input id="sAtual" type="password" class="form-control" autocomplete="current-password"><div class="invalid-feedback">Informe a senha atual.</div></div>
        <div class="col-12"><label class="form-label" for="sNova">Nova senha</label><input id="sNova" type="password" class="form-control" autocomplete="new-password"><div class="invalid-feedback">Mínimo 8 caracteres, com letras e números.</div></div>
        <div class="col-12"><label class="form-label" for="sConf">Confirmar nova senha</label><input id="sConf" type="password" class="form-control" autocomplete="new-password"><div class="invalid-feedback">As senhas não conferem.</div></div>
        <div class="col-12"><button class="btn btn-primary" type="submit"><i class="bi bi-key me-1"></i>Alterar senha</button></div></form></div></div></div>

      ${admin ? '<div class="tab-pane fade" id="tSis"><div id="sistema"></div></div>' : ''}

      <div class="tab-pane fade" id="tPriv"><div class="card"><div class="card-body">
        <h2 class="h5">Seus dados e a LGPD</h2>
        <p>O NexaCare coleta apenas os dados necessários para agendar, confirmar e gerenciar consultas: nome, CPF, data de nascimento, telefone e e-mail. <strong>Nenhuma informação clínica é armazenada.</strong></p>
        <ul><li>O acesso é limitado ao que cada perfil precisa para trabalhar.</li><li>Senhas são guardadas apenas como hash (BCrypt).</li><li>Você pode solicitar correção ou exclusão dos seus dados à recepção da clínica.</li></ul>
        <a href="/privacidade.html" target="_blank" rel="noopener" class="btn btn-outline-primary">Ler a política completa</a></div></div></div>
    </div>`;
  const $ = (s) => raiz.querySelector(s);

  $('#fDados').addEventListener('submit', async (e) => {
    e.preventDefault();
    const nome = $('#cNome').value.trim();
    $('#cNome').classList.toggle('is-invalid', nome.length < 3);
    if (nome.length < 3) return;
    salvarPreferencias({ nome, notificacoesAtivas: me.notificacoesAtivas, tema: me.tema }, 'Dados atualizados com sucesso.');
  });
  $('#fPref').addEventListener('submit', (e) => {
    e.preventDefault();
    salvarPreferencias({ nome: me.nome, notificacoesAtivas: $('#cNotif').checked, tema: $('#cTema').value }, 'Preferências salvas com sucesso.');
  });
  async function salvarPreferencias(corpo, msg) {
    try {
      me = await api.put('/auth/preferencias', corpo);
      sessao.atualizarUsuario(me);
      aplicarTema(me.tema);
      toast(msg);
    } catch (err) { erro(err); }
  }

  $('#fSenha').addEventListener('submit', async (e) => {
    e.preventDefault();
    const regras = [['sAtual', $('#sAtual').value], ['sNova', senhaForte($('#sNova').value)], ['sConf', $('#sConf').value === $('#sNova').value && $('#sConf').value]];
    let ok = true;
    regras.forEach(([id, v]) => { $('#' + id).classList.toggle('is-invalid', !v); if (!v) ok = false; });
    if (!ok) return;
    try {
      await api.put('/auth/senha', { senhaAtual: $('#sAtual').value, novaSenha: $('#sNova').value });
      e.target.reset(); toast('Senha alterada com sucesso.');
    } catch (err) { erro(err); }
  });

  if (admin) await montarSistema($('#sistema'));
}

async function montarSistema(alvo) {
  const [cfg, esps] = await Promise.all([api.get('/configuracoes'), api.get('/especialidades')]);
  alvo.innerHTML = `<div class="row g-3">
    <div class="col-lg-6"><div class="card h-100"><div class="card-header">Horário de atendimento</div><div class="card-body">
      <form id="fCfg" class="row g-3" novalidate>
        <div class="col-6"><label class="form-label" for="cfIni">Início</label><input id="cfIni" type="time" class="form-control" value="${cfg.inicio}" required></div>
        <div class="col-6"><label class="form-label" for="cfFim">Término</label><input id="cfFim" type="time" class="form-control" value="${cfg.fim}" required></div>
        <div class="col-6"><label class="form-label" for="cfPI">Início da pausa</label><input id="cfPI" type="time" class="form-control" value="${cfg.pausaInicio}" required></div>
        <div class="col-6"><label class="form-label" for="cfPF">Fim da pausa</label><input id="cfPF" type="time" class="form-control" value="${cfg.pausaFim}" required></div>
        <div class="col-12"><label class="form-label" for="cfDur">Duração de cada consulta (minutos)</label><input id="cfDur" type="number" min="10" max="120" step="5" class="form-control" value="${cfg.duracaoMinutos}" required></div>
        <div class="col-12"><button class="btn btn-primary" type="submit"><i class="bi bi-check2 me-1"></i>Salvar horários</button></div></form></div></div></div>
    <div class="col-lg-6"><div class="card h-100"><div class="card-header">Especialidades</div><div class="card-body">
      <ul class="list-group mb-3" id="listaEsp">${esps.map((e) => `<li class="list-group-item">${esc(e.nome)}</li>`).join('')}</ul>
      <form id="fEsp" class="input-group" novalidate><input id="espNome" class="form-control" maxlength="80" placeholder="Nova especialidade" aria-label="Nova especialidade">
        <button class="btn btn-outline-primary" type="submit"><i class="bi bi-plus-lg me-1"></i>Adicionar</button></form></div></div></div></div>`;
  alvo.querySelector('#fCfg').addEventListener('submit', async (e) => {
    e.preventDefault();
    const v = (id) => alvo.querySelector('#' + id).value;
    try {
      await api.put('/configuracoes', { inicio: v('cfIni'), fim: v('cfFim'), pausaInicio: v('cfPI'), pausaFim: v('cfPF'), duracaoMinutos: Number(v('cfDur')) });
      toast('Configurações salvas com sucesso.');
    } catch (err) { erro(err); }
  });
  alvo.querySelector('#fEsp').addEventListener('submit', async (e) => {
    e.preventDefault();
    const nome = alvo.querySelector('#espNome').value.trim();
    if (!nome) return toast('Preencha todos os campos obrigatórios.', 'warning');
    try {
      const nova = await api.post('/especialidades', { nome });
      alvo.querySelector('#listaEsp').insertAdjacentHTML('beforeend', `<li class="list-group-item">${esc(nova.nome)}</li>`);
      alvo.querySelector('#espNome').value = '';
      toast('Especialidade cadastrada com sucesso.');
    } catch (err) { erro(err); }
  });
}
