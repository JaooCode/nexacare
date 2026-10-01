// Ponto de entrada do sistema: proteção de rotas no front, menu por perfil e roteador por hash (#/rota).
import { api, sessao } from './api.js';
import { aplicarTema, toast, erro, carregando, esc } from './ui.js';

const usuario = sessao.usuario;
if (!sessao.token || !usuario) {
  window.location.replace('/login.html');
  throw new Error('não autenticado');
}
aplicarTema(usuario.tema);

const ROTULO_PERFIL = { ADMINISTRADOR: 'Administrador', RECEPCIONISTA: 'Recepcionista', PROFISSIONAL: 'Profissional de saúde', PACIENTE: 'Paciente' };
const TODOS = ['ADMINISTRADOR', 'RECEPCIONISTA', 'PROFISSIONAL', 'PACIENTE'];
const EQUIPE = ['ADMINISTRADOR', 'RECEPCIONISTA', 'PROFISSIONAL'];

// Cada rota: título, ícone, perfis permitidos, módulo da tela e rótulo alternativo do menu por perfil.
const ROTAS = {
  dashboard: { titulo: 'Dashboard', icone: 'bi-speedometer2', perfis: TODOS, modulo: './views/dashboard.js', menu: { PACIENTE: 'Início' } },
  agenda: { titulo: 'Agenda', icone: 'bi-calendar3', perfis: EQUIPE, modulo: './views/agenda.js', menu: { PROFISSIONAL: 'Minha agenda' } },
  consultas: { titulo: 'Consultas', icone: 'bi-clipboard2-pulse', perfis: TODOS, modulo: './views/consultas.js', menu: { PACIENTE: 'Minhas consultas', PROFISSIONAL: 'Minhas consultas' } },
  consulta: { titulo: 'Detalhes da consulta', icone: 'bi-clipboard2-pulse', perfis: TODOS, modulo: './views/consulta.js', oculta: true },
  pacientes: { titulo: 'Pacientes', icone: 'bi-people', perfis: ['RECEPCIONISTA', 'PROFISSIONAL'], modulo: './views/pacientes.js', menu: { PROFISSIONAL: 'Meus pacientes' } },
  profissionais: { titulo: 'Profissionais', icone: 'bi-person-badge', perfis: ['ADMINISTRADOR', 'RECEPCIONISTA'], modulo: './views/profissionais.js' },
  usuarios: { titulo: 'Usuários e permissões', icone: 'bi-shield-lock', perfis: ['ADMINISTRADOR'], modulo: './views/usuarios.js', menu: { ADMINISTRADOR: 'Usuários' } },
  notificacoes: { titulo: 'Notificações e lembretes', icone: 'bi-bell', perfis: TODOS, modulo: './views/notificacoes.js', menu: { ADMINISTRADOR: 'Notificações' } },
  configuracoes: { titulo: 'Configurações', icone: 'bi-gear', perfis: TODOS, modulo: './views/configuracoes.js' },
};

const $ = (id) => document.getElementById(id);
const sidebar = $('sidebar');

function montarMenu() {
  $('menu').innerHTML = Object.entries(ROTAS).filter(([, r]) => !r.oculta && r.perfis.includes(usuario.perfil)).map(([chave, r]) =>
    `<a class="nav-link" href="#/${chave}" data-rota="${chave}"><i class="bi ${r.icone}"></i>${esc(r.menu?.[usuario.perfil] || r.titulo)}
      ${chave === 'notificacoes' ? '<span id="menuBadgeNotif" class="badge bg-danger rounded-pill d-none">0</span>' : ''}</a>`).join('');
  $('rodapeUsuario').innerHTML = `<div class="text-white fw-semibold text-truncate">${esc(usuario.nome)}</div><div>${esc(ROTULO_PERFIL[usuario.perfil])}</div>`;
  $('topoUsuario').innerHTML = `<i class="bi bi-person-circle me-1"></i>${esc(usuario.nome)} · ${esc(ROTULO_PERFIL[usuario.perfil])}`;
}

function analisarHash() {
  const bruto = location.hash.replace(/^#\/?/, '');
  const [caminho, qs] = bruto.split('?');
  const [rota, ...resto] = (caminho || 'dashboard').split('/');
  return { rota: rota || 'dashboard', parametros: resto, query: new URLSearchParams(qs || '') };
}

let geracao = 0;
async function navegar() {
  const { rota, parametros, query } = analisarHash();
  const def = ROTAS[rota];
  const view = $('view');
  sidebar.classList.remove('open');

  if (!def) { location.hash = '#/dashboard'; return; }
  if (!def.perfis.includes(usuario.perfil)) {
    toast('Você não possui permissão para realizar esta ação.', 'danger');
    location.hash = '#/dashboard';
    return;
  }
  const minha = ++geracao;
  document.title = `${def.titulo} · NexaCare`;
  $('tituloPagina').textContent = def.titulo;
  document.querySelectorAll('#menu .nav-link').forEach((a) => a.classList.toggle('active', a.dataset.rota === rota || (rota === 'consulta' && a.dataset.rota === 'consultas')));
  carregando(view);
  try {
    const modulo = await import(def.modulo);
    if (minha !== geracao) return;
    view.innerHTML = '';
    await modulo.renderizar(view, { usuario, parametros, query, atualizarBadges });
  } catch (e) {
    if (minha !== geracao) return;
    console.error(e);
    view.innerHTML = `<div class="alert alert-danger">${esc(e.message || 'Não foi possível carregar esta tela.')}</div>`;
  }
  view.focus({ preventScroll: true });
  window.scrollTo(0, 0);
}

export async function atualizarBadges() {
  try {
    const d = await api.get('/dashboard');
    const n = d.notificacoesNaoLidas;
    for (const id of ['badgeNotif', 'menuBadgeNotif']) {
      const el = $(id);
      if (!el) continue;
      el.textContent = n > 99 ? '99+' : n;
      el.classList.toggle('d-none', n === 0);
    }
  } catch { /* o badge é opcional */ }
}

$('btnMenu').addEventListener('click', () => sidebar.classList.toggle('open'));
$('backdrop').addEventListener('click', () => sidebar.classList.remove('open'));
$('btnSair').addEventListener('click', async () => {
  try { await api.post('/auth/logout'); } catch { /* sessão pode já ter expirado */ }
  sessao.limpar();
  window.location.href = '/login.html';
});
window.addEventListener('hashchange', navegar);

montarMenu();
atualizarBadges();
navegar();
