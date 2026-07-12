import { ArrowRight, BarChart3, CheckCircle2, Database, GitCompareArrows, Layers3, Search, ShieldCheck, Wind } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { ExchangerVisual } from '../components/ExchangerVisual.jsx';

const families = [
  { family: 'PLATE', title: 'Пластинчатые', text: 'Компактные разборные и паяные модели для инженерных систем.', icon: Layers3 },
  { family: 'SHELL_AND_TUBE', title: 'Кожухотрубные', text: 'Надёжные серии для воды, пара, масел и технологических сред.', icon: Database },
  { family: 'AIR_COOLED', title: 'Воздушные', text: 'Сухие охладители, конденсаторы и газоохладители.', icon: Wind },
  { family: 'SPIRAL', title: 'Спиральные', text: 'Аппараты для загрязнённых сред и сложных тепловых режимов.', icon: BarChart3 },
];

export function LandingPage() {
  const { isAuthenticated } = useAuth();
  return (
    <>
      <section className="hero">
        <div className="hero__glow hero__glow--one" /><div className="hero__glow hero__glow--two" />
        <div className="container hero__grid">
          <div className="hero__content">
            <div className="eyebrow eyebrow--light"><span /> Инженерный поиск без догадок</div>
            <h1>Найдите теплообменник под <em>реальные ограничения</em></h1>
            <p>ThermoSelect сравнивает паспортные характеристики, исключает неизвестные значения и объясняет, почему модель попала в результат.</p>
            <div className="hero__actions">
              <Link className="button button--accent button--large" to={isAuthenticated ? '/catalog' : '/register'}>{isAuthenticated ? 'Перейти к подбору' : 'Начать подбор'} <ArrowRight /></Link>
              {!isAuthenticated && <Link className="button button--glass button--large" to="/login">Уже есть аккаунт</Link>}
            </div>
            <div className="hero__trust">
              <span><CheckCircle2 /> 42 паспортные записи</span>
              <span><CheckCircle2 /> 4 семейства аппаратов</span>
              <span><CheckCircle2 /> Источник у каждого параметра</span>
            </div>
          </div>
          <div className="hero__visual" aria-hidden="true">
            <div className="hero-device">
              <div className="hero-device__top"><span>THERMO / LIVE SELECTOR</span><i /></div>
              <ExchangerVisual family="PLATE" size="hero" />
              <div className="hero-device__metric hero-device__metric--a"><small>Рабочее давление</small><strong>30 bar</strong><span>паспортный предел</span></div>
              <div className="hero-device__metric hero-device__metric--b"><small>Совпадение</small><strong>94%</strong><span>точная конфигурация</span></div>
              <div className="hero-device__chart"><i /><i /><i /><i /><i /><i /><i /></div>
            </div>
          </div>
        </div>
      </section>

      <section className="section section--light">
        <div className="container">
          <div className="section-heading"><div><span className="eyebrow">Предметная область</span><h2>Четыре семейства — единый каталог</h2></div><p>Серии и точные конфигурации отмечены отдельно, поэтому диапазон производителя не маскируется под номинал конкретного аппарата.</p></div>
          <div className="family-grid">
            {families.map(({ family, title, text, icon: Icon }, index) => (
              <article className="family-card" key={family}>
                <div className="family-card__number">0{index + 1}</div><Icon /><h3>{title}</h3><p>{text}</p><ExchangerVisual family={family} size="small" />
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section section--ink">
        <div className="container workflow-grid">
          <div><span className="eyebrow eyebrow--light">Как работает подбор</span><h2>От требований к объяснимому результату</h2><p>Алгоритм не подменяет инженерный расчёт. Он применяет строгие ограничения, ранжирует оставшиеся модели и показывает неполноту данных.</p></div>
          <ol className="workflow-list">
            <li><span><Search /></span><div><strong>Задайте критерии</strong><p>Тип, производитель, область применения, материал, давление, температура и габариты.</p></div></li>
            <li><span><BarChart3 /></span><div><strong>Получите рейтинг</strong><p>Весовые критерии и близость параметров формируют прозрачную оценку 0–100.</p></div></li>
            <li><span><GitCompareArrows /></span><div><strong>Сравните до четырёх</strong><p>Общие и специальные характеристики выстраиваются рядом с подсветкой различий.</p></div></li>
          </ol>
        </div>
      </section>

      <section className="section">
        <div className="container proof-grid">
          <div className="proof-card"><ShieldCheck /><strong>Контролируемый доступ</strong><p>Поиск доступен USER и ADMIN. Каталог изменяет только администратор.</p></div>
          <div className="proof-card"><Database /><strong>Данные производителей</strong><p>Alfa Laval, Danfoss, Kelvion, SWEP и API Heat Transfer.</p></div>
          <div className="proof-card"><GitCompareArrows /><strong>Честное сравнение</strong><p>Неизвестные показатели остаются пустыми и не проходят строгий фильтр.</p></div>
        </div>
      </section>
    </>
  );
}
