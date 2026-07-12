import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ChevronLeft, ChevronRight, GitCompareArrows, Grid2X2, ListFilter, Rows3, SlidersHorizontal, X } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { apiFetch, getErrorMessage } from '../api/client.js';
import { useCompare } from '../context/CompareContext.jsx';
import { LoadingScreen } from '../components/LoadingScreen.jsx';
import { ResultCard } from '../components/ResultCard.jsx';
import { SearchFilters, toSearchRequest } from '../components/SearchFilters.jsx';

const DEFAULTS = { query: '', family: '', manufacturerId: '', applicationCode: '', materialCode: '', requiredPressureBar: '', requiredTemperatureC: '', requiredFlowM3h: '', requiredSurfaceAreaM2: '', requiredPowerKw: '', page: 0, size: 12, sort: 'RELEVANCE' };

function fromParams(params) {
  const result = { ...DEFAULTS };
  Object.keys(result).forEach((key) => {
    if (params.has(key)) result[key] = ['page', 'size'].includes(key) ? Number(params.get(key)) : params.get(key);
  });
  return result;
}

function normalizePage(payload, requestedSize) {
  return {
    items: payload?.items ?? payload?.content ?? [],
    page: payload?.page ?? payload?.number ?? 0,
    size: payload?.size ?? requestedSize,
    totalItems: payload?.totalItems ?? payload?.totalElements ?? 0,
    totalPages: payload?.totalPages ?? 0,
    excludedUnknownCount: payload?.excludedUnknownCount ?? 0,
  };
}

export function CatalogPage() {
  const [params, setParams] = useSearchParams();
  const [filters, setFilters] = useState(() => fromParams(params));
  const [lookups, setLookups] = useState({ families: [], manufacturers: [], applications: [], materials: [] });
  const [page, setPage] = useState({ items: [], page: 0, size: 12, totalItems: 0, totalPages: 0, excludedUnknownCount: 0 });
  const [view, setView] = useState(() => localStorage.getItem('thermoselect.view') || 'cards');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [mobileFilters, setMobileFilters] = useState(false);
  const [compareWarning, setCompareWarning] = useState(false);
  const compare = useCompare();

  useEffect(() => {
    apiFetch('/api/heat-exchangers/lookups').then((data) => setLookups({ families: data?.families ?? [], manufacturers: data?.manufacturers ?? [], applications: data?.applications ?? [], materials: data?.materials ?? [] })).catch((e) => setError(getErrorMessage(e)));
  }, []);

  const syncUrl = useCallback((value) => {
    const next = new URLSearchParams();
    Object.entries(value).forEach(([key, entry]) => { if (entry !== '' && entry != null && entry !== DEFAULTS[key]) next.set(key, String(entry)); });
    setParams(next, { replace: true });
  }, [setParams]);

  const search = useCallback(async (nextFilters = filters) => {
    setLoading(true); setError(''); syncUrl(nextFilters);
    try {
      const result = await apiFetch('/api/heat-exchangers/search', { method: 'POST', body: toSearchRequest(nextFilters) });
      setPage(normalizePage(result, nextFilters.size));
    } catch (requestError) { setError(getErrorMessage(requestError)); }
    finally { setLoading(false); }
  }, [filters, syncUrl]);

  useEffect(() => { search(filters); }, []); // initial request only

  function updatePage(nextPage) {
    const next = { ...filters, page: nextPage };
    setFilters(next); search(next); window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function changeView(next) { setView(next); localStorage.setItem('thermoselect.view', next); }
  const range = useMemo(() => page.totalItems === 0 ? '0' : `${page.page * page.size + 1}–${Math.min((page.page + 1) * page.size, page.totalItems)}`, [page]);

  return (
    <section className="catalog-page">
      <div className="catalog-hero"><div className="container"><span className="eyebrow eyebrow--light">Каталог / подбор</span><h1>Паспортные данные вместо предположений</h1><p>Задайте ограничения — ThermoSelect исключит неподтверждённые значения и объяснит порядок результатов.</p></div></div>
      <div className="container catalog-layout">
        <SearchFilters filters={filters} setFilters={setFilters} lookups={lookups} onSubmit={() => search({ ...filters, page: 0 })} mobileOpen={mobileFilters} setMobileOpen={setMobileFilters} />
        <div className="catalog-content">
          <div className="catalog-toolbar">
            <button className="button button--secondary mobile-filter-toggle" onClick={() => setMobileFilters(true)}><SlidersHorizontal size={17} /> Фильтры</button>
            <div><strong>{page.totalItems} аппаратов</strong><span>Показаны {range}</span></div>
            <div className="view-toggle"><button className={view === 'cards' ? 'active' : ''} onClick={() => changeView('cards')} title="Карточки"><Grid2X2 /></button><button className={view === 'table' ? 'active' : ''} onClick={() => changeView('table')} title="Таблица"><Rows3 /></button></div>
          </div>
          {page.excludedUnknownCount > 0 && <div className="info-banner"><AlertTriangle /><span><strong>{page.excludedUnknownCount}</strong> записей исключено: у производителя нет значения для выбранного строгого критерия.</span></div>}
          {error && <div className="alert alert--error" role="alert">{error}<button className="button button--small" onClick={() => search()}>Повторить</button></div>}
          {loading ? <LoadingScreen label="Сопоставляем характеристики…" /> : page.items.length === 0 ? (
            <div className="empty-state"><span><ListFilter /></span><h2>Подходящих аппаратов не найдено</h2><p>Ослабьте один из числовых критериев или выберите другое семейство.</p><button className="button button--primary" onClick={() => { const clean = { ...DEFAULTS }; setFilters(clean); search(clean); }}>Сбросить фильтры</button></div>
          ) : view === 'cards' ? (
            <div className="results-grid">{page.items.map((item) => <ResultCard key={item.id} item={item} onCompareLimit={() => setCompareWarning(true)} />)}</div>
          ) : (
            <div className="table-scroll"><table className="data-table results-table"><thead><tr><th>Тип</th><th>Модель</th><th>Гранулярность</th><th>Давление</th><th>Темп.</th><th>Расход</th><th>Площадь</th><th>Рейтинг</th><th /></tr></thead><tbody>{page.items.map((item) => <ResultCard key={item.id} item={item} view="table" onCompareLimit={() => setCompareWarning(true)} />)}</tbody></table></div>
          )}
          {!loading && page.totalPages > 1 && <nav className="pagination" aria-label="Страницы"><button disabled={page.page === 0} onClick={() => updatePage(page.page - 1)}><ChevronLeft /> Назад</button><span>Страница <strong>{page.page + 1}</strong> из {page.totalPages}</span><button disabled={page.page + 1 >= page.totalPages} onClick={() => updatePage(page.page + 1)}>Вперёд <ChevronRight /></button></nav>}
        </div>
      </div>
      {compare.ids.length > 0 && <div className="compare-dock"><div><GitCompareArrows /><span><strong>{compare.ids.length}</strong> из 4 выбрано</span></div><button className="button button--ghost button--small" onClick={compare.clear}>Очистить</button><Link className="button button--accent" to="/compare">Сравнить</Link></div>}
      {compareWarning && <div className="toast" role="status"><AlertTriangle /><span>Можно сравнить не более четырёх моделей.</span><button className="icon-button" onClick={() => setCompareWarning(false)}><X /></button></div>}
    </section>
  );
}
