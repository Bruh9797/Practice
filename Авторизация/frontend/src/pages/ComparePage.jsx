import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, GitCompareArrows, Plus, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiFetch, getErrorMessage } from '../api/client.js';
import { FAMILY_LABELS, GRANULARITY_LABELS, manufacturerName } from '../api/contracts.js';
import { ProductPhoto } from '../components/ProductPhoto.jsx';
import { LoadingScreen } from '../components/LoadingScreen.jsx';
import { useCompare } from '../context/CompareContext.jsx';
import { formatDimensions, formatRange, formatValue } from '../utils/formatters.js';

const rows = [
  ['Семейство', (item) => FAMILY_LABELS[item.family] ?? item.family],
  ['Гранулярность', (item) => GRANULARITY_LABELS[item.granularity] ?? item.granularity],
  ['Площадь, м²', (item) => formatValue(item.surfaceAreaM2, '', 1)],
  ['Расход min–max, м³/ч', (item) => formatRange(item.flowMinM3h, item.flowMaxM3h)],
  ['Температура min–max, °C', (item) => formatRange(item.temperatureMinC, item.temperatureMaxC)],
  ['Макс. давление, bar', (item) => formatValue(item.pressureMaxBar, '', 1)],
  ['Габариты Ш×В×Г, мм', (item) => formatDimensions(item.widthMm, item.heightMm, item.depthMm)],
  ['Масса, кг', (item) => formatValue(item.massKg, '', 1)],
];

function valueOrDash(value) { return value == null || value === '' ? '—' : value; }

export function ComparePage() {
  const compare = useCompare();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (compare.ids.length < 2) { setItems([]); return; }
    setLoading(true); setError('');
    apiFetch('/api/heat-exchangers/compare', { method: 'POST', body: { ids: compare.ids } })
      .then((payload) => setItems(payload?.items ?? payload ?? []))
      .catch((e) => setError(getErrorMessage(e)))
      .finally(() => setLoading(false));
  }, [compare.ids.join(',')]);

  const differences = useMemo(() => rows.map(([label, getter]) => ({ label, getter, differs: new Set(items.map((item) => String(getter(item) ?? ''))).size > 1 })), [items]);

  return (
    <section className="compare-page page-section">
      <div className="container"><Link className="back-link back-link--dark" to="/catalog"><ArrowLeft /> Вернуться в каталог</Link><div className="page-heading"><div><span className="eyebrow">Сравнение</span><h1>Модели рядом</h1><p>Различающиеся строки подсвечены. Диапазон серии не заменяет точную конфигурацию.</p></div><div className="compare-counter"><GitCompareArrows /><strong>{compare.ids.length}/4</strong><span>выбрано</span></div></div>
        {error && <div className="alert alert--error">{error}</div>}
        {compare.ids.length < 2 ? <div className="empty-state"><span><GitCompareArrows /></span><h2>Выберите минимум две модели</h2><p>Добавляйте аппараты из каталога — здесь можно сопоставить до четырёх.</p><Link className="button button--primary" to="/catalog"><Plus /> Выбрать модели</Link></div> : loading ? <LoadingScreen label="Строим таблицу сравнения…" /> : (
          <div className="compare-table-wrap"><table className="compare-table"><thead><tr><th>Параметр</th>{items.map((item) => <th key={item.id}><button className="compare-remove" onClick={() => compare.toggle(item.id)} title="Убрать"><Trash2 /></button><ProductPhoto item={item} size="compare" /><small>{manufacturerName(item.manufacturer)}</small><strong>{item.model || item.name}</strong><Link to={`/heat-exchangers/${item.slug}`}>Открыть карточку</Link></th>)}</tr></thead><tbody>{differences.map(({ label, getter, differs }) => <tr className={differs ? 'is-different' : ''} key={label}><th>{label}{differs && <span>различается</span>}</th>{items.map((item) => <td key={item.id}>{valueOrDash(getter(item))}</td>)}</tr>)}<tr><th>Области применения</th>{items.map((item) => <td key={item.id}><div className="chip-list">{item.applications?.map((entry) => <span key={entry.code}>{entry.name ?? entry.label}</span>)}</div></td>)}</tr></tbody></table></div>
        )}
      </div>
    </section>
  );
}
