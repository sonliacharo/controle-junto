import { useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { encerrarSessao, lerSessao, salvarSessao } from './api.js';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Painel from './pages/Painel.jsx';
import Gastos from './pages/Gastos.jsx';
import Grupos from './pages/Grupos.jsx';
import GrupoDetalhe from './pages/GrupoDetalhe.jsx';

export default function App() {
  const [sessao, setSessao] = useState(lerSessao());

  const entrar = (dados) => {
    salvarSessao(dados);
    setSessao(dados);
  };
  const sair = () => {
    encerrarSessao();
    setSessao(null);
  };

  if (!sessao) {
    return (
      <Routes>
        <Route path="*" element={<Login onEntrar={entrar} />} />
      </Routes>
    );
  }

  return (
    <Layout usuario={sessao.usuario} onSair={sair}>
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
