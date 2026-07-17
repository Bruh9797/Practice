import { useEffect, useState } from 'react';
import { ArrowRight, Database, ShieldCheck, UsersRound } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiFetch } from '../api/client.js';

export function AdminDashboardPage() {
  const [counts, setCounts] = useState({ models: '—', users: '—', archived: '—' });
  useEffect(() => {
    Promise.all([
      apiFetch('/api/admin/heat-exchangers?page=0&size=1').catch(() => ({})),
      apiFetch('/api/admin/heat-exchangers?status=ARCHIVED&page=0&size=1').catch(() => ({})),
      apiFetch('/api/admin/users').catch(() => []),
    ]).then(([models, archived, users]) => setCounts({ models: models.totalElements ?? models.totalItems ?? 0, archived: archived.totalElements ?? archived.totalItems ?? 0, users: users.length ?? users.totalElements ?? 0 }));
  }, []);
  return (
    <section className="page-section admin-page"><div className="container"><div className="page-heading"><div><span className="eyebrow">Панель администратора</span><h1>Управление ThermoSelect</h1><p>Каталог, публикации и права пользователей в одном рабочем пространстве.</p></div></div><div className="admin-stats"><div><Database /><span>Записей каталога</span><strong>{counts.models}</strong></div><div><UsersRound /><span>Пользователей</span><strong>{counts.users}</strong></div><div><ShieldCheck /><span>В архиве</span><strong>{counts.archived}</strong></div></div><div className="admin-action-grid"><Link to="/admin/catalog"><Database /><div><h2>Каталог аппаратов</h2><p>Создание, проверка источников, публикация и архивирование.</p></div><ArrowRight /></Link><Link to="/admin/users"><UsersRound /><div><h2>Пользователи</h2><p>Роли, блокировки и удаление учётных записей.</p></div><ArrowRight /></Link></div></div></section>
  );
}
