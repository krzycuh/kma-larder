import { t } from '../i18n';
import type { ApproximateDateInputValue } from '../utils/approximateDate';

interface ApproximateDateInputProps {
  id: string;
  label: string;
  hint?: string;
  value: ApproximateDateInputValue;
  onChange: (value: ApproximateDateInputValue) => void;
  error?: string | null;
  /** Rendered next to the label, e.g. the OCR button */
  action?: React.ReactNode;
}

const fieldClasses =
  'rounded-md border border-gray-300 px-2 py-2 text-center text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

/**
 * Day / month / year as three small fields, filled from the right: the year alone is enough ("2027"),
 * a month makes it "05.2027", a day a full date. Mirrors how dates are printed on packaging.
 */
export function ApproximateDateInput({ id, label, hint, value, onChange, error, action }: ApproximateDateInputProps) {
  const digits = (raw: string, max: number) => raw.replace(/\D/g, '').slice(0, max);
  return (
    <div>
      <div className="flex items-end justify-between gap-2">
        <label htmlFor={`${id}-year`} className="block text-sm font-medium text-gray-700">
          {label} <span className="text-gray-400">({t.common.optional})</span>
        </label>
        {action}
      </div>
      <div className="mt-1 flex items-center gap-1.5" role="group" aria-labelledby={`${id}-year`}>
        <input
          id={`${id}-day`}
          inputMode="numeric"
          placeholder={t.batch.dateDay}
          aria-label={t.batch.day}
          value={value.day}
          onChange={(e) => onChange({ ...value, day: digits(e.target.value, 2) })}
          className={`${fieldClasses} w-16`}
        />
        <span className="text-gray-400">.</span>
        <input
          id={`${id}-month`}
          inputMode="numeric"
          placeholder={t.batch.dateMonth}
          aria-label={t.batch.month}
          value={value.month}
          onChange={(e) => onChange({ ...value, month: digits(e.target.value, 2) })}
          className={`${fieldClasses} w-20`}
        />
        <span className="text-gray-400">.</span>
        <input
          id={`${id}-year`}
          inputMode="numeric"
          placeholder={t.batch.dateYear}
          aria-label={t.batch.year}
          value={value.year}
          onChange={(e) => onChange({ ...value, year: digits(e.target.value, 4) })}
          className={`${fieldClasses} w-24`}
        />
        {(value.day || value.month || value.year) && (
          <button
            type="button"
            onClick={() => onChange({ day: '', month: '', year: '' })}
            className="ml-1 rounded-md px-2 py-2 text-sm text-gray-500 hover:bg-gray-100 hover:text-gray-900"
            aria-label={`${t.common.delete}: ${label}`}
          >
            ✕
          </button>
        )}
      </div>
      {error ? <p className="mt-1 text-sm text-red-700">{error}</p> : hint ? <p className="mt-1 text-xs text-gray-500">{hint}</p> : null}
    </div>
  );
}
