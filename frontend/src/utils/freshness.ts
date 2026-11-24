import type { FreshnessStatus } from '../types/stock';
import { t } from '../i18n';

/** Badge colors: green / amber / red are reserved for freshness in both themes (theme.css) */
export const FRESHNESS_CLASSES: Record<FreshnessStatus, string> = {
  OK: 'bg-green-100 text-green-800',
  EXPIRING_SOON: 'bg-amber-100 text-amber-800',
  EXPIRED: 'bg-red-100 text-red-800',
  UNKNOWN: 'bg-gray-100 text-gray-600',
};

/** Left border of a list row, a glance-able traffic light */
export const FRESHNESS_BORDER_CLASSES: Record<FreshnessStatus, string> = {
  OK: 'border-l-green-500',
  EXPIRING_SOON: 'border-l-amber-500',
  EXPIRED: 'border-l-red-500',
  UNKNOWN: 'border-l-gray-300',
};

/** "jeszcze 12 dni" / "3 dni po terminie" / "bez terminu" */
export function describeDaysLeft(daysLeft: number | null): string {
  if (daysLeft === null) return t.batch.noDate;
  if (daysLeft < 0) return t.batch.daysOver(-daysLeft);
  return t.batch.daysLeft(daysLeft);
}
