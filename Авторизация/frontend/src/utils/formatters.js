const FORMATTERS = new Map();

export function formatNumber(value, maximumFractionDigits = 1) {
  if (value == null || value === '') return null;
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return String(value);
  const digits = Math.max(0, Math.min(1, maximumFractionDigits));
  const key = String(digits);
  if (!FORMATTERS.has(key)) {
    FORMATTERS.set(key, new Intl.NumberFormat('ru-RU', {
      minimumFractionDigits: 0,
      maximumFractionDigits: digits,
    }));
  }
  return FORMATTERS.get(key).format(numeric);
}

export function formatValue(value, unit = '', maximumFractionDigits = 1, empty = '—') {
  const formatted = formatNumber(value, maximumFractionDigits);
  if (formatted == null) return empty;
  return `${formatted}${unit ? ` ${unit}` : ''}`;
}

export function formatRange(min, max, unit = '', maximumFractionDigits = 1, empty = '—') {
  if ((min == null || min === '') && (max == null || max === '')) return empty;
  const left = formatNumber(min, maximumFractionDigits) ?? '—';
  const right = formatNumber(max, maximumFractionDigits) ?? '—';
  return `${left}–${right}${unit ? ` ${unit}` : ''}`;
}

export function formatDimensions(width, height, depth) {
  if ([width, height, depth].every((value) => value == null || value === '')) return '—';
  return [width, height, depth].map((value) => formatNumber(value, 0) ?? '—').join(' × ');
}
