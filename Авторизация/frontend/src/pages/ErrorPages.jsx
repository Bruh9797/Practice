import { ArrowLeft, LockKeyhole, SearchX } from 'lucide-react';
import { Link } from 'react-router-dom';

export function ForbiddenPage(){return <section className="page-section container"><div className="empty-state"><span><LockKeyhole/></span><h1>Недостаточно прав</h1><p>Этот раздел доступен только администратору.</p><Link className="button button--primary" to="/catalog"><ArrowLeft/>В каталог</Link></div></section>;}
export function NotFoundPage(){return <section className="page-section container"><div className="empty-state"><span><SearchX/></span><h1>Страница не найдена</h1><p>Проверьте адрес или вернитесь на главную страницу.</p><Link className="button button--primary" to="/"><ArrowLeft/>На главную</Link></div></section>;}
