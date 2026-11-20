// Comunicação com o back-end. Todas as rotas começam com /api.
const CHAVE_TOKEN = 'controlejunto.token';
const CHAVE_USUARIO = 'controlejunto.usuario';
const CHAVE_MOTIVO = 'controlejunto.motivoSaida';
const CHAVE_ATIVIDADE = 'controlejunto.ultimaAtividade';

/** Registra a última interação do usuário (compartilhada entre abas). */
export function registrarAtividade(momento = Date.now()) {
  try { localStorage.setItem(CHAVE_ATIVIDADE, String(momento)); } catch { /* ignora */ }
}

export function ultimaAtividade() {
  try { return Number(localStorage.getItem(CHAVE_ATIVIDADE)) || Date.now(); } catch { return Date.now(); }
}

export const CHAVES_SESSAO = { token: CHAVE_TOKEN };

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
    registrarAtividade();
  } catch {
    /* navegador sem armazenamento: a sessão dura até recarregar */
  }
}

export function encerrarSessao(motivo) {
  try {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
    if (motivo) sessionStorage.setItem(CHAVE_MOTIVO, motivo);
  } catch {
    /* ignora */
  }
}

/** Motivo do último encerramento de sessão, mostrado uma vez na tela de login. */
export function lerMotivoSaida() {
  try { return sessionStorage.getItem(CHAVE_MOTIVO); } catch { return null; }
}

export function limparMotivoSaida() {
  try { sessionStorage.removeItem(CHAVE_MOTIVO); } catch { /* ignora */ }
}

// RNF-005: tempo sem uso até a sessão expirar (o mesmo do back-end)
export const MINUTOS_INATIVIDADE = 30;

// Momento da última resposta autenticada (o back-end renova o token em cada uma)
let ultimaRenovacao = Date.now();
export const msDesdeUltimaRenovacao = () => Date.now() - ultimaRenovacao;

async function chamar(metodo, caminho, corpo) {
  const sessao = lerSessao();
  const resposta = await fetch(`/api${caminho}`, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(sessao ? { Authorization: `Bearer ${sessao.token}` } : {}),
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });

  // RNF-005: o back-end devolve um token novo a cada requisição; a sessão
  // só expira depois de 30 minutos sem uso.
  const renovado = resposta.headers.get('X-Token-Renovado');
  if (renovado && sessao) {
    try { localStorage.setItem(CHAVE_TOKEN, renovado); } catch { /* ignora */ }
    ultimaRenovacao = Date.now();
  }

  // Sessão expirada: volta para o login
  if (resposta.status === 401 && sessao) {
    encerrarSessao('expirada');
    window.location.replace('/login');
    throw new Error('Sua sessão expirou. Entre novamente.');
  }
  return resposta;
}

async function requisicao(metodo, caminho, corpo) {
  const resposta = await chamar(metodo, caminho, corpo);
  if (resposta.status === 204) return null;

  const dados = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    throw new Error(dados?.erro || 'Não foi possível concluir a operação.');
  }
  return dados;
}

/** Baixa um arquivo da API (ex.: relatório em PDF, RF-015). */
async function baixar(caminho, nomePadrao) {
  const resposta = await chamar('GET', caminho);
  if (!resposta.ok) {
    const dados = await resposta.json().catch(() => null);
    throw new Error(dados?.erro || 'Não foi possível gerar o arquivo.');
  }
  const disposicao = resposta.headers.get('Content-Disposition') || '';
  const nome = /filename="?([^";]+)"?/.exec(disposicao)?.[1] || nomePadrao;
  const url = URL.createObjectURL(await resposta.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export const api = {
  get: (c) => requisicao('GET', c),
  post: (c, b) => requisicao('POST', c, b ?? {}),
  put: (c, b) => requisicao('PUT', c, b),
  del: (c) => requisicao('DELETE', c),
  baixar,
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
