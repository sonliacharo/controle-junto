// Ícones simples em SVG (traço), para não depender de bibliotecas externas.
const caminhos = {
  painel: 'M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z',
  gastos: 'M3 6h18M3 12h18M3 18h12',
  grupos: 'M16 11a3 3 0 1 0-3-3 3 3 0 0 0 3 3zM8 11a3 3 0 1 0-3-3 3 3 0 0 0 3 3zm0 2c-2.7 0-6 1.3-6 3.5V19h12v-2.5C14 14.3 10.7 13 8 13zm8 0c-.3 0-.7 0-1.1.1A4.3 4.3 0 0 1 16 16.5V19h6v-2.5c0-2.2-3.3-3.5-6-3.5z',
  sino: 'M18 16v-5a6 6 0 1 0-12 0v5l-2 2h16l-2-2zM10 20a2 2 0 0 0 4 0',
  sair: 'M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3',
  ia: 'M12 3l1.8 4.7L18.5 9.5 13.8 11.3 12 16l-1.8-4.7L5.5 9.5l4.7-1.8L12 3zM19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9L19 15z',
  mais: 'M12 5v14M5 12h14',
  lixo: 'M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3',
  editar: 'M4 20h4L19 9l-4-4L4 16v4zM14 6l4 4',
  check: 'M5 12l5 5L20 7',
  alerta: 'M12 9v4M12 17h.01M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z',
  voltar: 'M15 18l-6-6 6-6',
};

export default function Icone({ nome, tamanho = 18 }) {
  const preenchido = nome === 'painel' || nome === 'grupos' || nome === 'ia';
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 24 24" aria-hidden="true"
      fill={preenchido ? 'currentColor' : 'none'}
      stroke={preenchido ? 'none' : 'currentColor'}
      strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d={caminhos[nome]} />
    </svg>
  );
}
