import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, dataBR, hojeISO, reais } from '../api.js';
import Icone from '../components/Icone.jsx';

/**
 * Detalhe do grupo: participantes (RF-006), nova despesa (RF-007),
 * saldos (RF-008), quitação (RF-009) e encerramento/exclusão (RN-003, RN-007).
 */
export default function GrupoDetalhe() {
  const { id } = useParams();
  const navegar = useNavigate();
  const [grupo, setGrupo] = useState(null);
  const [erro, setErro] = useState('');
  const [aviso, setAviso] = useState('');

  const carregar = () => api.get(`/grupos/${id}`).then(setGrupo).catch((e) => setErro(e.message));
  useEffect(() => { carregar(); }, [id]);

  const executar = async (acao, mensagem) => {
    setErro('');
    setAviso('');
    try {
      const atualizado = await acao();
      if (atualizado) setGrupo(atualizado);
      if (mensagem) setAviso(mensagem);
      return true;
    } catch (err) {
      setErro(err.message);
      return false;
    }
  };

  if (!grupo) return <p className="vazio">{erro || 'Carregando…'}</p>;

  const ativo = grupo.status === 'ATIVO';
  const pendentes = grupo.dividas.filter((d) => d.status === 'PENDENTE');
  const quitadas = grupo.dividas.filter((d) => d.status !== 'PENDENTE');

  const encerrar = () => executar(() => api.post(`/grupos/${id}/encerrar`), 'Grupo encerrado. Ele continua disponível no histórico.');
  const excluir = async () => {
    if (!window.confirm('Excluir este grupo? Esta ação não pode ser desfeita.')) return;
    try {
      await api.del(`/grupos/${id}`);
      navegar('/grupos');
    } catch (err) {
      setErro(err.message);
    }
  };

  return (
    <>
      <Link to="/grupos" className="voltar"><Icone nome="voltar" /> Grupos</Link>
      <div className="cabecalho-pagina">
        <div>
          <h1>{grupo.nome} {!ativo && <span className="etiqueta cinza">Encerrado</span>}</h1>
          <p className="subtitulo">Total de despesas: <b>{reais(grupo.totalGasto)}</b> · {grupo.participantes.length} participantes</p>
        </div>
        {grupo.souAdministrador && ativo && (
          <div className="acoes">
            {grupo.despesas.length === 0
              ? <button className="botao perigo" onClick={excluir}><Icone nome="lixo" /> Excluir grupo</button>
              : <button className="botao" onClick={encerrar} disabled={pendentes.length > 0}
                  title={pendentes.length > 0 ? 'Só é possível encerrar com todas as dívidas quitadas' : ''}>
                  <Icone nome="check" /> Encerrar grupo
                </button>}
          </div>
        )}
      </div>

      {erro && <div className="aviso erro-caixa"><Icone nome="alerta" /> {erro}</div>}
      {aviso && <div className="aviso sucesso"><Icone nome="check" /> {aviso}</div>}

      <div className="grade-detalhe">
        <div className="coluna">
          <section className="cartao">
            <h2>Saldos</h2>
            <ul className="saldos">
              {grupo.saldos.map((s) => {
                const v = Number(s.saldo);
                return (
                  <li key={s.usuarioId}>
                    <span className="avatar pequeno">{s.nome.split(' ').map((p) => p[0]).slice(0, 2).join('')}</span>
                    <span className="saldo-nome">{s.nome}{s.usuarioId === grupo.meuId && ' (você)'}</span>
                    <span className={`saldo-valor ${v > 0 ? 'positivo' : v < 0 ? 'negativo' : ''}`}>
                      {v > 0 ? `recebe ${reais(v)}` : v < 0 ? `deve ${reais(-v)}` : 'quite'}
                    </span>
                  </li>
                );
              })}
            </ul>
          </section>

          <section className="cartao">
            <h2>Quem deve a quem</h2>
            {pendentes.length === 0 ? <p className="vazio">Nenhuma dívida pendente.</p> : (
              <ul className="dividas">
                {pendentes.map((d) => {
                  const envolvido = d.devedorId === grupo.meuId || d.credorId === grupo.meuId;
                  return (
                    <li key={d.id}>
                      <div>
                        <b>{d.devedorNome}</b> deve <b>{reais(d.valor)}</b> a <b>{d.credorNome}</b>
                        <small>{d.despesa}</small>
                      </div>
                      {envolvido && ativo && (
                        <button className="botao pequeno" onClick={() => executar(() => api.post(`/dividas/${d.id}/quitar`), 'Dívida marcada como paga.')}>
                          <Icone nome="check" tamanho={16} /> Marcar como paga
                        </button>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
            {quitadas.length > 0 && (
              <details className="quitadas">
                <summary>{quitadas.length} dívida(s) quitada(s)</summary>
                <ul className="dividas">
                  {quitadas.map((d) => (
                    <li key={d.id} className="quitada">
                      <div>{d.devedorNome} pagou {reais(d.valor)} a {d.credorNome}<small>{d.despesa} · em {dataBR(d.dataQuitacao)}</small></div>
                    </li>
                  ))}
                </ul>
              </details>
            )}
          </section>

          <Participantes grupo={grupo} ativo={ativo} executar={executar} />
        </div>

        <div className="coluna">
          {ativo && <NovaDespesa grupo={grupo} executar={executar} />}
          <section className="cartao">
            <h2>Despesas</h2>
            {grupo.despesas.length === 0 ? <p className="vazio">Nenhuma despesa registrada.</p> : (
              <ul className="despesas">
                {grupo.despesas.map((d) => (
                  <li key={d.id}>
                    <div className="despesa-linha">
                      <div>
                        <b>{d.descricao}</b>
                        <small>{dataBR(d.data)} · pago por {d.pagadorNome}</small>
                      </div>
                      <span className="valor">{reais(d.valor)}</span>
                    </div>
                    <div className="divisoes">
                      {d.divisoes.map((v) => <span key={v.usuarioId} className="etiqueta cinza">{v.nome.split(' ')[0]}: {reais(v.valorDevido)}</span>)}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </div>
    </>
  );
}

function Participantes({ grupo, ativo, executar }) {
  const [email, setEmail] = useState('');
  const adicionar = (e) => {
    e.preventDefault();
    executar(() => api.post(`/grupos/${grupo.id}/participantes`, { email }), 'Participante adicionado.')
      .then((ok) => ok && setEmail(''));
  };
  return (
    <section className="cartao">
      <h2>Participantes</h2>
      <ul className="participantes">
        {grupo.participantes.map((p) => (
          <li key={p.usuarioId}>
            <span>{p.nome}<small>{p.email}</small></span>
            {p.papel === 'ADMINISTRADOR'
              ? <span className="etiqueta">Administrador</span>
              : grupo.souAdministrador && ativo && (
                <button className="botao-icone" aria-label={`Remover ${p.nome}`}
                  onClick={() => executar(() => api.del(`/grupos/${grupo.id}/participantes/${p.usuarioId}`), 'Participante removido.')}>
                  <Icone nome="lixo" />
                </button>
              )}
          </li>
        ))}
      </ul>
      {grupo.souAdministrador && ativo && (
        <form className="linha-form" onSubmit={adicionar}>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
            placeholder="E-mail do participante" required aria-label="E-mail do participante" />
          <button className="botao"><Icone nome="mais" /> Adicionar</button>
        </form>
      )}
    </section>
  );
}

function NovaDespesa({ grupo, executar }) {
  const todos = grupo.participantes.map((p) => p.usuarioId);
  const inicial = () => ({
    descricao: '', valor: '', data: hojeISO(), pagadorId: String(grupo.meuId),
    selecionados: todos, personalizada: false, valores: {},
  });
  const [f, setF] = useState(inicial);

  const alternar = (uid) => setF({
    ...f,
    selecionados: f.selecionados.includes(uid) ? f.selecionados.filter((x) => x !== uid) : [...f.selecionados, uid],
  });

  const soma = f.selecionados.reduce((s, uid) => s + Number(f.valores[uid] || 0), 0);
  const parteIgual = f.selecionados.length ? Number(f.valor || 0) / f.selecionados.length : 0;

  const salvar = (e) => {
    e.preventDefault();
    const valores = f.personalizada
      ? Object.fromEntries(f.selecionados.map((uid) => [uid, Number(f.valores[uid] || 0)]))
      : null;
    executar(() => api.post(`/grupos/${grupo.id}/despesas`, {
      descricao: f.descricao, valor: Number(f.valor), data: f.data,
      pagadorId: Number(f.pagadorId), participantesIds: f.selecionados, valores,
    }), 'Despesa registrada e saldos atualizados.').then((ok) => ok && setF(inicial()));
  };

  return (
    <section className="cartao destaque">
      <h2>Nova despesa</h2>
      <form className="form-grade" onSubmit={salvar}>
        <label className="col-4">Descrição
          <input value={f.descricao} onChange={(e) => setF({ ...f, descricao: e.target.value })}
            placeholder="Ex.: Pousada em Lençóis" required maxLength={200} />
        </label>
        <label className="col-2">Valor (R$)
          <input type="number" min="0.01" step="0.01" value={f.valor} onChange={(e) => setF({ ...f, valor: e.target.value })} required />
        </label>
        <label className="col-2">Data
          <input type="date" value={f.data} onChange={(e) => setF({ ...f, data: e.target.value })} required />
        </label>
        <label className="col-4">Quem pagou
          <select value={f.pagadorId} onChange={(e) => setF({ ...f, pagadorId: e.target.value })}>
            {grupo.participantes.map((p) => <option key={p.usuarioId} value={p.usuarioId}>{p.nome}</option>)}
          </select>
        </label>

        <fieldset className="col-4 divisao">
          <legend>Dividir entre</legend>
          <div className="alternador">
            <button type="button" className={!f.personalizada ? 'ativo' : ''} onClick={() => setF({ ...f, personalizada: false })}>Partes iguais</button>
            <button type="button" className={f.personalizada ? 'ativo' : ''} onClick={() => setF({ ...f, personalizada: true })}>Valores personalizados</button>
          </div>
          {grupo.participantes.map((p) => (
            <div key={p.usuarioId} className="participante-divisao">
              <label className="checkbox">
                <input type="checkbox" checked={f.selecionados.includes(p.usuarioId)} onChange={() => alternar(p.usuarioId)} />
                {p.nome}
              </label>
              {f.selecionados.includes(p.usuarioId) && (f.personalizada ? (
                <input type="number" min="0" step="0.01" className="valor-parte" aria-label={`Valor de ${p.nome}`}
                  value={f.valores[p.usuarioId] || ''} onChange={(e) => setF({ ...f, valores: { ...f.valores, [p.usuarioId]: e.target.value } })} />
              ) : <span className="detalhe">{reais(parteIgual)}</span>)}
            </div>
          ))}
          {f.personalizada && (
            <p className={`detalhe ${Math.abs(soma - Number(f.valor || 0)) > 0.001 ? 'negativo' : 'positivo'}`}>
              Soma das partes: {reais(soma)} de {reais(f.valor)}
            </p>
          )}
        </fieldset>
        <div className="acoes col-4">
          <button className="botao primario" disabled={f.selecionados.length === 0}><Icone nome="mais" /> Registrar despesa</button>
        </div>
      </form>
    </section>
  );
}
