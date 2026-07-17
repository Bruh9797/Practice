import { useEffect, useState } from 'react';
import { ExchangerVisual } from './ExchangerVisual.jsx';
import { productPhotoAlt, productPhotoUrl } from '../utils/productPhotos.js';

export function ProductPhoto({ item, size = 'normal', loading = 'lazy' }) {
  const source = productPhotoUrl(item);
  const [failed, setFailed] = useState(false);

  useEffect(() => setFailed(false), [source]);

  if (!source || failed) return <ExchangerVisual family={item?.family} size={size} />;

  return (
    <div className={`product-photo product-photo--${size}`}>
      <img
        src={source}
        alt={productPhotoAlt(item)}
        loading={loading}
        decoding="async"
        onError={() => setFailed(true)}
      />
    </div>
  );
}
