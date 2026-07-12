import { useCallback, useEffect, useState } from 'react';
import { Archive, Edit3, Plus, RefreshCw, Search, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiFetch, getErrorMessage } from '../api/client.js';
import { FAMILY_LABELS, GRANULARITY_LABELS, STATUS_LABELS, manufacturerName } from '../api/contracts.js';
import { LoadingScreen } from '../components/LoadingScreen.jsx';

export function AdminCatalogPage() {
  const [query, setQuery] = useState(''); const [status, setStatus] = useState('');
  const [data, setData] = useState({ items: [], page: 0, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const load = useCallback(async (page = 0) => {
    setLoading(true); setError('');
    const params = new URLSearchParams({ page: String(page), size: '20' }); if (query) params.set('query', query); if (status) params.set('status', status);
    try { const result = await apiFetch(`/api/admin/heat-exchangers?${params}`); setData({ ...result, items: result.items ?? result.content ?? [], totalElements: result.totalElements ?? result.totalItems ?? 0 }); }
    catch (e) { setError(getErrorMessage(e)); } finally { setLoading(false); }
  }, [query, status]);
  useEffect(() => { load(); }, [status]);

  async function updateStatus(item, nextStatus) {
    try { await apiFetch(`/api/admin/heat-exchangers/${item.id}/status`, { method: 'PATCH', body: { status: nextStatus, version: item.version } }); await load(data.page); }
    catch (e) { setError(getErrorMessage(e)); }
  }
  async function archive(item) {
    if (!confirm(`Архивировать «${item.model}»?`)) return;
    try { await apiFetch(`/api/admin/heat-exchangers/${item.id}?version=${item.version}`, { method: 'DELETE' }); await load(data.page); }
    catch (e) { setError(getErrorMessage(e)); }
  }
  return (
    <section className="page-section admin-page"><div className="container"><div className="page-heading"><div><span className="eyebrow">Админка / каталог</span><h1>Каталог аппаратов</h1><p>{data.totalElements} записей с контролируемым статусом и источниками.</p></div><Link className="button button--primary" to="/admin/catalog/new"><Plus /> Новая запись</Link></div><div className="admin-toolbar"><form onSubmit={(e) => { e.preventDefault(); load(); }}><div className="input-with-icon"><Search /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Модель, серия или производитель" /></div><button className="button button--secondary">Найти</button></form><select value={status} onChange={(e) => setStatus(e.target.value)}><option value="">Все статусы</option><option value="DRAFT">Черновики</option><option value="PUBLISHED">Опубликованные</option><option value="ARCHIVED">Архив</option></select><button className="icon-button" onClick={() => load(data.page)} title="Обновить"><RefreshCw /></button></div>{error && <div className="alert alert--error">{error}</div>}{loading ? <LoadingScreen /> : <div className="table-scroll"><table className="data-table admin-table"><thead><tr><th>Модель</th><th>Тип</th><th>Гранулярность</th><th>Статус</th><th>Версия</th><th /></tr></thead><tbody>{data.items.map((item) => <tr key={item.id}><td><strong>{item.model || item.name}</strong><small>{manufacturerName(item.manufacturer)} · {item.seriesName}</small></td><td>{FAMILY_LABELS[item.family] ?? item.family}</td><td>{GRANULARITY_LABELS[item.granularity] ?? item.granularity}</td><td><span className={`status status--${item.status?.toLowerCase()}`}>{STATUS_LABELS[item.status] ?? item.status}</span></td><td>v{item.version ?? 0}</td><td className="table-actions"><Link className="icon-button" to={`/admin/catalog/${item.id}`} title="Редактировать"><Edit3 /></Link>{item.status === 'DRAFT' && <button className="icon-button icon-button--success" onClick={() => updateStatus(item, 'PUBLISHED')} title="Опубликовать"><RefreshCw /></button>}{item.status === 'ARCHIVED' ? <button className="icon-button icon-button--success" onClick={() => updateStatus(item, 'DRAFT')} title="Восстановить"><RefreshCw /></button> : <button className="icon-button icon-button--danger" onClick={() => archive(item)} title="Архивировать"><Archive /></button>}</td></tr>)}</tbody></table></div>}{!loading && data.items.length === 0 && <div className="empty-state"><Trash2 /><h2>Записей не найдено</h2><p>Измените фильтр или создайте новую запись.</p></div>}</div></section>
  );
}
