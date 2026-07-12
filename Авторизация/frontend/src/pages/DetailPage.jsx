import { useEffect, useState } from 'react';
import { ArrowLeft, CheckCircle2, ExternalLink, FileCheck2, GitCompareArrows, Info, Ruler, ShieldCheck, Tag, Thermometer, Waves } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { apiFetch, getErrorMessage } from '../api/client.js';
import { FAMILY_LABELS, GRANULARITY_LABELS, labelOf, manufacturerName } from '../api/contracts.js';
import { ExchangerVisual } from '../components/ExchangerVisual.jsx';
import { LoadingScreen } from '../components/LoadingScreen.jsx';
import { useCompare } from '../context/CompareContext.jsx';

function shown(value, unit = '') {
  if (value == null || value === '') return 'Нет данных';
  return `${value} ${unit}`.trim();
}

const MOCK_FIELD_LABELS = {
  surfaceAreaM2: 'площадь теплообмена', flowMinM3h: 'минимальный расход', flowMaxM3h: 'максимальный расход',
  powerMinKw: 'минимальная мощность', powerMaxKw: 'максимальная мощность', temperatureMinC: 'минимальная температура',
  temperatureMaxC: 'максимальная температура', pressureMinBar: 'минимальное давление', pressureMaxBar: 'максимальное давление',
  widthMm: 'ширина', heightMm: 'высота', depthMm: 'глубина', massKg: 'масса', pressureCurve: 'кривая давления',
};

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

  const name = item.name || [item.seriesName ?? item.series, item.model, item.configuration].filter(Boolean).join(' ');
  const mockFact = item.facts?.find((fact) => fact.key === 'mockFields');
  const mockFields = mockFact?.value?.split(',').filter(Boolean) ?? [];
  const mockSet = new Set(mockFields);
  const specs = [
    ['surfaceAreaM2', 'Площадь теплообмена', item.surfaceAreaM2, 'м²'],
    ['flowMinM3h', 'Расход, от', item.flowMinM3h, 'м³/ч'], ['flowMaxM3h', 'Расход, до', item.flowMaxM3h, 'м³/ч'],
    ['powerMinKw', 'Мощность серии, от', item.powerMinKw, 'кВт'], ['powerMaxKw', 'Мощность серии, до', item.powerMaxKw, 'кВт'],
    ['temperatureMinC', 'Температура, от', item.temperatureMinC, '°C'], ['temperatureMaxC', 'Температура, до', item.temperatureMaxC, '°C'],
    ['pressureMinBar', 'Давление, от', item.pressureMinBar, 'bar'], ['pressureMaxBar', 'Давление, до', item.pressureMaxBar, 'bar'],
    ['massKg', 'Масса', item.massKg, 'кг'], ['widthMm', 'Ширина', item.widthMm, 'мм'],
    ['heightMm', 'Высота', item.heightMm, 'мм'], ['depthMm', 'Глубина / длина', item.depthMm, 'мм'],
  ];

  function toggleCompare() {
    if (compare.toggle(item.id) === 'limit') setWarning('В сравнении уже четыре модели. Удалите одну из них.');
    else setWarning('');
  }

  return (
    <section className="detail-page">
      <div className="detail-hero"><div className="container"><Link className="back-link" to="/catalog"><ArrowLeft /> Назад к результатам</Link><div className="detail-hero__grid"><div><div className="eyebrow eyebrow--light">{manufacturerName(item.manufacturer)} / {item.seriesName ?? item.series}</div><h1>{name}</h1><p>{item.summary || 'Паспортная запись из официального каталога производителя.'}</p><div className="detail-tags"><span>{FAMILY_LABELS[item.family] ?? item.family}</span><span>{GRANULARITY_LABELS[item.granularity] ?? item.granularity}</span>{item.status && <span>{item.status}</span>}</div><div className="detail-actions"><button className={`button button--accent ${compare.has(item.id) ? 'button--selected' : ''}`} onClick={toggleCompare}><GitCompareArrows /> {compare.has(item.id) ? 'Убрать из сравнения' : 'Добавить к сравнению'}</button>{compare.ids.length >= 2 && <Link className="button button--glass" to="/compare">Открыть сравнение</Link>}</div>{warning && <p className="detail-warning">{warning}</p>}</div><ExchangerVisual family={item.family} size="detail" /></div></div></div>

      <div className="container detail-layout">
        <article className="detail-main">
          <section className="detail-section"><div className="detail-section__title"><Ruler /><div><span>Основные параметры</span><h2>Паспортные и демонстрационные характеристики</h2></div></div><dl className="detail-specs">{specs.map(([field, label, value, unit]) => <div className={`${value == null ? 'unknown' : ''} ${mockSet.has(field) ? 'is-mock' : ''}`} key={field}><dt>{label}{mockSet.has(field) && <span className="mock-badge">DEMO</span>}</dt><dd>{shown(value, unit)}</dd></div>)}</dl><div className={`data-note ${mockFields.length ? 'data-note--mock' : ''}`}><Info /><p><strong>Важно:</strong> {mockFields.length ? 'значения с меткой DEMO сгенерированы для проверки интерфейса и фильтров. Они не являются паспортными и не применяются для теплового расчёта.' : 'диапазон серии не является гарантированным номиналом конкретной поставки.'}</p></div></section>
          {item.facts?.length > 0 && <section className="detail-section"><div className="detail-section__title"><Tag /><div><span>Конструкция</span><h2>Специальные характеристики</h2></div></div><div className="facts-list">{item.facts.map((fact) => <div key={`${fact.key}-${fact.label}`}><span>{fact.label}</span><strong>{fact.key === 'mockFields' ? mockFields.map((field) => MOCK_FIELD_LABELS[field] ?? field).join(', ') : shown(fact.value, fact.unit)}</strong></div>)}</div></section>}
          {item.pressureLimits?.length > 0 && <section className="detail-section"><div className="detail-section__title"><Thermometer /><div><span>Рабочая оболочка</span><h2>Давление в зависимости от температуры</h2></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>Температура</th><th>Макс. давление</th><th>Основание</th></tr></thead><tbody>{item.pressureLimits.map((limit, index) => <tr key={index}><td>{shown(limit.temperatureC, '°C')}</td><td>{shown(limit.maxPressureBar, 'bar')}</td><td>{limit.note || 'Паспорт производителя'}</td></tr>)}</tbody></table></div></section>}
        </article>

        <aside className="detail-sidebar">
          <div className="source-panel"><div className="source-panel__head"><FileCheck2 /><div><span>Происхождение данных</span><strong>{item.sources?.length ?? 0} источника</strong></div></div>{item.sources?.map((source, index) => <a key={source.url} href={source.url} target="_blank" rel="noreferrer"><span><strong>{source.title || `Источник ${index + 1}`}</strong><small>{source.checkedOn ? `Проверено ${source.checkedOn}` : 'Официальный каталог'}</small>{source.measurementBasis && <small className="source-basis">{source.measurementBasis}</small>}</span><ExternalLink /></a>)}{(!item.sources || item.sources.length === 0) && <p>Источник не указан.</p>}</div>
          <div className="side-panel"><h3><ShieldCheck /> Достоверность</h3><ul><li><CheckCircle2 /> Гранулярность указана</li><li><CheckCircle2 /> Единицы приведены к СИ</li><li><CheckCircle2 /> {mockFields.length ? 'Mock-поля явно помечены' : 'Характеристики подтверждены источником'}</li></ul></div>
          <div className="side-panel"><h3><Waves /> Применение</h3><div className="chip-list">{item.applications?.map((entry) => <span key={entry.code ?? entry}>{labelOf(entry)}</span>)}</div><h3><Tag /> Материалы</h3><div className="chip-list">{item.materials?.map((entry) => <span key={entry.code ?? entry}>{labelOf(entry)}</span>)}</div></div>
        </aside>
      </div>
    </section>
  );
}
