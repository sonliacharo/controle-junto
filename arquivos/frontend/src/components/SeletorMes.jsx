import { MESES } from '../api.js';

/** Escolha de mês e ano usada no painel e nos gastos. */
export default function SeletorMes({ mes, ano, onChange }) {
  const anterior = () => (mes === 1 ? onChange(12, ano - 1) : onChange(mes - 1, ano));
  const proximo = () => (mes === 12 ? onChange(1, ano + 1) : onChange(mes + 1, ano));
  return (
    <div className="seletor-mes">
      <button className="botao-icone" onClick={anterior} aria-label="Mês anterior">‹</button>
      <span>{MESES[mes - 1]} de {ano}</span>
      <button className="botao-icone" onClick={proximo} aria-label="Próximo mês">›</button>
    </div>
  );
}
