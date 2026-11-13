import { useEffect, useRef, useState } from 'react';
import { api, dataBR, hojeISO, reais } from '../api.js';
import SeletorMes from '../components/SeletorMes.jsx';
import Icone from '../components/Icone.jsx';

const VAZIO = { descricao: '', valor: '', data: hojeISO(), categoriaId: '', comentario: '' };

/**
 * Gastos pessoais: registrar (RF-003), editar/excluir (RF-004), filtrar (RF-014),
 * comentário (RF-016) e categoria sugerida pela IA (RF-010, RN-005).
 */
export default function Gastos() {
  const hoje = new Date();
  const [mes, setMes] = useState(hoje.getMonth() + 1);
  const [ano, setAno] = useState(hoje.getFullYear());
  const [filtroCategoria, setFiltroCategoria] = useState('');
  const [categorias, setCategorias] = useState([]);
  const [gastos, setGastos] = useState([]);
  const [form, setForm] = useState(VAZIO);
  const [editandoId, setEditandoId] = useState(null);
  const [sugestao, setSugestao] = useState(null);
  const [buscandoIA, setBuscandoIA] = useState(false);
  const [erro, setErro] = useState('');
  const temporizador = useRef(null);

  useEffect(() => { api.get('/categorias').then(setCategorias); }, []);

  const carregar = () => {
    const filtro = filtroCategoria ? `&categoriaId=${filtroCategoria}` : '';
    api.get(`/gastos?mes=${mes}&ano=${ano}${filtro}`).then(setGastos).catch((e) => setErro(e.message));
  };
  useEffect(carregar, [mes, ano, filtroCategoria]);

  // Pede a sugestão da IA quando o usuário para de digitar a descrição
  const mudarDescricao = (texto) => {
    setForm((f) => ({ ...f, descricao: texto }));
    clearTimeout(temporizador.current);
    if (texto.trim().length < 3 || editandoId) return;
    temporizador.current = setTimeout(async () => {
      setBuscandoIA(true);
      try {
        const s = await api.post('/categorias/sugestao', { descricao: texto });
        setSugestao(s);
        setForm((f) => ({ ...f, categoriaId: String(s.categoria.id) }));
      } catch {
        setSugestao(null); // RNF-015: sem sugestão, o usuário escolhe manualmente
      } finally {
        setBuscandoIA(false);
      }
    }, 600);
  };

  const salvar = async (e) => {
    e.preventDefault();
    setErro('');
    const corpo = { ...form, valor: Number(form.valor), categoriaId: Number(form.categoriaId) };
    try {
      if (editandoId) await api.put(`/gastos/${editandoId}`, corpo);
      else await api.post('/gastos', corpo);
      cancelar();
      carregar();
    } catch (err) {
      setErro(err.message);
    }
  };

  const editar = (g) => {
    setEditandoId(g.id);
    setSugestao(null);
    setForm({ descricao: g.descricao, valor: g.valor, data: g.data, categoriaId: String(g.categoria.id), comentario: g.comentario || '' });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const excluir = async (g) => {
    if (!window.confirm(`Excluir o gasto "${g.descricao}"?`)) return;
    await api.del(`/gastos/${g.id}`);
    carregar();
  };

  const cancelar = () => { setForm({ ...VAZIO, data: hojeISO() }); setEditandoId(null); setSugestao(null); };

  const total = gastos.reduce((s, g) => s + Number(g.valor), 0);
  const sugeridaAlterada = sugestao && form.categoriaId !== String(sugestao.categoria.id);

  return (
    <>
      <div className="cabecalho-pagina">
        <div>
          <h1>Meus gastos</h1>
          <p className="subtitulo">Registre seus gastos do dia a dia.</p>
        </div>
        <SeletorMes mes={mes} ano={ano} onChange={(m, a) => { setMes(m); setAno(a); }} />
      </div>

      <section className="cartao">
        <h2>{editandoId ? 'Editar gasto' : 'Novo gasto'}</h2>
        <form className="form-grade" onSubmit={salvar}>
          <label className="col-2">Descrição
            <input value={form.descricao} onChange={(e) => mudarDescricao(e.target.value)}
              placeholder="Ex.: Uber aeroporto" required maxLength={200} />
          </label>
          <label>Valor (R$)
            <input type="number" min="0.01" step="0.01" value={form.valor}
              onChange={(e) => setForm({ ...form, valor: e.target.value })} required />
          </label>
          <label>Data
            <input type="date" value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} required />
          </label>
          <label className="col-2">Categoria
            <select value={form.categoriaId} onChange={(e) => setForm({ ...form, categoriaId: e.target.value })} required>
              <option value="">Selecione…</option>
              {categorias.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
            {buscandoIA && <span className="selo-ia carregando"><Icone nome="ia" tamanho={14} /> Consultando a IA…</span>}
            {!buscandoIA && sugestao && (
              <span className={`selo-ia ${sugeridaAlterada ? 'alterado' : ''}`}>
                <Icone nome="ia" tamanho={14} />
                {sugeridaAlterada
                  ? `A IA sugeriu "${sugestao.categoria.nome}", você escolheu outra`
                  : `Sugerido ${sugestao.origem === 'IA' ? 'pela IA' : 'automaticamente'}: ${sugestao.categoria.nome}`}
              </span>
            )}
          </label>
          <label className="col-2">Comentário (opcional)
            <input value={form.comentario} onChange={(e) => setForm({ ...form, comentario: e.target.value })}
              placeholder="Ex.: dividido com a família" maxLength={300} />
          </label>
          {erro && <p className="erro col-4">{erro}</p>}
          <div className="acoes col-4">
            {editandoId && <button type="button" className="botao" onClick={cancelar}>Cancelar</button>}
            <button className="botao primario"><Icone nome={editandoId ? 'check' : 'mais'} /> {editandoId ? 'Salvar alterações' : 'Registrar gasto'}</button>
          </div>
        </form>
      </section>

      <section className="cartao">
        <div className="cartao-cabecalho">
          <h2>Gastos do mês <span className="total-lista">{reais(total)}</span></h2>
          <select className="filtro" value={filtroCategoria} onChange={(e) => setFiltroCategoria(e.target.value)} aria-label="Filtrar por categoria">
            <option value="">Todas as categorias</option>
            {categorias.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select>
        </div>
        {gastos.length === 0 ? <p className="vazio">Nenhum gasto encontrado.</p> : (
          <table className="tabela">
            <thead>
              <tr><th>Data</th><th>Descrição</th><th>Categoria</th><th className="direita">Valor</th><th /></tr>
            </thead>
            <tbody>
              {gastos.map((g) => (
                <tr key={g.id}>
                  <td>{dataBR(g.data)}</td>
                  <td>{g.descricao}{g.comentario && <small className="comentario">{g.comentario}</small>}</td>
                  <td><span className="etiqueta">{g.categoria.nome}</span></td>
                  <td className="direita valor">{reais(g.valor)}</td>
                  <td className="direita acoes-linha">
                    <button className="botao-icone" onClick={() => editar(g)} aria-label="Editar"><Icone nome="editar" /></button>
                    <button className="botao-icone" onClick={() => excluir(g)} aria-label="Excluir"><Icone nome="lixo" /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
