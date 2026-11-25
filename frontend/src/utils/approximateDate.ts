import type { ApproximateDate } from '../types/stock';

/** What the three date fields hold while the user types; empty strings mean "not given". */
export interface ApproximateDateInputValue {
  day: string;
  month: string;
  year: string;
}

export const EMPTY_DATE_INPUT: ApproximateDateInputValue = { day: '', month: '', year: '' };

export function toDateInput(value: ApproximateDate | null | undefined): ApproximateDateInputValue {
  if (!value) return EMPTY_DATE_INPUT;
  return { day: value.day ? String(value.day) : '', month: value.month ? String(value.month) : '', year: String(value.year) };
}

function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

/**
 * Turns the typed fields into a date, or reports why they are not one. A two-digit year is read as 20xx.
 * A day without a month, or a month without a year, is an error rather than a guess.
 */
export function parseDateInput(input: ApproximateDateInputValue): { date: ApproximateDate | null; error: boolean } {
  const day = input.day.trim();
  const month = input.month.trim();
  const year = input.year.trim();
  if (!day && !month && !year) return { date: null, error: false };
  if (!year || !/^\d{2}$|^\d{4}$/.test(year)) return { date: null, error: true };
  const y = year.length === 2 ? 2000 + Number(year) : Number(year);
  if (y < 1990 || y > 2200) return { date: null, error: true };
  if (!month) return day ? { date: null, error: true } : { date: { year: y, month: null, day: null }, error: false };
  const m = Number(month);
  if (!/^\d{1,2}$/.test(month) || m < 1 || m > 12) return { date: null, error: true };
  if (!day) return { date: { year: y, month: m, day: null }, error: false };
  const d = Number(day);
  if (!/^\d{1,2}$/.test(day) || d < 1 || d > daysInMonth(y, m)) return { date: null, error: true };
  return { date: { year: y, month: m, day: d }, error: false };
}

/** Last day of the period, for "is it already past" checks in the form (mirrors ApproximateDate.endDate). */
export function endDateOf(value: ApproximateDate): Date {
  if (!value.month) return new Date(value.year, 11, 31);
  if (!value.day) return new Date(value.year, value.month, 0);
  return new Date(value.year, value.month - 1, value.day);
}

export function isPast(value: ApproximateDate, today = new Date()): boolean {
  const end = endDateOf(value);
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  return end < start;
}
