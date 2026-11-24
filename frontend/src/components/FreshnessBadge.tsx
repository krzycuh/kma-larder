import type { FreshnessStatus } from '../types/stock';
import { FRESHNESS_CLASSES } from '../utils/freshness';
import { t } from '../i18n';

interface FreshnessBadgeProps {
  status: FreshnessStatus;
  /** Extra text after the label, e.g. "jeszcze 12 dni" */
  detail?: string;
  className?: string;
}

/** Traffic-light pill: fine / running out / past its date / undated */
export function FreshnessBadge({ status, detail, className = '' }: FreshnessBadgeProps) {
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${FRESHNESS_CLASSES[status]} ${className}`}>
      {t.freshness.label[status]}
      {detail && <span className="opacity-80">· {detail}</span>}
    </span>
  );
}
