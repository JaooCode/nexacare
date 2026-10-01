// Utilitários de interface: toasts, confirmação, modais, formatação e paginação.
export function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const DIAS = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado'];

export const hojeISO = () => paraISO(new Date());
export function paraISO(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
export function dataDeISO(iso) { const [a, m, d] = iso.split('-').map(Number); return new Date(a, m - 1, d); }
export function somarDias(iso, n) { const d = dataDeISO(iso); d.setDate(d.getDate() + n); return paraISO(d); }
export const fmtData = (iso) => (iso ? iso.split('-').reverse().join('/') : '—');
export const fmtHora = (h) => (h ? h.slice(0, 5) : '—');
export const diaSemana = (iso) => DIAS[dataDeISO(iso).getDay()];
export function fmtDataHora(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}
export function idade(iso) {
  const n = dataDeISO(iso), h = new Date();
  let a = h.getFullYear() - n.getFullYear();
  if (h.getMonth() < n.getMonth() || (h.getMonth() === n.getMonth() && h.getDate() < n.getDate())) a--;
  return a;
}

export function badgeStatus(status, rotulo) {
  return `<span class="badge badge-status st-${esc(status)}">${esc(rotulo || status)}</span>`;
}

export function estadoVazio(icone, texto) {
  return `<div class="tabela-vazia"><i class="bi ${icone}"></i>${esc(texto)}</div>`;
}

// ---------- Toasts ----------
export function toast(mensagem, tipo = 'success') {
  let area = document.getElementById('toasts');
  if (!area) {
    area = document.createElement('div');
    area.id = 'toasts';
    area.className = 'toast-container position-fixed top-0 end-0 p-3';
    area.style.zIndex = 2000;
    document.body.appendChild(area);
  }
  const icone = { success: 'bi-check-circle-fill', danger: 'bi-exclamation-triangle-fill', warning: 'bi-exclamation-circle-fill', info: 'bi-info-circle-fill' }[tipo];
  const el = document.createElement('div');
  el.className = `toast align-items-center text-bg-${tipo} border-0`;
  el.setAttribute('role', tipo === 'danger' ? 'alert' : 'status');
  el.innerHTML = `<div class="d-flex"><div class="toast-body"><i class="bi ${icone} me-2"></i>${esc(mensagem)}</div>
    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Fechar"></button></div>`;
  area.appendChild(el);
  const t = new bootstrap.Toast(el, { delay: tipo === 'danger' ? 6000 : 3500 });
  el.addEventListener('hidden.bs.toast', () => el.remove());
  t.show();
}
export const erro = (e) => toast(e?.message || 'Não foi possível concluir a operação.', 'danger');

// ---------- Modais ----------
/** Cria um modal Bootstrap. `corpo` e `rodape` são HTML. Retorna { el, modal, fechar, corpo, rodape }. */
export function abrirModal({ titulo, corpo, rodape = '', tamanho = '', onFechar }) {
  const el = document.createElement('div');
  el.className = 'modal fade';
  el.tabIndex = -1;
  el.innerHTML = `<div class="modal-dialog modal-dialog-scrollable modal-dialog-centered ${tamanho}">
    <div class="modal-content">
      <div class="modal-header"><h2 class="modal-title fs-5">${titulo}</h2>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button></div>
      <div class="modal-body">${corpo}</div>
      ${rodape ? `<div class="modal-footer">${rodape}</div>` : ''}
    </div></div>`;
  document.body.appendChild(el);
  const modal = new bootstrap.Modal(el);
  el.addEventListener('hidden.bs.modal', () => { el.remove(); onFechar?.(); });
  // Links com data-bs-dismiss não navegam (o Bootstrap cancela o clique); por isso usamos data-fechar.
  el.addEventListener('click', (e) => { if (e.target.closest('a[data-fechar]')) modal.hide(); });
  ['input', 'change'].forEach((ev) => el.addEventListener(ev, (e) => e.target.classList?.remove('is-invalid')));
  modal.show();
  return { el, modal, fechar: () => modal.hide(), corpo: el.querySelector('.modal-body'), rodape: el.querySelector('.modal-footer') };
}

/** Pede confirmação antes de ações destrutivas/irreversíveis. Resolve true/false. */
export function confirmar({ titulo = 'Confirmar ação', mensagem, textoConfirmar = 'Confirmar', perigo = true }) {
  return new Promise((resolve) => {
    let decidido = false;
    const m = abrirModal({
      titulo: `<i class="bi bi-exclamation-triangle-fill text-${perigo ? 'danger' : 'primary'} me-2"></i>${esc(titulo)}`,
      corpo: `<p class="mb-0">${mensagem}</p>`,
      rodape: `<button class="btn btn-outline-secondary" data-acao="nao">Voltar</button>
               <button class="btn btn-${perigo ? 'danger' : 'primary'}" data-acao="sim">${esc(textoConfirmar)}</button>`,
      onFechar: () => { if (!decidido) resolve(false); },
    });
    m.el.querySelector('[data-acao="nao"]').onclick = () => m.fechar();
    m.el.querySelector('[data-acao="sim"]').onclick = () => { decidido = true; resolve(true); m.fechar(); };
  });
}

// ---------- Paginação (no navegador) ----------
export function paginar(itens, pagina, porPagina = 10) {
  const total = Math.max(1, Math.ceil(itens.length / porPagina));
  const p = Math.min(Math.max(1, pagina), total);
  return { itens: itens.slice((p - 1) * porPagina, p * porPagina), pagina: p, total };
}

export function htmlPaginacao({ pagina, total }, quantidade) {
  if (total <= 1) return `<div class="small text-secondary">${quantidade} registro(s)</div>`;
  const bt = (p, rot, ativo, off) => `<li class="page-item ${ativo ? 'active' : ''} ${off ? 'disabled' : ''}">
    <button class="page-link" data-pag="${p}">${rot}</button></li>`;
  let paginas = '';
  for (let i = 1; i <= total; i++) paginas += bt(i, i, i === pagina, false);
  return `<div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
    <div class="small text-secondary">${quantidade} registro(s) · página ${pagina} de ${total}</div>
    <nav aria-label="Paginação"><ul class="pagination pagination-sm mb-0">
      ${bt(pagina - 1, '&laquo;', false, pagina === 1)}${paginas}${bt(pagina + 1, '&raquo;', false, pagina === total)}
    </ul></nav></div>`;
}

export function debounce(fn, ms = 300) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

export function carregando(container) {
  container.innerHTML = `<div class="text-center text-secondary py-5"><div class="spinner-border spinner-border-sm me-2" role="status"></div>Carregando...</div>`;
}

export function aplicarTema(tema) {
  document.documentElement.setAttribute('data-bs-theme', tema === 'escuro' ? 'dark' : 'light');
}
