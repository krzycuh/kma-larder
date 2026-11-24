import { describe, expect, it } from 'vitest';
import { formatApproximateDate, formatDate } from '../format';

describe('format utils', () => {
  it('formats a date with month precision', () => {
    expect(formatApproximateDate({ year: 2027, month: 5, day: null })).toBe('maj 2027');
  });

  it('formats a date with year precision', () => {
    expect(formatApproximateDate({ year: 2027, month: null, day: null })).toBe('2027');
  });

  it('formats a full date', () => {
    expect(formatApproximateDate({ year: 2027, month: 5, day: 12 })).toBe('12 maj 2027');
  });

  it('formats a missing date', () => {
    expect(formatApproximateDate(null)).toBe('nie podano');
  });

  it('formats ISO date', () => {
    expect(formatDate('2024-03-05')).toMatch(/5 mar 2024/);
    expect(formatDate(null)).toBe('—');
  });
});
