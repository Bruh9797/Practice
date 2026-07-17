import { describe, expect, it } from 'vitest';
import { formatDimensions, formatNumber, formatRange, formatValue } from './formatters.js';

describe('catalog number formatting', () => {
  it('limits values to one decimal place', () => {
    expect(formatNumber(17.316)).toBe('17,3');
    expect(formatRange(0.324, 6, 'м³/ч')).toBe('0,3–6 м³/ч');
  });

  it('rounds millimetres to whole values', () => {
    expect(formatValue(190.512, 'мм', 0)).toBe('191 мм');
    expect(formatDimensions(259.08, 701.04, 170.18)).toBe('259 × 701 × 170');
  });

  it('never appends null as a unit', () => {
    expect(formatValue('DN50', null)).toBe('DN50');
  });
});
