interface QuantityStepperProps {
  id: string;
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  label?: string;
}

/** Big −/+ buttons around a number: usable with a thumb while holding a jar in the other hand */
export function QuantityStepper({ id, value, onChange, min = 0, max = 100000, label }: QuantityStepperProps) {
  const set = (next: number) => onChange(Math.min(max, Math.max(min, next)));
  const buttonClasses =
    'h-12 w-12 rounded-lg border border-gray-300 bg-white text-xl font-semibold text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-40';
  return (
    <div className="flex items-center gap-2">
      <button type="button" className={buttonClasses} onClick={() => set(value - 1)} disabled={value <= min} aria-label="−1">
        −
      </button>
      <input
        id={id}
        inputMode="numeric"
        aria-label={label}
        value={value}
        onChange={(e) => {
          const n = Number(e.target.value.replace(/\D/g, ''));
          set(Number.isFinite(n) ? n : min);
        }}
        className="h-12 w-20 rounded-lg border border-gray-300 text-center text-xl font-semibold text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
      />
      <button type="button" className={buttonClasses} onClick={() => set(value + 1)} disabled={value >= max} aria-label="+1">
        +
      </button>
    </div>
  );
}
