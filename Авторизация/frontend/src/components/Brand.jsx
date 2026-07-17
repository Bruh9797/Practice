import { Link } from 'react-router-dom';

export function Brand({ compact = false }) {
  return (
    <Link className={`brand ${compact ? 'brand--compact' : ''}`} to="/" aria-label="ThermoSelect — главная">
      <span className="brand__mark" aria-hidden="true">
        <span />
        <span />
        <span />
      </span>
      <span className="brand__text">Thermo<span>Select</span></span>
    </Link>
  );
}
