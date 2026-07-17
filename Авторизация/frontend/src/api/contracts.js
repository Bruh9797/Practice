/**
 * @typedef {'USER'|'ADMIN'} UserRole
 * @typedef {{id:number, username:string, email:string|null, role:UserRole, enabled:boolean, createdAt:string}} UserResponse
 * @typedef {{timestamp:string, status:number, error:string, code:string, message:string, details:string[], path:string}} ApiError
 * @typedef {'PLATE'|'SHELL_AND_TUBE'|'AIR_COOLED'|'SPIRAL'} ExchangerFamily
 * @typedef {'EXACT_CONFIGURATION'|'STANDARD_MODEL'|'SERIES'} RecordGranularity
 * @typedef {'DRAFT'|'PUBLISHED'|'ARCHIVED'} PublicationStatus
 * @typedef {{id:number, version:number, name:string, websiteUrl?:string, country?:string}} Manufacturer
 * @typedef {{code:string, name?:string, label?:string}} LookupOption
 * @typedef {{families:LookupOption[], applications:LookupOption[], materials:LookupOption[], manufacturers:Manufacturer[]}} CatalogLookups
 * @typedef {{query?:string, families?:ExchangerFamily[], manufacturerIds?:number[], applicationCodes?:string[], materialCodes?:string[], requiredSurfaceAreaM2?:number, requiredFlowM3h?:number, requiredTemperatureC?:number, requiredPressureBar?:number, page?:number, size?:number}} SearchRequest
 * @typedef {{id:number, slug:string, manufacturer:Manufacturer, model:string, seriesName?:string, family:ExchangerFamily, granularity:RecordGranularity, summary?:string, score:number, confidence?:string, completeness:number, reasons:string[], surfaceAreaM2?:number, flowMinM3h?:number, flowMaxM3h?:number, temperatureMinC?:number, temperatureMaxC?:number, pressureMaxBar?:number, widthMm?:number, heightMm?:number, depthMm?:number, massKg?:number, applications:LookupOption[], materials:LookupOption[]}} SearchItem
 * @typedef {{items:SearchItem[], page:number, size:number, totalElements:number, totalPages:number, excludedUnknownCount:number}} SearchPage
 * @typedef {{key:string, label:string, value:string, unit?:string}} TechnicalFact
 * @typedef {{title:string, url:string, checkedOn:string, measurementBasis:string}} CatalogSource
 * @typedef {{temperatureC:number, maxPressureBar:number, note?:string}} PressureLimit
 * @typedef {{id:number, version:number, slug:string, manufacturer:Manufacturer, model:string, seriesName?:string, family:ExchangerFamily, granularity:RecordGranularity, status:PublicationStatus, summary?:string, surfaceAreaM2?:number, flowMinM3h?:number, flowMaxM3h?:number, temperatureMinC?:number, temperatureMaxC?:number, pressureMaxBar?:number, widthMm?:number, heightMm?:number, depthMm?:number, massKg?:number, applications:LookupOption[], materials:LookupOption[], facts:TechnicalFact[], sources:CatalogSource[], pressureLimits:PressureLimit[]}} HeatExchangerDetail
 * @typedef {{version?:number, manufacturerId:number, slug:string, family:ExchangerFamily, model:string, seriesName?:string, granularity:RecordGranularity, status:PublicationStatus, summary?:string, applicationCodes:string[], materialCodes:string[], surfaceAreaM2?:number|null, flowMinM3h?:number|null, flowMaxM3h?:number|null, temperatureMinC?:number|null, temperatureMaxC?:number|null, pressureMaxBar?:number|null, widthMm?:number|null, heightMm?:number|null, depthMm?:number|null, massKg?:number|null, facts:TechnicalFact[], sources:CatalogSource[], pressureLimits:PressureLimit[]}} CatalogRecordInput
 */

export const FAMILY_LABELS = {
  PLATE: 'Пластинчатый',
  SHELL_AND_TUBE: 'Кожухотрубный',
  AIR_COOLED: 'Воздушный',
  SPIRAL: 'Спиральный',
};

export const GRANULARITY_LABELS = {
  EXACT_CONFIGURATION: 'Точная конфигурация',
  STANDARD_MODEL: 'Стандартная модель',
  SERIES: 'Серия под заказ',
};

export const STATUS_LABELS = {
  DRAFT: 'Черновик',
  PUBLISHED: 'Опубликован',
  ARCHIVED: 'В архиве',
};

export const CONFIDENCE_LABELS = {
  CONFIRMED: 'Подтверждено паспортом',
  EXACT: 'Точная конфигурация',
  STANDARD_MODEL: 'Стандартная модель',
  SERIES_RANGE: 'Диапазон серии',
  NEEDS_VERIFICATION: 'Требует уточнения',
};

export function labelOf(option) {
  if (typeof option === 'string') return option;
  return option?.label ?? option?.name ?? option?.code ?? '—';
}

export function manufacturerName(value) {
  return typeof value === 'string' ? value : value?.name ?? 'Не указан';
}

export function numberValue(value) {
  if (value === '' || value == null) return undefined;
  const parsed = Number(String(value).replace(',', '.'));
  return Number.isFinite(parsed) ? parsed : undefined;
}
