import { useMutation, useQueryClient } from '@tanstack/react-query';
import { batchesApi } from '../api/stock';
import { useToast } from './useToast';
import { t } from '../i18n';
import { getApiErrorMessage } from '../utils/apiError';

/** Every stock change touches several lists; one place to say which. */
export function useInvalidateStock() {
  const queryClient = useQueryClient();
  return () => {
    for (const key of ['dashboard', 'products', 'product', 'expiring', 'replenishment', 'movements', 'product-search']) {
      queryClient.invalidateQueries({ queryKey: [key] });
    }
  };
}

/** Consume / discard / adjust / remove a batch, with toasts and list refresh */
export function useBatchMutations(onDone?: () => void) {
  const toast = useToast();
  const invalidate = useInvalidateStock();
  const settle = (message: string) => () => {
    invalidate();
    toast.success(message);
    onDone?.();
  };
  const fail = (err: unknown) => toast.error(getApiErrorMessage(err));

  const consume = useMutation({
    mutationFn: ({ id, quantity }: { id: string; quantity: number }) => batchesApi.consume(id, { quantity }),
    onSuccess: settle(t.batch.consumed),
    onError: fail,
  });
  const discard = useMutation({
    mutationFn: ({ id, quantity }: { id: string; quantity: number }) => batchesApi.discard(id, { quantity }),
    onSuccess: settle(t.batch.discarded),
    onError: fail,
  });
  const adjust = useMutation({
    mutationFn: ({ id, quantity }: { id: string; quantity: number }) => batchesApi.adjust(id, { quantity }),
    onSuccess: settle(t.batch.adjusted),
    onError: fail,
  });
  const remove = useMutation({
    mutationFn: (id: string) => batchesApi.delete(id),
    onSuccess: settle(t.batch.removed),
    onError: fail,
  });

  return { consume, discard, adjust, remove, isPending: consume.isPending || discard.isPending || adjust.isPending || remove.isPending };
}
