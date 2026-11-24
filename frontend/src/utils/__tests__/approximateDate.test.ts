import { describe, expect, it } from 'vitest';
import { isPast, parseDateInput, toDateInput } from '../approximateDate';

describe('parseDateInput', () => {
  it('reads year, month+year and full dates', () => {
    expect(parseDateInput({ day: '', month: '', year: '2027' })).toEqual({ date: { year: 2027, month: null, day: null }, error: false });
    expect(parseDateInput({ day: '', month: '5', year: '27' })).toEqual({ date: { year: 2027, month: 5, day: null }, error: false });
    expect(parseDateInput({ day: '12', month: '05', year: '2027' })).toEqual({ date: { year: 2027, month: 5, day: 12 }, error: false });
  });

  it('is empty when nothing was typed', () => {
    expect(parseDateInput({ day: '', month: '', year: ' ' })).toEqual({ date: null, error: false });
  });

  it('rejects partial or impossible dates', () => {
    expect(parseDateInput({ day: '', month: '5', year: '' }).error).toBe(true);
    expect(parseDateInput({ day: '3', month: '', year: '2027' }).error).toBe(true);
    expect(parseDateInput({ day: '', month: '13', year: '2027' }).error).toBe(true);
    expect(parseDateInput({ day: '30', month: '2', year: '2027' }).error).toBe(true);
    expect(parseDateInput({ day: '', month: '', year: '1980' }).error).toBe(true);
    expect(parseDateInput({ day: '', month: '', year: '202' }).error).toBe(true);
  });

  it('round-trips through the input fields', () => {
    expect(toDateInput({ year: 2027, month: 5, day: null })).toEqual({ day: '', month: '5', year: '2027' });
    expect(toDateInput(null)).toEqual({ day: '', month: '', year: '' });
  });

  it('knows whether the period is already over', () => {
    const today = new Date(2026, 10, 24);
    expect(isPast({ year: 2026, month: null, day: null }, today)).toBe(false);
    expect(isPast({ year: 2026, month: 10, day: null }, today)).toBe(true);
    expect(isPast({ year: 2026, month: 11, day: 24 }, today)).toBe(false);
    expect(isPast({ year: 2026, month: 11, day: 23 }, today)).toBe(true);
  });
});
