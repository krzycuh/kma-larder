import { Link } from 'react-router-dom';
import { BatchActions } from './BatchActions';
import { FreshnessBadge } from './FreshnessBadge';
import { t } from '../i18n';
import type { Batch, Product } from '../types/stock';
import { formatApproximateDate, formatDate } from '../utils/format';
import { describeDaysLeft, FRESHNESS_BORDER_CLASSES } from '../utils/freshness';
import { ProductThumb } from './ProductThumb';

interface BatchCardProps {
  batch: Batch;
  product: Product;
  /** Show the product name (lists across products) or not (product page) */
  showProduct?: boolean;
  /** Extra controls (edit) on the right of the header */
  extra?: React.ReactNode;
}

/**
 * One batch: how many units, until when, where – and the buttons to use them up.
 */
export function BatchCard({ batch, product, showProduct = false, extra }: BatchCardProps) {
  const dateLine =
    batch.bestBefore ? t.batch.expiresOn(formatApproximateDate(batch.bestBefore)) : batch.expiresOn ? t.batch.expiresOn(formatDate(batch.expiresOn)) : null;
  return (
    <li className={`rounded-lg border-l-4 bg-white p-3 shadow-sm sm:p-4 ${FRESHNESS_BORDER_CLASSES[batch.freshness]} ${batch.quantity === 0 ? 'opacity-60' : ''}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          {showProduct && (
            <Link to={`/products/${product.id}`} className="flex items-center gap-2 font-semibold text-gray-900 hover:text-blue-700">
              <ProductThumb product={product} className="h-8 w-8" />
              <span className="min-w-0 truncate">
                {product.name}
                {product.packageSize && <span className="ml-1 text-sm font-normal text-gray-500">{product.packageSize}</span>}
              </span>
            </Link>
          )}
          <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-gray-700">
            <span className="text-2xl font-bold text-gray-900">{batch.quantity}</span>
            <span className="text-gray-500">szt.</span>
            {dateLine && <span>{dateLine}</span>}
            {batch.producedOn && <span className="text-gray-500">{t.batch.producedLabel(formatApproximateDate(batch.producedOn))}</span>}
            {batch.location && <span className="text-gray-500">📍 {batch.location}</span>}
          </p>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            <FreshnessBadge status={batch.freshness} detail={batch.expiresOn ? describeDaysLeft(batch.daysLeft) : undefined} />
            {batch.notes && <span className="truncate text-xs text-gray-500">{batch.notes}</span>}
          </div>
        </div>
        {extra}
      </div>
      <div className="mt-3">
        <BatchActions batch={batch} productName={product.name} variant={showProduct ? 'compact' : 'full'} />
      </div>
    </li>
  );
}
