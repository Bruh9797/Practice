import { describe, expect, it } from 'vitest';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { productPhotoAlt, productPhotoUrl } from './productPhotos.js';

const catalogSlugs = [
  'alfa-laval-t2-bfg-15-8240125735', 'alfa-laval-t5-bfg-10-8240124306',
  'alfa-laval-t5-bfg-20m-8240171459', 'alfa-laval-t5-bfg-35m-8240171464',
  'alfa-laval-t5-mfg-50m-8240171452', 'alfa-laval-t6-pfg-15-8240120945',
  'alfa-laval-t6-bfg-15m-8240152969', 'alfa-laval-t6-bfg-70m-8240152981',
  'danfoss-b3-012-40-111b6315', 'danfoss-b3-113-78-111b6015',
  'danfoss-b3-113-110-111b0321', 'danfoss-b3-113-90-111b0320',
  'danfoss-b3-095b-101-111b0103', 'danfoss-b3-027-14-111b1131',
  'kelvion-nx-25m', 'kelvion-nx-50m', 'kelvion-nx-100x', 'kelvion-nx-150l',
  'kelvion-nx-250l', 'kelvion-nx-400x', 'swep-b5t-all-stainless',
  'swep-b10ts-all-stainless', 'swep-b15t-all-stainless', 'swep-b80s-all-stainless',
  'basco-type-500', 'basco-bw', 'basco-bws', 'basco-type-op', 'basco-type-ht',
  'basco-type-aht', 'kelvion-lf-drycooler', 'kelvion-lf-s-drycooler',
  'kelvion-rf-s-condenser', 'kelvion-gf-s-gas-cooler', 'kelvion-lv-m-drycooler',
  'kelvion-rv-t-condenser', 'alfa-laval-she-ltl-2s', 'alfa-laval-she-ltl-4l',
  'alfa-laval-she-ltl-8l', 'alfa-laval-she-ltl-30l', 'alfa-laval-she-cond-1s',
  'alfa-laval-she-cond-14l', 'ridan-nn-04', 'ridan-nn-08', 'ridan-nn-14',
  'ridan-nn-21-22', 'chzto-tng-159-16-1000', 'chzto-tnv-273-16-2000',
  'chzto-hng-400-16-3000', 'chzto-tpg-600-10-3000',
];

describe('productPhotoUrl', () => {
  it('provides a local image for every seeded catalog record', () => {
    for (const slug of catalogSlugs) {
      const url = productPhotoUrl({ slug, family: 'PLATE' });
      expect(url, slug).toMatch(/^\/catalog-images\/.+\.(?:jpg|png|gif|webp)$/);
      expect(existsSync(path.resolve('public', url.slice(1))), slug).toBe(true);
    }
  });

  it('uses a family fallback for an administrator-created record', () => {
    expect(productPhotoUrl({ slug: 'new-record', family: 'SPIRAL' })).toBe('/catalog-images/alfa-spiral.png');
  });

  it('builds an accessible description', () => {
    expect(productPhotoAlt({ manufacturer: { name: 'Ридан' }, model: 'НН№08' })).toBe('Теплообменник Ридан НН№08');
  });
});
