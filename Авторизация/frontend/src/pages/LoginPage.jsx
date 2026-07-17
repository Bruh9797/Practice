import { useState } from 'react';
import { ArrowRight, LockKeyhole, UserRound } from 'lucide-react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (auth.isAuthenticated) return <Navigate to="/catalog" replace />;

  async function submit(event) {
    event.preventDefault(); setSubmitting(true); setError('');
    try {
      await auth.login(form);
      navigate(location.state?.from?.pathname || '/catalog', { replace: true });
    } catch (requestError) { setError(getErrorMessage(requestError)); }
    finally { setSubmitting(false); }
  }

  return (
    <section className="auth-section">
      <div className="auth-panel">
        <div className="auth-panel__aside"><span className="eyebrow eyebrow--light">ThermoSelect / Access</span><h1>Инженерные данные доступны после входа</h1><p>Авторизация защищает каталог, сравнение и административные действия.</p><div className="auth-orbit"><i /><i /><i /><LockKeyhole /></div></div>
        <div className="auth-panel__form">
          <div><span className="eyebrow">Добро пожаловать</span><h2>Вход в систему</h2><p>Введите данные своей учётной записи.</p></div>
          {location.state?.registered && <div className="alert alert--success">Регистрация завершена. Теперь войдите в систему.</div>}
          {error && <div className="alert alert--error" role="alert">{error}</div>}
          <form onSubmit={submit} className="form-stack">
            <label><span>Логин</span><div className="input-with-icon"><UserRound /><input autoFocus required autoComplete="username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} placeholder="Введите логин" /></div></label>
            <label><span>Пароль</span><div className="input-with-icon"><LockKeyhole /><input required minLength={8} type="password" autoComplete="current-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Не менее 8 символов" /></div></label>
            <button className="button button--primary button--large button--full" disabled={submitting}>{submitting ? <span className="spinner spinner--small" /> : <>Войти <ArrowRight /></>}</button>
          </form>
          <p className="auth-switch">Нет аккаунта? <Link to="/register">Зарегистрироваться</Link></p>
        </div>
      </div>
    </section>
  );
}
