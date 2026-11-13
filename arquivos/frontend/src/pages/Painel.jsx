import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, dataBR, MESES, reais } from '../api.js';
import SeletorMes from '../components/SeletorMes.jsx';
import Icone from '../components/Icone.jsx';

const VERDE = '#21C25E';
const VERDE_CLARO = '#BDEFCF';

/** RF-011 (dashboard), RF-013 (orçamento) e RN-004 (alerta de 90%). */
export default function Painel() {
  const hoje = new Date();
  const [mes, setMes] = useState(hoje.getMonth() + 1);
  const [ano, setAno] = useState(hoje.getFullYear());
  const [dados, setDados] = useState(null);
  const [ultimos, setUltimos] = useState([]);
  const [editandoOrcamento, setEditandoOrcamento] = useState(false);
  const [valorOrcamento, setValorOrcamento] = useState('');
  const [erro, setErro] = useState('');

  const carregar = () => {
    api.get(`/dashboard?mes=${mes}&ano=${ano}`).then(setDados).catch((e) => setErro(e.message));
    api.get(`/gastos?mes=${mes}&ano=${ano}`).then((g) => setUltimos(g.slice(0, 5))).catch(() => setUltimos([]));
  };
  useEffect(carregar, [mes, ano]);

  const salvarOrcamento = async (e) => {
    e.preventDefault();
    try {
      await api.put('/orcamento', { mes, ano, valorLimite: Number(valorOrcamento) });
      setEditandoOrcamento(false);
      carregar();
    } catch (err) {
      setErro(err.message);
    }
  };

  if (!dados) return <p className="vazio">{erro || 'Carregando…'}</p>;

  const orc = dados.orcamento;
  const total = Number(dados.totalMes);
  const limite = orc.valorLimite ? Number(orc.valorLimite) : null;
  const restante = limite !== null ? limite - total : null;

  // Comparação com o mês anterior
  const meses = dados.ultimosMeses.map((m) => ({
    nome: MESES[m.mes - 1].slice(0, 3),
    total: Number(m.total),
    atual: m.mes === mes && m.ano === ano,
  }));
  const anterior = meses.length > 1 ? meses[meses.length - 2].total : 0;
  const variacao = anterior > 0 ? ((total - anterior) / anterior) * 100 : null;

  // Categorias em ordem decrescente, com o percentual de cada uma no mês
  const categorias = dados.porCategoria.map((c) => ({ nome: c.categoria, total: Number(c.total) }));
  const maior = categorias.length ? categorias[0].total : 0;

  return (
    <>
      <div className="cabecalho-pagina">
        <div>
          <h1>Painel</h1>
          <p className="subtitulo">Resumo dos seus gastos pessoais e dos grupos.</p>
        </div>
        <SeletorMes mes={mes} ano={ano} onChange={(m, a) => { setMes(m); setAno(a); }} />
      </div>

      {orc.alerta && (
        <div className="aviso alerta">
          <Icone nome="alerta" />
          <span>Atenção: você já usou <b>{orc.percentualUsado}%</b> do orçamento de {MESES[mes - 1].toLowerCase()}.</span>
        </div>
      )}

      <div className="indicadores">
        <div className="cartao indicador">
          <span className="rotulo">Gasto no mês</span>
          <strong>{reais(total)}</strong>
          {variacao !== null && (
            <span className={`detalhe ${variacao > 0 ? 'negativo' : 'positivo'}`}>
              {variacao > 0 ? '▲' : '▼'} {Math.abs(variacao).toFixed(0)}% em relação a {MESES[(mes + 10) % 12].toLowerCase()}
            </span>
          )}
        </div>

        <div className="cartao indicador">
          <span className="rotulo">Orçamento do mês</span>
          {editandoOrcamento || limite === null ? (
            <form className="orcamento-form" onSubmit={salvarOrcamento}>
              <input type="number" min="1" step="0.01" placeholder="Ex.: 1500" value={valorOrcamento}
                onChange={(e) => setValorOrcamento(e.target.value)} required aria-label="Valor do orçamento" />
              <button className="botao primario pequeno">Salvar</button>
            </form>
          ) : (
            <>
              <strong className={restante < 0 ? 'negativo' : ''}>
                {restante >= 0 ? `Restam ${reais(restante)}` : `${reais(-restante)} acima`}
              </strong>
              <div className="barra" aria-label={`${orc.percentualUsado}% usado`}>
                <div className={`barra-preenchida ${orc.alerta ? 'perigo' : ''}`} style={{ width: `${Math.min(orc.percentualUsado, 100)}%` }} />
              </div>
              <span className="detalhe">{orc.percentualUsado}% de {reais(limite)} ·{' '}
                <button className="link" onClick={() => { setValorOrcamento(limite); setEditandoOrcamento(true); }}>alterar</button>
              </span>
            </>
          )}
        </div>

        <Link to="/grupos" className="cartao indicador indicador-link">
          <span className="rotulo">Você deve nos grupos</span>
          <strong className={Number(dados.totalAPagarGrupos) > 0 ? 'negativo' : ''}>{reais(dados.totalAPagarGrupos)}</strong>
          <span className="detalhe">Ver grupos ›</span>
        </Link>

        <Link to="/grupos" className="cartao indicador indicador-link">
          <span className="rotulo">Você tem a receber</span>
          <strong className={Number(dados.totalAReceberGrupos) > 0 ? 'positivo' : ''}>{reais(dados.totalAReceberGrupos)}</strong>
          <span className="detalhe">Ver grupos ›</span>
        </Link>
      </div>
      {erro && <p className="erro">{erro}</p>}

      <div className="graficos">
        <section className="cartao">
          <div className="cartao-cabecalho">
            <h2>Gastos por mês</h2>
            {limite !== null && <span className="legenda-linha">Orçamento de {MESES[mes - 1].toLowerCase()}</span>}
          </div>
          <ResponsiveContainer width="100%" height={270}>
            <BarChart data={meses} margin={{ top: 10, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
              <XAxis dataKey="nome" tickLine={false} axisLine={false} />
              <YAxis tickLine={false} axisLine={false} width={72}
                tickFormatter={(v) => v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })} />
              <Tooltip formatter={(v) => [reais(v), 'Total']} cursor={{ fill: '#F1F5F9' }} />
              {limite !== null && <ReferenceLine y={limite} stroke="#64748B" strokeDasharray="5 4" />}
              <Bar dataKey="total" radius={[6, 6, 0, 0]} maxBarSize={48}>
                {meses.map((m) => <Cell key={m.nome} fill={m.atual ? VERDE : VERDE_CLARO} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </section>

        <section className="cartao">
          <div className="cartao-cabecalho">
            <h2>Para onde foi o dinheiro</h2>
            <span className="detalhe">{MESES[mes - 1]}</span>
          </div>
          {categorias.length === 0 ? <p className="vazio">Nenhum gasto neste mês.</p> : (
            <ul className="ranking">
              {categorias.map((c) => (
                <li key={c.nome}>
                  <div className="ranking-texto">
                    <span>{c.nome}</span>
                    <span><b>{reais(c.total)}</b><small>{total > 0 ? Math.round((c.total / total) * 100) : 0}%</small></span>
                  </div>
                  <div className="ranking-barra"><div style={{ width: `${(c.total / maior) * 100}%` }} /></div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="cartao">
        <div className="cartao-cabecalho">
          <h2>Últimos gastos</h2>
          <Link to="/gastos" className="link">Ver todos ›</Link>
        </div>
        {ultimos.length === 0 ? <p className="vazio">Nenhum gasto neste mês.</p> : (
          <ul className="ultimos">
            {ultimos.map((g) => (
              <li key={g.id}>
                <div>
                  <span>{g.descricao}</span>
                  <small>{dataBR(g.data)}</small>
                </div>
                <span className="etiqueta cinza">{g.categoria.nome}</span>
                <span className="valor">{reais(g.valor)}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
