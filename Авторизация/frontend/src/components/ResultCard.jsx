import { ArrowUpRight, Check, GitCompareArrows, Gauge, Layers3, Ruler, Thermometer, Waves } from 'lucide-react';
import { Link } from 'react-router-dom';
import { CONFIDENCE_LABELS, FAMILY_LABELS, GRANULARITY_LABELS, manufacturerName } from '../api/contracts.js';
import { useCompare } from '../context/CompareContext.jsx';
import { ExchangerVisual } from './ExchangerVisual.jsx';

function spec(item, ...keys) {
  for (const key of keys) {
    const value = item?.specifications?.[key] ?? item?.[key];
    if (value != null && value !== '') return value;
  }
  return null;
}

function format(value, unit) {
  if (value == null) return '—';
  if (typeof value === 'object') {
    const min = value.min ?? value.minimum;
    const max = value.max ?? value.maximum;
    if (min != null && max != null) return `${min}–${max} ${value.unit ?? unit}`;
    return `${value.value ?? max ?? min ?? '—'} ${value.unit ?? unit}`.trim();
  }
  return `${value} ${unit}`.trim();
}

export function ResultCard({ item, view = 'cards', onCompareLimit }) {
  const compare = useCompare();
  const selected = compare.has(item.id);
  const name = item.name || [item.series, item.model, item.configuration].filter(Boolean).join(' ') || 'Без названия';

  function handleCompare() {
    if (compare.toggle(item.id) === 'limit') onCompareLimit?.();
  }

  const rows = [
    { icon: Gauge, label: 'Давление', value: format(spec(item, 'pressureMaxBar', 'maxPressureBar', 'maxWorkingPressureBar'), 'bar') },
    { icon: Thermometer, label: 'Температура', value: format(spec(item, 'temperatureMaxC', 'maxTemperatureC', 'maxOperatingTemperatureC'), '°C') },
    { icon: Waves, label: 'Расход', value: format(spec(item, 'flowMaxM3h', 'maxFlowM3h', 'maxWaterFlowM3h'), 'м³/ч') },
    { icon: Layers3, label: 'Площадь', value: format(spec(item, 'surfaceAreaM2', 'areaM2', 'heatTransferAreaM2'), 'м²') },
  ];

  if (view === 'table') {
    return (
      <tr>
        <td><span className={`family-dot family-dot--${item.family?.toLowerCase()}`} />{FAMILY_LABELS[item.family] ?? item.family}</td>
        <td><strong>{name}</strong><small>{manufacturerName(item.manufacturer)}</small></td>
        <td>{GRANULARITY_LABELS[item.granularity] ?? item.granularity}</td>
        <td>{rows[0].value}</td><td>{rows[1].value}</td><td>{rows[2].value}</td><td>{rows[3].value}</td>
        <td>{item.score != null ? <span className="score-pill">{Math.round(item.score)}%</span> : '—'}</td>
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
        <ExchangerVisual family={item.family} />
        <span className="family-badge">{FAMILY_LABELS[item.family] ?? item.family}</span>
        {item.score != null && <span className="score-badge"><strong>{Math.round(item.score)}%</strong><small>совпадение</small></span>}
      </div>
      <div className="result-card__body">
        <div className="eyebrow">{manufacturerName(item.manufacturer)} · {item.series || GRANULARITY_LABELS[item.granularity]}</div>
        <h3><Link to={`/heat-exchangers/${item.slug}`}>{name}</Link></h3>
        <div className="confidence-row"><Check size={15} /><span>{CONFIDENCE_LABELS[item.confidence] ?? GRANULARITY_LABELS[item.granularity] ?? 'Паспортные данные'}</span>{item.containsMockData && <span className="mock-badge">есть DEMO</span>}</div>
        <dl className="spec-grid">
          {rows.map(({ icon: Icon, label, value }) => <div key={label}><dt><Icon size={15} />{label}</dt><dd>{value}</dd></div>)}
        </dl>
        {(item.reasons ?? item.matchReasons)?.length > 0 && <ul className="match-reasons">{(item.reasons ?? item.matchReasons).slice(0, 2).map((reason) => <li key={reason}>{reason}</li>)}</ul>}
        {item.missingFields?.length > 0 && <p className="missing-note"><Ruler size={14} /> Нет данных: {item.missingFields.slice(0, 2).join(', ')}</p>}
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
