// Tela de login: validação, mostrar/ocultar senha, recuperação simulada, autocadastro de paciente.
import { api, sessao } from './api.js';
import { cpfValido, emailValido, telefoneValido, senhaForte, ativarMascaras } from './validacao.js';

const $ = (id) => document.getElementById(id);
const params = new URLSearchParams(location.search);

if (sessao.token && sessao.usuario) location.replace('/app.html#/dashboard');
if (params.get('expirada')) $('loginAviso').classList.remove('d-none');
if (params.get('aba') === 'cadastro') new bootstrap.Tab($('abaCadastro')).show();
ativarMascaras(document);

['input', 'change'].forEach((ev) => document.addEventListener(ev, (e) => e.target.classList?.remove('is-invalid')));

function marcar(campos) {
  let ok = true;
  campos.forEach(([id, valido]) => { $(id).classList.toggle('is-invalid', !valido); if (!valido) ok = false; });
  return ok;
}
function mostrarErro(id, mensagem) { const el = $(id); el.textContent = mensagem; el.classList.remove('d-none'); }

$('verSenha').addEventListener('click', () => {
  const campo = $('senha'), oculto = campo.type === 'password';
  campo.type = oculto ? 'text' : 'password';
  $('verSenha').innerHTML = `<i class="bi ${oculto ? 'bi-eye-slash' : 'bi-eye'}"></i>`;
});

document.querySelectorAll('[data-demo]').forEach((b) => b.addEventListener('click', () => {
  const [email, senha] = b.dataset.demo.split('|');
  $('email').value = email; $('senha').value = senha;
  $('email').classList.remove('is-invalid'); $('senha').classList.remove('is-invalid');
  $('btnEntrar').focus();
}));

$('formLogin').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('loginErro').classList.add('d-none');
  if (!marcar([['email', emailValido($('email').value)], ['senha', $('senha').value]])) return;
  $('btnEntrar').disabled = true;
  try {
    const r = await api.publico('POST', '/auth/login', { email: $('email').value.trim(), senha: $('senha').value });
    sessao.salvar(r.token, r.usuario);
    location.href = '/app.html#/dashboard'; // o dashboard e o menu mudam conforme o perfil
  } catch (err) {
    mostrarErro('loginErro', err.message);
    $('btnEntrar').disabled = false;
  }
});

$('esqueci').addEventListener('click', () => {
  $('senhaMsg').classList.add('d-none');
  $('emailRec').value = $('email').value;
  new bootstrap.Modal($('modalSenha')).show();
});
$('formSenha').addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!marcar([['emailRec', emailValido($('emailRec').value)]])) return;
  try {
    const r = await api.publico('POST', '/auth/recuperar-senha', { email: $('emailRec').value.trim() });
    $('senhaMsg').textContent = r.mensagem;
    $('senhaMsg').classList.remove('d-none');
  } catch (err) { $('senhaMsg').textContent = err.message; $('senhaMsg').classList.remove('d-none'); }
});

$('formCadastro').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('cadErro').classList.add('d-none'); $('cadOk').classList.add('d-none');
  const hoje = new Date().toISOString().slice(0, 10);
  const ok = marcar([['cNome', $('cNome').value.trim().length >= 3], ['cCpf', cpfValido($('cCpf').value)],
    ['cNasc', $('cNasc').value && $('cNasc').value < hoje && $('cNasc').value >= '1900-01-01'], ['cTel', telefoneValido($('cTel').value)],
    ['cEmail', emailValido($('cEmail').value)], ['cSenha', senhaForte($('cSenha').value)], ['cLgpd', $('cLgpd').checked]]);
  if (!ok) { mostrarErro('cadErro', 'Preencha todos os campos obrigatórios corretamente.'); return; }
  $('btnCadastro').disabled = true;
  try {
    await api.publico('POST', '/auth/registrar-paciente', { nome: $('cNome').value, cpf: $('cCpf').value, dataNascimento: $('cNasc').value,
      telefone: $('cTel').value, email: $('cEmail').value, senha: $('cSenha').value, aceiteLgpd: $('cLgpd').checked });
    $('cadOk').textContent = 'Paciente cadastrado com sucesso! Você já pode entrar com seu e-mail e senha.';
    $('cadOk').classList.remove('d-none');
    $('email').value = $('cEmail').value;
    e.target.reset();
    new bootstrap.Tab($('abaEntrar')).show();
    $('loginErro').classList.add('d-none');
  } catch (err) { mostrarErro('cadErro', err.message); }
  $('btnCadastro').disabled = false;
});
