// Comunicação com o back-end. Todas as rotas começam com /api.
const CHAVE_TOKEN = 'controlejunto.token';
const CHAVE_USUARIO = 'controlejunto.usuario';

export function lerSessao() {
  try {
    const token = localStorage.getItem(CHAVE_TOKEN);
    const usuario = JSON.parse(localStorage.getItem(CHAVE_USUARIO) || 'null');
    return token && usuario ? { token, usuario } : null;
  } catch {
    return null;
  }
}

export function salvarSessao({ token, usuario }) {
  try {
    localStorage.setItem(CHAVE_TOKEN, token);
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
  } catch {
    /* navegador sem armazenamento: a sessão dura até recarregar */
  }
}

export function encerrarSessao() {
  try {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
  } catch {
    /* ignora */
  }
}

async function requisicao(metodo, caminho, corpo) {
  const sessao = lerSessao();
  const resposta = await fetch(`/api${caminho}`, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(sessao ? { Authorization: `Bearer ${sessao.token}` } : {}),
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });

  // Sessão expirada (RNF-005): volta para o login
  if (resposta.status === 401) {
    encerrarSessao();
    window.location.href = '/login';
    throw new Error('Sua sessão expirou. Entre novamente.');
  }
  if (resposta.status === 204) return null;

  const dados = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    throw new Error(dados?.erro || 'Não foi possível concluir a operação.');
  }
  return dados;
}

export const api = {
  get: (c) => requisicao('GET', c),
  post: (c, b) => requisicao('POST', c, b ?? {}),
  put: (c, b) => requisicao('PUT', c, b),
  del: (c) => requisicao('DELETE', c),
};

export const reais = (valor) =>
  Number(valor || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export const dataBR = (iso) => (iso ? new Date(`${iso}T12:00:00`).toLocaleDateString('pt-BR') : '');

export const MESES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho',
  'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];

export const hojeISO = () => {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};
