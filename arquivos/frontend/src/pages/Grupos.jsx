import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, dataBR, reais } from '../api.js';
import Icone from '../components/Icone.jsx';

/** Lista dos grupos do usuário e criação de grupo (RF-005). */
export default function Grupos() {
  const [grupos, setGrupos] = useState([]);
  const [nome, setNome] = useState('');
  const [erro, setErro] = useState('');
  const navegar = useNavigate();

  useEffect(() => { api.get('/grupos').then(setGrupos).catch((e) => setErro(e.message)); }, []);

  const criar = async (e) => {
    e.preventDefault();
    try {
      const grupo = await api.post('/grupos', { nome });
      navegar(`/grupos/${grupo.id}`);
    } catch (err) {
      setErro(err.message);
    }
  };

  const ativos = grupos.filter((g) => g.status === 'ATIVO');
  const encerrados = grupos.filter((g) => g.status !== 'ATIVO');

  const Cartao = ({ g }) => {
    const saldo = Number(g.meuSaldo);
    return (
      <Link to={`/grupos/${g.id}`} className={`cartao grupo-cartao ${g.status !== 'ATIVO' ? 'encerrado' : ''}`}>
        <div className="grupo-topo">
          <h3>{g.nome}</h3>
          {g.souAdministrador && <span className="etiqueta">Administrador</span>}
          {g.status !== 'ATIVO' && <span className="etiqueta cinza">Encerrado</span>}
        </div>
        <p className="detalhe">{g.participantes} participante{g.participantes > 1 ? 's' : ''} · criado em {dataBR(g.dataCriacao)}</p>
        <div className={`grupo-saldo ${saldo > 0 ? 'positivo' : saldo < 0 ? 'negativo' : ''}`}>
          {saldo > 0 && <>Você tem a receber <b>{reais(saldo)}</b></>}
          {saldo < 0 && <>Você deve <b>{reais(-saldo)}</b></>}
          {saldo === 0 && <>Você está quite</>}
        </div>
      </Link>
    );
  };

  return (
    <>
      <div className="cabecalho-pagina">
        <div>
          <h1>Grupos</h1>
          <p className="subtitulo">Viagens, república, eventos: divida as despesas com quem participou.</p>
        </div>
      </div>

      <section className="cartao">
        <h2>Novo grupo</h2>
        <form className="linha-form" onSubmit={criar}>
          <input value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Ex.: Viagem Chapada 2026"
            required maxLength={100} aria-label="Nome do grupo" />
          <button className="botao primario"><Icone nome="mais" /> Criar grupo</button>
        </form>
        {erro && <p className="erro">{erro}</p>}
      </section>

      <h2 className="secao">Grupos ativos</h2>
      {ativos.length === 0 ? <p className="vazio">Você ainda não participa de nenhum grupo ativo.</p> : (
        <div className="grade-grupos">{ativos.map((g) => <Cartao key={g.id} g={g} />)}</div>
      )}

      {encerrados.length > 0 && (
        <>
          <h2 className="secao">Histórico (encerrados)</h2>
          <div className="grade-grupos">{encerrados.map((g) => <Cartao key={g.id} g={g} />)}</div>
        </>
      )}
    </>
  );
}
