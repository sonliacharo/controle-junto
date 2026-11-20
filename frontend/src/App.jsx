import { useEffect, useRef, useState } from 'react';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import {
  api, CHAVES_SESSAO, encerrarSessao, lerSessao, MINUTOS_INATIVIDADE, msDesdeUltimaRenovacao,
  registrarAtividade, salvarSessao, ultimaAtividade,
} from './api.js';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Painel from './pages/Painel.jsx';
import Gastos from './pages/Gastos.jsx';
import Grupos from './pages/Grupos.jsx';
import GrupoDetalhe from './pages/GrupoDetalhe.jsx';

const LIMITE_INATIVIDADE = MINUTOS_INATIVIDADE * 60 * 1000;
const INTERVALO_RENOVACAO = 5 * 60 * 1000; // renova o token a cada 5 min enquanto há uso
const EVENTOS_DE_USO = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'wheel'];

/**
 * RNF-005: encerra a sessão após 30 minutos sem interação. Enquanto o usuário
 * está usando a tela (mesmo sem trocar de página), o token é renovado.
 */
function useSessaoPorInatividade(ativa, onExpirar) {
  const expirar = useRef(onExpirar);
  expirar.current = onExpirar;

  useEffect(() => {
    if (!ativa) return undefined;
    let ultimaGravacao = 0;
    let usouDesdeRenovacao = false;

    const marcarUso = () => {
      usouDesdeRenovacao = true;
      const agora = Date.now();
      if (agora - ultimaGravacao > 5000) { // grava no máximo a cada 5 s
        ultimaGravacao = agora;
        registrarAtividade(agora);
      }
    };

    const verificar = () => {
      if (Date.now() - ultimaAtividade() >= LIMITE_INATIVIDADE) {
        expirar.current();
      } else if (usouDesdeRenovacao && msDesdeUltimaRenovacao() >= INTERVALO_RENOVACAO) {
        usouDesdeRenovacao = false;
        api.get('/auth/me').catch(() => {});
      }
    };

    verificar();
    EVENTOS_DE_USO.forEach((e) => window.addEventListener(e, marcarUso, { passive: true }));
    const timer = setInterval(verificar, 15 * 1000);
    document.addEventListener('visibilitychange', verificar);
    return () => {
      EVENTOS_DE_USO.forEach((e) => window.removeEventListener(e, marcarUso));
      clearInterval(timer);
      document.removeEventListener('visibilitychange', verificar);
    };
  }, [ativa]);
}

export default function App() {
  const [sessao, setSessao] = useState(lerSessao());
  const navegar = useNavigate();

  const entrar = (dados) => {
    salvarSessao(dados);
    setSessao(dados);
    navegar('/', { replace: true }); // sempre começa pelo painel
  };

  const sair = (motivo) => {
    encerrarSessao(motivo);
    setSessao(null);
    navegar('/login', { replace: true });
  };

  useSessaoPorInatividade(Boolean(sessao), () => sair('inatividade'));

  // Saiu em outra aba (ou a sessão expirou lá): sai aqui também
  useEffect(() => {
    const aoMudar = (e) => {
      if ((e.key === CHAVES_SESSAO.token || e.key === null) && !lerSessao()) {
        setSessao(null);
        navegar('/login', { replace: true });
      }
    };
    window.addEventListener('storage', aoMudar);
    return () => window.removeEventListener('storage', aoMudar);
  }, [navegar]);

  if (!sessao) {
    return (
      <Routes>
        <Route path="/login" element={<Login onEntrar={entrar} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <Layout usuario={sessao.usuario} onSair={() => sair()}>
      <Routes>
        <Route path="/" element={<Painel />} />
        <Route path="/gastos" element={<Gastos />} />
        <Route path="/grupos" element={<Grupos />} />
        <Route path="/grupos/:id" element={<GrupoDetalhe />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
