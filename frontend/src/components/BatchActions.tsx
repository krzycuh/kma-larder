import { useState } from 'react';
import { ConfirmationModal } from './ConfirmationModal';
import { QuantityModal } from './QuantityModal';
import { useBatchMutations } from '../hooks/useStockMutations';
import { t } from '../i18n';
import type { Batch } from '../types/stock';

interface BatchActionsProps {
  batch: Batch;
  productName: string;
  /** compact: "Zużyj 1" and a menu of the rest; full: every action as a button (product page) */
  variant?: 'compact' | 'full';
}

type Pending = 'consume' | 'discard' | 'adjust' | 'remove' | null;

/**
 * The verbs of a batch: eat one, eat many, throw away, correct the count, remove the batch.
 */
export function BatchActions({ batch, productName, variant = 'full' }: BatchActionsProps) {
  const [pending, setPending] = useState<Pending>(null);
  const mutations = useBatchMutations(() => setPending(null));
  const inStock = batch.quantity > 0;

  const smallButton =
    'rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50';
  const primaryButton =
    'rounded-md bg-blue-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50';

  return (
    <div className="flex flex-wrap items-center gap-2">
      {inStock && (
        <button
          type="button"
          className={primaryButton}
          disabled={mutations.isPending}
          onClick={() => mutations.consume.mutate({ id: batch.id, quantity: 1 })}
        >
          {t.batch.consumeOne}
        </button>
      )}
      {inStock && batch.quantity > 1 && (
        <button type="button" className={smallButton} disabled={mutations.isPending} onClick={() => setPending('consume')}>
          {t.batch.consume}…
        </button>
      )}
      {inStock && (
        <button type="button" className={smallButton} disabled={mutations.isPending} onClick={() => setPending('discard')}>
          {t.batch.discard}
        </button>
      )}
      {variant === 'full' && (
        <>
          <button type="button" className={smallButton} disabled={mutations.isPending} onClick={() => setPending('adjust')}>
            {t.batch.adjust}
          </button>
          <button
            type="button"
            className={`${smallButton} text-red-700 hover:bg-red-50`}
            disabled={mutations.isPending}
            onClick={() => setPending('remove')}
          >
            {t.common.delete}
          </button>
        </>
      )}

      <QuantityModal
        open={pending === 'consume'}
        title={`${t.batch.consume}: ${productName}`}
        initial={1}
        min={1}
        max={batch.quantity}
        confirmLabel={t.batch.consume}
        isLoading={mutations.consume.isPending}
        onClose={() => setPending(null)}
        onConfirm={(quantity) => mutations.consume.mutate({ id: batch.id, quantity })}
      />
      <QuantityModal
        open={pending === 'discard'}
        title={t.batch.discardTitle}
        description={t.batch.discardText(batch.quantity, productName)}
        initial={batch.quantity}
        min={1}
        max={batch.quantity}
        confirmLabel={t.batch.discard}
        variant="danger"
        isLoading={mutations.discard.isPending}
        onClose={() => setPending(null)}
        onConfirm={(quantity) => mutations.discard.mutate({ id: batch.id, quantity })}
      />
      <QuantityModal
        open={pending === 'adjust'}
        title={t.batch.adjustTitle}
        description={t.batch.adjustHint}
        initial={batch.quantity}
        min={0}
        confirmLabel={t.common.save}
        isLoading={mutations.adjust.isPending}
        onClose={() => setPending(null)}
        onConfirm={(quantity) => mutations.adjust.mutate({ id: batch.id, quantity })}
      />
      <ConfirmationModal
        isOpen={pending === 'remove'}
        onClose={() => setPending(null)}
        onConfirm={() => mutations.remove.mutate(batch.id)}
        title={t.batch.removeConfirmTitle}
        description={t.batch.removeConfirmText}
        confirmText={t.common.delete}
        variant="danger"
        isLoading={mutations.remove.isPending}
      />
    </div>
  );
}
