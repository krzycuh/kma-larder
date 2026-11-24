import { describe, expect, it } from 'vitest';
import { extractDates, repairDigits } from '../dateOcr';

const today = new Date(2026, 10, 24);
const dates = (text: string) => extractDates(text, today).map((c) => c.date);

describe('extractDates', () => {
  it('reads full dates in the common European and ISO formats', () => {
    expect(dates('Najlepiej spożyć przed: 12.05.2027')).toEqual([{ year: 2027, month: 5, day: 12 }]);
    expect(dates('BEST BEFORE 12/05/27 L2345')).toEqual([{ year: 2027, month: 5, day: 12 }]);
    expect(dates('EXP 2027-05-12')).toEqual([{ year: 2027, month: 5, day: 12 }]);
    expect(dates('12 05 2027')).toEqual([{ year: 2027, month: 5, day: 12 }]);
  });

  it('reads month and year, including two-digit years', () => {
    expect(dates('Najlepiej spożyć przed końcem: 05.2027')).toEqual([{ year: 2027, month: 5, day: null }]);
    expect(dates('BBE 05/27')).toEqual([{ year: 2027, month: 5, day: null }]);
    expect(dates('2027-05')).toEqual([{ year: 2027, month: 5, day: null }]);
  });

  it('reads a lone year as the least certain candidate', () => {
    expect(dates('Ogórki 2026')).toEqual([{ year: 2026, month: null, day: null }]);
    const all = extractDates('rocznik 2026, spożyć do 06.2027', today);
    expect(all.map((c) => c.date)).toEqual([
      { year: 2027, month: 6, day: null },
      { year: 2026, month: null, day: null },
    ]);
  });

  it('repairs letters the OCR put in place of digits', () => {
    expect(repairDigits('O5.2O27 Zużyj')).toBe('05.2027 Zużyj');
    expect(repairDigits('l2.O5.2027')).toBe('12.05.2027');
    expect(repairDigits('Zużyć do O5.2O27')).toBe('Zużyć do 05.2027');
    expect(dates('O5.2O27')).toEqual([{ year: 2027, month: 5, day: null }]);
  });

  it('ignores impossible dates, far-off years and lot numbers', () => {
    expect(dates('31.02.2027')).toEqual([]);
    expect(dates('13.2027')).toEqual([]);
    expect(dates('12.05.1998')).toEqual([]);
    expect(dates('L 123456 987654321')).toEqual([]);
    expect(dates('tel. 22 123 45 67')).toEqual([]);
  });

  it('deduplicates and keeps the raw text', () => {
    const found = extractDates('12.05.2027 ... 12.05.2027', today);
    expect(found).toHaveLength(1);
    expect(found[0].raw).toBe('12.05.2027');
    expect(found[0].score).toBe(3);
  });
});
