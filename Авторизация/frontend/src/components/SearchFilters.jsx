import { Filter, RotateCcw, Search, SlidersHorizontal, X } from 'lucide-react';
import { numberValue } from '../api/contracts.js';

function optionsOf(values = []) {
  return values.map((value) => ({ value: value.id ?? value.code, label: value.label ?? value.name ?? value.code }));
}

export function SearchFilters({ filters, setFilters, lookups, onSubmit, mobileOpen, setMobileOpen }) {
  const set = (key, value) => setFilters((current) => ({ ...current, [key]: value, page: 0 }));
  const count = Object.entries(filters).filter(([key, value]) => !['page', 'size', 'sort'].includes(key) && value !== '' && value != null).length;
  const families = optionsOf(lookups.families);
  const manufacturers = optionsOf(lookups.manufacturers);
  const applications = optionsOf(lookups.applications);
  const materials = optionsOf(lookups.materials);

  function reset() {
    setFilters({ query: '', family: '', manufacturerId: '', applicationCode: '', materialCode: '', requiredPressureBar: '', requiredTemperatureC: '', requiredFlowM3h: '', requiredSurfaceAreaM2: '', requiredPowerKw: '', page: 0, size: filters.size || 12, sort: 'RELEVANCE' });
  }

  function submit(event) { event.preventDefault(); onSubmit(); setMobileOpen(false); }

  return (
    <aside className={`filters-panel ${mobileOpen ? 'filters-panel--open' : ''}`}>
      <div className="filters-panel__mobile-head"><strong><SlidersHorizontal /> Критерии подбора</strong><button className="icon-button" onClick={() => setMobileOpen(false)} type="button"><X /></button></div>
      <form onSubmit={submit}>
        <div className="filter-group filter-group--search"><label htmlFor="catalog-query">Поиск по модели, серии или SKU</label><div className="input-with-icon"><Search /><input id="catalog-query" value={filters.query} onChange={(e) => set('query', e.target.value)} placeholder="Например, NX80M" /></div></div>
        <div className="filter-group"><h3>Классификация</h3>
          <label>Семейство<select value={filters.family} onChange={(e) => set('family', e.target.value)}><option value="">Все семейства</option>{families.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
          <label>Производитель<select value={filters.manufacturerId} onChange={(e) => set('manufacturerId', e.target.value)}><option value="">Все производители</option>{manufacturers.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
          <label>Применение<select value={filters.applicationCode} onChange={(e) => set('applicationCode', e.target.value)}><option value="">Любая область</option>{applications.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
          <label>Материал<select value={filters.materialCode} onChange={(e) => set('materialCode', e.target.value)}><option value="">Любой материал</option>{materials.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
        </div>
        <div className="filter-group"><h3>Рабочие ограничения</h3>
          <div className="unit-input"><label>Давление не менее<input inputMode="decimal" type="number" min="0" step="0.1" value={filters.requiredPressureBar} onChange={(e) => set('requiredPressureBar', e.target.value)} /></label><span>bar</span></div>
          <div className="unit-input"><label>Температура не ниже<input inputMode="decimal" type="number" step="1" value={filters.requiredTemperatureC} onChange={(e) => set('requiredTemperatureC', e.target.value)} /></label><span>°C</span></div>
          <div className="unit-input"><label>Расход не менее<input inputMode="decimal" type="number" min="0" step="0.1" value={filters.requiredFlowM3h} onChange={(e) => set('requiredFlowM3h', e.target.value)} /></label><span>м³/ч</span></div>
          <div className="unit-input"><label>Площадь не менее<input inputMode="decimal" type="number" min="0" step="0.1" value={filters.requiredSurfaceAreaM2} onChange={(e) => set('requiredSurfaceAreaM2', e.target.value)} /></label><span>м²</span></div>
          <div className="unit-input"><label>Мощность серии от<input inputMode="decimal" type="number" min="0" step="1" value={filters.requiredPowerKw} onChange={(e) => set('requiredPowerKw', e.target.value)} /></label><span>кВт</span></div>
          <p className="filter-hint">Неизвестное паспортное значение не считается совпадением.</p>
        </div>
        <div className="filters-panel__actions"><button className="button button--primary button--full" type="submit"><Filter size={17} /> Применить {count > 0 && <span>{count}</span>}</button><button className="button button--ghost button--full" type="button" onClick={reset}><RotateCcw size={16} /> Сбросить</button></div>
      </form>
    </aside>
  );
}

export function toSearchRequest(filters) {
  const request = { page: Number(filters.page || 0), size: Number(filters.size || 12) };
  if (filters.query?.trim()) request.query = filters.query.trim();
  if (filters.family) request.families = [filters.family];
  if (filters.manufacturerId) request.manufacturerIds = [Number(filters.manufacturerId)];
  if (filters.applicationCode) request.applicationCodes = [filters.applicationCode];
  if (filters.materialCode) request.materialCodes = [filters.materialCode];
  ['requiredPressureBar', 'requiredTemperatureC', 'requiredFlowM3h', 'requiredSurfaceAreaM2', 'requiredPowerKw'].forEach((key) => {
    const value = numberValue(filters[key]);
    if (value !== undefined) request[key] = value;
  });
  return request;
}
