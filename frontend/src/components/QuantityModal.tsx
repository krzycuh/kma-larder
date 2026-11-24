import { useEffect, useState } from 'react';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/react';
import { QuantityStepper } from './QuantityStepper';
import { t } from '../i18n';

interface QuantityModalProps {
  open: boolean;
  title: string;
  description?: string;
  initial: number;
  min?: number;
  max?: number;
  confirmLabel: string;
  variant?: 'default' | 'danger';
  isLoading?: boolean;
  onClose: () => void;
  onConfirm: (quantity: number) => void;
}

/** "How many?" dialog shared by consume, discard and stock-taking */
export function QuantityModal({
  open,
  title,
  description,
  initial,
  min = 0,
  max,
  confirmLabel,
  variant = 'default',
  isLoading = false,
  onClose,
  onConfirm,
}: QuantityModalProps) {
  const [quantity, setQuantity] = useState(initial);
  useEffect(() => {
    if (open) setQuantity(initial);
  }, [open, initial]);

  const confirmClasses =
    variant === 'danger'
      ? 'bg-red-600 hover:bg-red-700 focus:ring-red-500'
      : 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500';

  return (
    <Dialog open={open} onClose={onClose} className="relative z-50">
      <div className="fixed inset-0 bg-black/40" aria-hidden="true" />
      <div className="fixed inset-0 flex items-end justify-center p-4 sm:items-center">
        <DialogPanel className="w-full max-w-sm rounded-lg bg-white p-5 shadow-xl">
          <DialogTitle className="text-lg font-semibold text-gray-900">{title}</DialogTitle>
          {description && <p className="mt-1 text-sm text-gray-600">{description}</p>}
          <div className="mt-4 flex flex-col items-center gap-2">
            <label htmlFor="quantity-modal-input" className="text-sm font-medium text-gray-700">
              {t.batch.howMany}
            </label>
            <QuantityStepper id="quantity-modal-input" value={quantity} onChange={setQuantity} min={min} max={max} label={t.batch.howMany} />
          </div>
          <div className="mt-5 flex gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              className="flex-1 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              {t.common.cancel}
            </button>
            <button
              type="button"
              onClick={() => onConfirm(quantity)}
              disabled={isLoading || quantity < min || (max !== undefined && quantity > max)}
              className={`flex-1 rounded-lg px-4 py-2 text-sm font-semibold text-white focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${confirmClasses}`}
            >
              {isLoading ? t.common.saving : confirmLabel}
            </button>
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  );
}
