const ROOT = '/catalog-images';

const PHOTO_RULES = [
  [/^alfa-laval-t2-/, 'alfa-t2.webp'],
  [/^alfa-laval-t5-bfg-10-/, 'alfa-fast-track.webp'],
  [/^alfa-laval-t5-/, 'alfa-t5.webp'],
  [/^alfa-laval-t6-/, 'alfa-t6.webp'],
  [/^danfoss-b3-/, 'danfoss-b3.jpg'],
  [/^kelvion-nx-/, 'kelvion-nx.jpg'],
  [/^swep-/, 'swep-all-stainless.png'],
  [/^basco-type-500$/, 'basco-500.gif'],
  [/^basco-(bw|bws)$/, 'basco-utube.gif'],
  [/^basco-type-op$/, 'basco-op.gif'],
  [/^basco-type-(ht|aht)$/, 'basco-hub.gif'],
  [/^kelvion-lf-drycooler$/, 'kelvion-lf.jpg'],
  [/^kelvion-lf-s-drycooler$/, 'kelvion-lfs.jpg'],
  [/^kelvion-(rf-s-condenser|gf-s-gas-cooler)$/, 'kelvion-rfgfs.jpg'],
  [/^kelvion-lv-m-drycooler$/, 'kelvion-lvm.jpg'],
  [/^kelvion-rv-t-condenser$/, 'kelvion-rvt.jpg'],
  [/^alfa-laval-she-/, 'alfa-spiral.png'],
  [/^ridan-nn-/, 'ridan-nn.png'],
  [/^chzto-/, 'chzto-shell-photo.png'],
];

const FAMILY_FALLBACKS = {
  PLATE: 'kelvion-nx.jpg',
  SHELL_AND_TUBE: 'basco-500.gif',
  AIR_COOLED: 'kelvion-lf.jpg',
  SPIRAL: 'alfa-spiral.png',
};

export function productPhotoUrl(item) {
  const slug = item?.slug ?? '';
  const match = PHOTO_RULES.find(([pattern]) => pattern.test(slug));
  const file = match?.[1] ?? FAMILY_FALLBACKS[item?.family];
  return file ? `${ROOT}/${file}` : null;
}

export function productPhotoAlt(item) {
  const manufacturer = typeof item?.manufacturer === 'string'
    ? item.manufacturer
    : item?.manufacturer?.name;
  const model = item?.model ?? item?.name ?? item?.seriesName ?? item?.series;
  return ['Теплообменник', manufacturer, model].filter(Boolean).join(' ');
}
