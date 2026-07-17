import { useState } from 'react';
import { ArrowRight, AtSign, LockKeyhole, UserRound } from 'lucide-react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';

export function RegisterPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', email: '', password: '', repeat: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  if (auth.isAuthenticated) return <Navigate to="/catalog" replace />;

  async function submit(event) {
    event.preventDefault(); setError('');
    if (form.password !== form.repeat) { setError('Пароли не совпадают'); return; }
    setSubmitting(true);
    try {
      await auth.register({ username: form.username, email: form.email || null, password: form.password });
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (requestError) { setError(getErrorMessage(requestError)); }
    finally { setSubmitting(false); }
  }

  return (
    <section className="auth-section">
      <div className="auth-panel auth-panel--reverse">
        <div className="auth-panel__aside"><span className="eyebrow eyebrow--light">ThermoSelect / New user</span><h1>Создайте рабочее пространство подбора</h1><p>Самостоятельная регистрация всегда создаёт безопасную роль USER.</p><div className="auth-data-lines"><i /><i /><i /><i /><i /></div></div>
        <div className="auth-panel__form">
          <div><span className="eyebrow">Новая учётная запись</span><h2>Регистрация</h2><p>После регистрации потребуется выполнить вход.</p></div>
          {error && <div className="alert alert--error" role="alert">{error}</div>}
          <form onSubmit={submit} className="form-stack">
            <label><span>Логин</span><div className="input-with-icon"><UserRound /><input required minLength={3} maxLength={64} autoComplete="username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></div></label>
            <label><span>Email <small>необязательно</small></span><div className="input-with-icon"><AtSign /><input type="email" autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div></label>
            <div className="form-row"><label><span>Пароль</span><div className="input-with-icon"><LockKeyhole /><input required minLength={8} maxLength={128} type="password" autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></div></label><label><span>Повторите пароль</span><div className="input-with-icon"><LockKeyhole /><input required type="password" autoComplete="new-password" value={form.repeat} onChange={(e) => setForm({ ...form, repeat: e.target.value })} /></div></label></div>
            <button className="button button--primary button--large button--full" disabled={submitting}>{submitting ? <span className="spinner spinner--small" /> : <>Создать аккаунт <ArrowRight /></>}</button>
          </form>
          <p className="auth-switch">Уже зарегистрированы? <Link to="/login">Войти</Link></p>
        </div>
      </div>
    </section>
  );
}
