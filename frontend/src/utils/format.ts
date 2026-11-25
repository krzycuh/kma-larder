import dayjs from 'dayjs';
import 'dayjs/locale/pl';
import { t } from '../i18n';
import type { ApproximateDate } from '../types/stock';

dayjs.locale('pl');

/**
 * Formats an ISO date (YYYY-MM-DD) as a human readable Polish date.
 */
export function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  return dayjs(value).format('D MMM YYYY');
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—';
  return dayjs(value).format('D MMM YYYY, HH:mm');
}

/**
 * Formats a date as printed on packaging, with whatever precision it has: "2027", "maj 2027", "12 maj 2027".
 */
export function formatApproximateDate(value: ApproximateDate | null | undefined): string {
  if (!value) return t.batch.unknownDate;
  if (!value.month) return String(value.year);
  const month = t.months[value.month - 1];
  if (!value.day) return `${month} ${value.year}`;
  return `${value.day} ${t.monthsShort[value.month - 1]} ${value.year}`;
}

export function todayIso(): string {
  return dayjs().format('YYYY-MM-DD');
}

export function currentYear(): number {
  return dayjs().year();
}
