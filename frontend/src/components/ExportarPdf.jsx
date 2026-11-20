import { useState } from 'react';
import { api } from '../api.js';
import Icone from './Icone.jsx';

/** RF-015: baixa o relatório do mês em PDF. */
export default function ExportarPdf({ mes, ano }) {
  const [gerando, setGerando] = useState(false);

  const exportar = async () => {
    setGerando(true);
    try {
      const mm = String(mes).padStart(2, '0');
      await api.baixar(`/relatorios/mensal?mes=${mes}&ano=${ano}`, `controlejunto-relatorio-${ano}-${mm}.pdf`);
    } catch (err) {
      window.alert(err.message);
    } finally {
      setGerando(false);
    }
  };

  return (
    <button className="botao" onClick={exportar} disabled={gerando} title="Baixar o relatório do mês em PDF">
      <Icone nome="baixar" /> {gerando ? 'Gerando…' : 'Exportar PDF'}
    </button>
  );
}
