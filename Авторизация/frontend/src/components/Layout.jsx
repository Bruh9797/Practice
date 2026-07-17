import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { GitCompareArrows, LogOut, Menu, Search, Shield, UserRound, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext.jsx';
import { useCompare } from '../context/CompareContext.jsx';
import { getErrorMessage } from '../api/client.js';
import { Brand } from './Brand.jsx';

const navClass = ({ isActive }) => `nav-link ${isActive ? 'nav-link--active' : ''}`;

export function Layout() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { ids } = useCompare();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [logoutError, setLogoutError] = useState('');

  async function handleLogout() {
    setLogoutError('');
    try {
      await logout();
      setOpen(false);
      navigate('/');
    } catch (error) {
      setLogoutError(getErrorMessage(error));
    }
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="container site-header__inner">
          <Brand />
          <button className="icon-button mobile-menu-button" type="button" onClick={() => setOpen(!open)} aria-label="Открыть меню">
            {open ? <X /> : <Menu />}
          </button>
          <nav className={`main-nav ${open ? 'main-nav--open' : ''}`} aria-label="Основная навигация">
            {isAuthenticated && (
              <>
                <NavLink className={navClass} to="/catalog" onClick={() => setOpen(false)}><Search size={17} /> Каталог</NavLink>
                <NavLink className={navClass} to="/compare" onClick={() => setOpen(false)}>
                  <GitCompareArrows size={17} /> Сравнение <span className="nav-count">{ids.length}</span>
                </NavLink>
                {isAdmin && <NavLink className={navClass} to="/admin" onClick={() => setOpen(false)}><Shield size={17} /> Админка</NavLink>}
              </>
            )}
            <div className="main-nav__spacer" />
            {isAuthenticated ? (
              <>
                <Link className="user-chip" to="/account" onClick={() => setOpen(false)}>
                  <span className="user-chip__avatar"><UserRound size={16} /></span>
                  <span><strong>{user?.username}</strong><small>{isAdmin ? 'Администратор' : 'Пользователь'}</small></span>
                </Link>
                <button className="button button--ghost button--small" type="button" onClick={handleLogout}><LogOut size={16} /> Выйти</button>
              </>
            ) : (
              <>
                <NavLink className={navClass} to="/login" onClick={() => setOpen(false)}>Войти</NavLink>
                <Link className="button button--primary button--small" to="/register" onClick={() => setOpen(false)}>Регистрация</Link>
              </>
            )}
          </nav>
        </div>
      </header>
      {logoutError && <div className="container"><div className="alert alert--error header-alert" role="alert">Не удалось завершить сессию: {logoutError}</div></div>}
      <main className="site-main"><Outlet /></main>
      <footer className="site-footer">
        <div className="container site-footer__inner">
          <Brand compact />
          <p>Информационно-поисковая система выбора теплообменных аппаратов</p>
          <span>Учебная практика · 2026</span>
        </div>
      </footer>
    </div>
  );
}
