import { FAMILY_LABELS } from '../api/contracts.js';

export function ExchangerVisual({ family = 'PLATE', size = 'normal' }) {
  return (
    <div className={`exchanger-visual exchanger-visual--${family.toLowerCase()} exchanger-visual--${size}`} role="img" aria-label={FAMILY_LABELS[family] ?? 'Теплообменник'}>
      <span className="exchanger-visual__body" />
      <span className="exchanger-visual__flow exchanger-visual__flow--hot" />
      <span className="exchanger-visual__flow exchanger-visual__flow--cold" />
      <span className="exchanger-visual__grid" />
    </div>
  );
}
