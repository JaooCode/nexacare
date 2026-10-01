// Cliente da API REST. Guarda o token da sessão e traduz erros em mensagens amigáveis.
const CHAVE_TOKEN = 'nexacare.token';
const CHAVE_USUARIO = 'nexacare.usuario';

export const sessao = {
  get token() { return sessionStorage.getItem(CHAVE_TOKEN); },
  get usuario() {
    try { return JSON.parse(sessionStorage.getItem(CHAVE_USUARIO)); } catch { return null; }
  },
  salvar(token, usuario) {
    sessionStorage.setItem(CHAVE_TOKEN, token);
    sessionStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
  },
  atualizarUsuario(usuario) { sessionStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario)); },
  limpar() { sessionStorage.removeItem(CHAVE_TOKEN); sessionStorage.removeItem(CHAVE_USUARIO); },
};

export class ApiError extends Error {
  constructor(mensagem, status) { super(mensagem); this.status = status; }
}

async function requisitar(metodo, caminho, corpo, { publico = false } = {}) {
  const headers = {};
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';
  if (sessao.token && !publico) headers['Authorization'] = 'Bearer ' + sessao.token;

  let resposta;
  try {
    resposta = await fetch('/api' + caminho, {
      method: metodo, headers, body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    });
  } catch {
    throw new ApiError('Não foi possível conectar ao servidor. Verifique se o NexaCare está em execução.', 0);
  }

  let dados = null;
  const texto = await resposta.text();
  if (texto) { try { dados = JSON.parse(texto); } catch { /* resposta sem JSON */ } }

  if (!resposta.ok) {
    if (resposta.status === 401 && !publico) {
      sessao.limpar();
      window.location.href = '/login.html?expirada=1';
    }
    throw new ApiError(dados?.mensagem || 'Não foi possível concluir a operação.', resposta.status);
  }
  return dados;
}

export const api = {
  get: (c) => requisitar('GET', c),
  post: (c, corpo = {}) => requisitar('POST', c, corpo),
  put: (c, corpo) => requisitar('PUT', c, corpo),
  patch: (c, corpo = {}) => requisitar('PATCH', c, corpo),
  del: (c) => requisitar('DELETE', c),
  publico: (metodo, c, corpo) => requisitar(metodo, c, corpo, { publico: true }),
};

export function query(parametros) {
  const p = new URLSearchParams();
  Object.entries(parametros).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') p.set(k, v); });
  const s = p.toString();
  return s ? '?' + s : '';
}
