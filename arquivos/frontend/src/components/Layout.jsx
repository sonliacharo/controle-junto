import { useEffect, useRef, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { api } from '../api.js';
import Icone from './Icone.jsx';

function Notificacoes() {
  const [aberto, setAberto] = useState(false);
  const [itens, setItens] = useState([]);
  const ref = useRef(null);

  const carregar = () => api.get('/notificacoes').then(setItens).catch(() => {});

  useEffect(() => {
    carregar();
    const fechar = (e) => ref.current && !ref.current.contains(e.target) && setAberto(false);
    document.addEventListener('mousedown', fechar);
    return () => document.removeEventListener('mousedown', fechar);
  }, []);

  const naoLidas = itens.filter((n) => !n.lida).length;

  const marcar = async (n) => {
    if (n.lida) return;
    await api.post(`/notificacoes/${n.id}/lida`);
    carregar();
  };

  return (
    <div className="notif" ref={ref}>
      <button className="botao-icone" aria-label="Notificações"
        onClick={() => { setAberto(!aberto); if (!aberto) carregar(); }}>
        <Icone nome="sino" />
        {naoLidas > 0 && <span className="notif-contador">{naoLidas}</span>}
      </button>
      {aberto && (
        <div className="notif-painel">
          <div className="notif-titulo">Notificações</div>
          {itens.length === 0 && <p className="vazio">Nenhuma notificação.</p>}
          {itens.map((n) => (
            <button key={n.id} className={`notif-item ${n.lida ? '' : 'nova'}`} onClick={() => marcar(n)}>
              <span>{n.mensagem}</span>
              <small>{n.dataHora}</small>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function Layout({ usuario, onSair, children }) {
  const iniciais = usuario.nome.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase();
  return (
    <div className="app">
      <aside className="menu">
        <div className="marca">
          <span className="marca-logo" aria-hidden="true" />
          <span>ControleJunto</span>
        </div>
        <nav>
          <NavLink to="/" end><Icone nome="painel" /> Painel</NavLink>
          <NavLink to="/gastos"><Icone nome="gastos" /> Meus gastos</NavLink>
          <NavLink to="/grupos"><Icone nome="grupos" /> Grupos</NavLink>
        </nav>
        <button className="menu-sair" onClick={onSair}><Icone nome="sair" /> Sair</button>
      </aside>
      <div className="principal">
        <header className="topo">
          <div />
          <div className="topo-direita">
            <Notificacoes />
            <div className="usuario">
              <span className="avatar">{iniciais}</span>
              <span className="usuario-nome">{usuario.nome}</span>
            </div>
          </div>
        </header>
        <main className="conteudo">{children}</main>
      </div>
    </div>
  );
}
