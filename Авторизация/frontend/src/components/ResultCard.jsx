import { ArrowUpRight, Check, GitCompareArrows, Gauge, Layers3, Thermometer, Waves } from 'lucide-react';
import { Link } from 'react-router-dom';
import { CONFIDENCE_LABELS, FAMILY_LABELS, GRANULARITY_LABELS, manufacturerName } from '../api/contracts.js';
import { useCompare } from '../context/CompareContext.jsx';
import { ProductPhoto } from './ProductPhoto.jsx';
import { formatValue } from '../utils/formatters.js';

function spec(item, ...keys) {
  for (const key of keys) {
    const value = item?.specifications?.[key] ?? item?.[key];
    if (value != null && value !== '') return value;
  }
  return null;
}

function formatSpec(value, unit) {
  if (value == null) return '—';
  if (typeof value === 'object') {
    const min = value.min ?? value.minimum;
    const max = value.max ?? value.maximum;
    if (min != null && max != null) return `${formatValue(min, '', 1)}–${formatValue(max, value.unit ?? unit, 1)}`;
    return formatValue(value.value ?? max ?? min, value.unit ?? unit, 1);
  }
  return formatValue(value, unit, 1);
}

export function ResultCard({ item, view = 'cards', onCompareLimit }) {
  const compare = useCompare();
  const selected = compare.has(item.id);
  const name = item.name || [item.series, item.model, item.configuration].filter(Boolean).join(' ') || 'Без названия';

  function handleCompare() {
    if (compare.toggle(item.id) === 'limit') onCompareLimit?.();
  }

  const rows = [
    { icon: Gauge, label: 'Давление', value: formatSpec(spec(item, 'pressureMaxBar', 'maxPressureBar', 'maxWorkingPressureBar'), 'bar') },
    { icon: Thermometer, label: 'Температура', value: formatSpec(spec(item, 'temperatureMaxC', 'maxTemperatureC', 'maxOperatingTemperatureC'), '°C') },
    { icon: Waves, label: 'Расход', value: formatSpec(spec(item, 'flowMaxM3h', 'maxFlowM3h', 'maxWaterFlowM3h'), 'м³/ч') },
    { icon: Layers3, label: 'Площадь', value: formatSpec(spec(item, 'surfaceAreaM2', 'areaM2', 'heatTransferAreaM2'), 'м²') },
  ];

  if (view === 'table') {
    return (
      <tr>
        <td><span className={`family-dot family-dot--${item.family?.toLowerCase()}`} />{FAMILY_LABELS[item.family] ?? item.family}</td>
        <td><strong>{name}</strong><small>{manufacturerName(item.manufacturer)}</small></td>
        <td>{rows[0].value}</td><td>{rows[1].value}</td><td>{rows[2].value}</td><td>{rows[3].value}</td>
        <td className="table-actions">
          <button className={`icon-button ${selected ? 'icon-button--selected' : ''}`} type="button" onClick={handleCompare} aria-label="Добавить к сравнению"><GitCompareArrows size={17} /></button>
          <Link className="icon-button" to={`/heat-exchangers/${item.slug}`} aria-label="Открыть"><ArrowUpRight size={17} /></Link>
        </td>
      </tr>
    );
  }

  return (
    <article className="result-card">
      <div className="result-card__visual">
        <ProductPhoto item={item} />
        <span className="family-badge">{FAMILY_LABELS[item.family] ?? item.family}</span>
      </div>
      <div className="result-card__body">
        <div className="eyebrow">{manufacturerName(item.manufacturer)} · {item.series || GRANULARITY_LABELS[item.granularity]}</div>
        <h3><Link to={`/heat-exchangers/${item.slug}`}>{name}</Link></h3>
        <div className="confidence-row"><Check size={15} /><span>{CONFIDENCE_LABELS[item.confidence] ?? GRANULARITY_LABELS[item.granularity] ?? 'Паспортные данные'}</span></div>
        <dl className="spec-grid">
          {rows.map(({ icon: Icon, label, value }) => <div key={label}><dt><Icon size={15} />{label}</dt><dd>{value}</dd></div>)}
        </dl>
      </div>
      <div className="result-card__actions">
        <button className={`button button--secondary ${selected ? 'button--selected' : ''}`} type="button" onClick={handleCompare}>
          <GitCompareArrows size={17} /> {selected ? 'В сравнении' : 'Сравнить'}
        </button>
        <Link className="button button--primary" to={`/heat-exchangers/${item.slug}`}>Подробнее <ArrowUpRight size={17} /></Link>
      </div>
    </article>
  );
}
