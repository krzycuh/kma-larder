import { useEffect, useState } from 'react';
import type { Product } from '../types/stock';
import { CATEGORY_ICONS } from '../utils/categories';

type ThumbSize = 'sm' | 'md' | 'lg';

const SIZE_CLASSES: Record<ThumbSize, { box: string; icon: string }> = {
  sm: { box: 'h-10 w-10 rounded-md', icon: 'text-2xl' },
  md: { box: 'h-14 w-14 rounded-lg', icon: 'text-3xl' },
  lg: { box: 'h-24 w-24 rounded-lg', icon: 'text-5xl' },
};

interface ProductThumbProps {
  product: Pick<Product, 'category' | 'imageUrl'> & { name?: string };
  size?: ThumbSize;
  className?: string;
}

/**
 * A product's picture when it has a link to one, the category pictogram otherwise – also when the picture
 * does not load (the link went stale, no network). Decorative: lists already name the product next to it.
 */
export function ProductThumb({ product, size = 'sm', className = '' }: ProductThumbProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const url = product.imageUrl;
  useEffect(() => setFailedUrl(null), [url]);
  const classes = SIZE_CLASSES[size];
  const showImage = !!url && failedUrl !== url;

  return (
    <span className={`inline-flex shrink-0 items-center justify-center overflow-hidden bg-gray-100 ${classes.box} ${className}`} aria-hidden="true">
      {showImage ? (
        <img src={url} alt="" loading="lazy" decoding="async" className="h-full w-full object-cover" onError={() => setFailedUrl(url)} />
      ) : (
        <span className={classes.icon}>{CATEGORY_ICONS[product.category]}</span>
      )}
    </span>
  );
}
