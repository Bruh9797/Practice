import { describe, expect, it } from 'vitest';
import { toSearchRequest } from './SearchFilters.jsx';

describe('toSearchRequest', () => {
  it('переводит URL-состояние в строгий контракт API', () => {
    expect(toSearchRequest({
      query: ' NX80M ', family: 'PLATE', manufacturerId: '3', applicationCode: 'HEATING', materialCode: 'STAINLESS_STEEL',
      requiredPressureBar: '12,5', requiredTemperatureC: '150', requiredFlowM3h: '80', requiredSurfaceAreaM2: '', page: 2, size: 12,
    })).toEqual({
      query: 'NX80M', families: ['PLATE'], manufacturerIds: [3], applicationCodes: ['HEATING'], materialCodes: ['STAINLESS_STEEL'],
      requiredPressureBar: 12.5, requiredTemperatureC: 150, requiredFlowM3h: 80, page: 2, size: 12,
    });
  });

  it('не отправляет пустые и некорректные числовые значения', () => {
    const result = toSearchRequest({ page: 0, size: 20, requiredPressureBar: '', requiredFlowM3h: 'abc' });
    expect(result).toEqual({ page: 0, size: 20 });
  });
});
