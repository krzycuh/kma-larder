import { useCurrentPantry } from '../hooks/usePantries';
import { t } from '../i18n';

/**
 * Picks the pantry the stock pages work on. Hidden when the user has only their own pantry.
 */
export function PantrySwitcher() {
  const { pantries, pantry, setPantry } = useCurrentPantry();
  if (pantries.length < 2 || !pantry) return null;

  return (
    <div className="flex items-center gap-2">
      <label htmlFor="pantry-switcher" className="text-sm font-medium text-gray-700">
        {t.pantry.switcherLabel}
      </label>
      <select
        id="pantry-switcher"
        value={pantry.id}
        onChange={(e) => setPantry(e.target.value)}
        className="min-w-0 flex-1 rounded-md border border-gray-300 bg-white px-3 py-2 text-base text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 sm:flex-none"
      >
        {pantries.map((item) => (
          <option key={item.id} value={item.id}>
            {item.name}
            {item.memberCount > 1 ? ` (${item.memberCount})` : ''}
          </option>
        ))}
      </select>
    </div>
  );
}
