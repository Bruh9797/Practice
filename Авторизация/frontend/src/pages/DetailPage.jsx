import { useEffect, useState } from 'react';
import { ArrowLeft, ExternalLink, FileCheck2, GitCompareArrows, Info, Ruler, Tag, Thermometer, Waves } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { apiFetch, getErrorMessage } from '../api/client.js';
import { FAMILY_LABELS, GRANULARITY_LABELS, labelOf, manufacturerName } from '../api/contracts.js';
import { ProductPhoto } from '../components/ProductPhoto.jsx';
import { LoadingScreen } from '../components/LoadingScreen.jsx';
import { useCompare } from '../context/CompareContext.jsx';
import { formatValue } from '../utils/formatters.js';

function shown(value, unit = '', maximumFractionDigits = 1) {
  return formatValue(value, unit, maximumFractionDigits, '—');
}

export function DetailPage() {
  const { slug } = useParams();
  const compare = useCompare();
  const [item, setItem] = useState(null);
  const [error, setError] = useState('');
  const [warning, setWarning] = useState('');

  useEffect(() => {
    setItem(null); setError('');
    apiFetch(`/api/heat-exchangers/${encodeURIComponent(slug)}`).then(setItem).catch((e) => setError(getErrorMessage(e)));
  }, [slug]);

  if (error) return <section className="page-section container"><div className="empty-state"><Info /><h1>Карточка недоступна</h1><p>{error}</p><Link className="button button--primary" to="/catalog">Вернуться в каталог</Link></div></section>;
  if (!item) return <LoadingScreen label="Загружаем паспорт аппарата…" />;

  const name = item.name || item.model || item.configuration || item.seriesName || item.series;
  const specs = [
    ['surfaceAreaM2', 'Площадь теплообмена', item.surfaceAreaM2, 'м²', 1],
    ['flowMinM3h', 'Расход, от', item.flowMinM3h, 'м³/ч', 1], ['flowMaxM3h', 'Расход, до', item.flowMaxM3h, 'м³/ч', 1],
    ['temperatureMinC', 'Температура, от', item.temperatureMinC, '°C', 1], ['temperatureMaxC', 'Температура, до', item.temperatureMaxC, '°C', 1],
    ['pressureMaxBar', 'Максимальное давление', item.pressureMaxBar, 'bar', 1],
    ['massKg', 'Масса', item.massKg, 'кг', 1], ['widthMm', 'Ширина', item.widthMm, 'мм', 0],
    ['heightMm', 'Высота', item.heightMm, 'мм', 0], ['depthMm', 'Глубина / длина', item.depthMm, 'мм', 0],
  ];

  function toggleCompare() {
    if (compare.toggle(item.id) === 'limit') setWarning('В сравнении уже четыре модели. Удалите одну из них.');
    else setWarning('');
  }

  return (
    <section className="detail-page">
      <div className="detail-hero"><div className="container"><Link className="back-link" to="/catalog"><ArrowLeft /> Назад к результатам</Link><div className="detail-hero__grid"><div><div className="eyebrow eyebrow--light">{manufacturerName(item.manufacturer)} / {item.seriesName ?? item.series}</div><h1>{name}</h1><p>{item.summary || 'Паспортная запись из официального каталога производителя.'}</p><div className="detail-tags"><span>{FAMILY_LABELS[item.family] ?? item.family}</span><span>{GRANULARITY_LABELS[item.granularity] ?? item.granularity}</span></div><div className="detail-actions"><button className={`button button--accent ${compare.has(item.id) ? 'button--selected' : ''}`} onClick={toggleCompare}><GitCompareArrows /> {compare.has(item.id) ? 'Убрать из сравнения' : 'Добавить к сравнению'}</button>{compare.ids.length >= 2 && <Link className="button button--glass" to="/compare">Открыть сравнение</Link>}</div>{warning && <p className="detail-warning">{warning}</p>}</div><ProductPhoto item={item} size="detail" loading="eager" /></div></div></div>

      <div className="container detail-layout">
        <article className="detail-main">
          <section className="detail-section"><div className="detail-section__title"><Ruler /><div><span>Основные параметры</span><h2>Технические характеристики</h2></div></div><dl className="detail-specs">{specs.map(([field, label, value, unit, digits]) => <div className={value == null ? 'unknown' : ''} key={field}><dt>{label}</dt><dd>{shown(value, unit, digits)}</dd></div>)}</dl></section>
          {item.facts?.length > 0 && <section className="detail-section"><div className="detail-section__title"><Tag /><div><span>Конструкция</span><h2>Дополнительные характеристики</h2></div></div><div className="facts-list">{item.facts.map((fact) => <div key={`${fact.key}-${fact.label}`}><span>{fact.label}</span><strong>{shown(fact.value, fact.unit ?? '')}</strong></div>)}</div></section>}
          {item.pressureLimits?.length > 0 && <section className="detail-section"><div className="detail-section__title"><Thermometer /><div><span>Рабочая оболочка</span><h2>Давление в зависимости от температуры</h2></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>Температура</th><th>Макс. давление</th><th>Основание</th></tr></thead><tbody>{item.pressureLimits.map((limit, index) => <tr key={index}><td>{shown(limit.temperatureC, '°C')}</td><td>{shown(limit.maxPressureBar, 'bar')}</td><td>{limit.note || 'Паспорт производителя'}</td></tr>)}</tbody></table></div></section>}
        </article>

        <aside className="detail-sidebar">
          <div className="source-panel"><div className="source-panel__head"><FileCheck2 /><div><span>Происхождение данных</span><strong>{item.sources?.length ?? 0} источника</strong></div></div>{item.sources?.map((source, index) => <a key={source.url} href={source.url} target="_blank" rel="noreferrer"><span><strong>{source.title || `Источник ${index + 1}`}</strong><small>{source.checkedOn ? `Проверено ${source.checkedOn}` : 'Официальный каталог'}</small>{source.measurementBasis && <small className="source-basis">{source.measurementBasis}</small>}</span><ExternalLink /></a>)}{(!item.sources || item.sources.length === 0) && <p>Источник не указан.</p>}</div>
          <div className="side-panel"><h3><Waves /> Применение</h3><div className="chip-list">{item.applications?.map((entry) => <span key={entry.code ?? entry}>{labelOf(entry)}</span>)}</div><h3><Tag /> Материалы</h3><div className="chip-list">{item.materials?.map((entry) => <span key={entry.code ?? entry}>{labelOf(entry)}</span>)}</div></div>
        </aside>
      </div>
    </section>
  );
}
