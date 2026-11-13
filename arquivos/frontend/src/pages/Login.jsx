import { useState } from 'react';
import { api } from '../api.js';

/** RF-001 (cadastro) e RF-002 (login). */
export default function Login({ onEntrar }) {
  const [modo, setModo] = useState('login');
  const [form, setForm] = useState({ nome: '', email: '', senha: '' });
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  const campo = (nome) => (e) => setForm({ ...form, [nome]: e.target.value });

  const enviar = async (e) => {
    e.preventDefault();
    setErro('');
    setCarregando(true);
    try {
      const dados = modo === 'login'
        ? await api.post('/auth/login', { email: form.email, senha: form.senha })
        : await api.post('/auth/cadastro', form);
      onEntrar(dados);
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  };

  return (
    <div className="login">
      <div className="login-caixa">
        <div className="marca marca-login">
          <span className="marca-logo" aria-hidden="true" />
          <span>ControleJunto</span>
        </div>

        <section className="login-cartao">
          <h1>{modo === 'login' ? 'Acesse sua conta' : 'Crie sua conta'}</h1>
          <form onSubmit={enviar}>
            {modo === 'cadastro' && (
              <label>Nome
                <input value={form.nome} onChange={campo('nome')} required autoComplete="name" />
              </label>
            )}
            <label>E-mail
              <input type="email" value={form.email} onChange={campo('email')} required autoComplete="email" />
            </label>
            <label>Senha
              <input type="password" value={form.senha} onChange={campo('senha')} required minLength={6}
                autoComplete={modo === 'login' ? 'current-password' : 'new-password'} />
            </label>
            {erro && <p className="erro">{erro}</p>}
            <button className="botao primario largo" disabled={carregando}>
              {carregando ? 'Aguarde…' : modo === 'login' ? 'Entrar' : 'Cadastrar'}
            </button>
          </form>
        </section>

        <p className="login-troca">
          {modo === 'login' ? 'Ainda não tem conta?' : 'Já tem conta?'}{' '}
          <button className="link" onClick={() => { setModo(modo === 'login' ? 'cadastro' : 'login'); setErro(''); }}>
            {modo === 'login' ? 'Cadastre-se' : 'Entrar'}
          </button>
        </p>
        {modo === 'login' && (
          <p className="dica">Demonstração: livia@controlejunto.com · senha 123456</p>
        )}
      </div>
    </div>
  );
}
