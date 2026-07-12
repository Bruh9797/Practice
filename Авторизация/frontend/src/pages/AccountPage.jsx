import { CalendarDays, Mail, Shield, UserRound } from 'lucide-react';
import { useAuth } from '../context/AuthContext.jsx';

export function AccountPage() {
  const { user } = useAuth();
  return (
    <section className="page-section account-page"><div className="container"><div className="page-heading"><div><span className="eyebrow">Учётная запись</span><h1>Ваш профиль</h1><p>Данные доступны только для просмотра.</p></div></div><article className="account-card"><div className="account-card__avatar"><UserRound /></div><div className="account-card__identity"><span>{user.role === 'ADMIN' ? 'Администратор' : 'Пользователь'}</span><h2>{user.username}</h2><p>ID #{user.id}</p></div><dl><div><dt><Mail /> Email</dt><dd>{user.email || 'Не указан'}</dd></div><div><dt><Shield /> Роль</dt><dd><span className={`status status--${user.role.toLowerCase()}`}>{user.role}</span></dd></div><div><dt><CalendarDays /> Создана</dt><dd>{user.createdAt ? new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' }).format(new Date(user.createdAt)) : '—'}</dd></div></dl><div className="data-note"><p>Редактирование профиля и смена пароля не входят в эту версию. Для PostgreSQL учётная запись администратора задаётся переменными окружения.</p></div></article></div></section>
  );
}
