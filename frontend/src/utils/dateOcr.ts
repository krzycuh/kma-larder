import type { ApproximateDate } from '../types/stock';

export interface DateCandidate {
  date: ApproximateDate;
  /** The characters the date was read from, for the user to judge */
  raw: string;
  /** Higher is more certain: a full date beats month+year beats a lone year */
  score: number;
}

/** Dates on packaging fall within a few years of today; anything else is a misread */
const YEARS_BACK = 3;
const YEARS_AHEAD = 15;

function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

/**
 * OCR confuses letters and digits inside printed numbers: "O5.2O27" is 05.2027, "l2" is 12.
 * Only tokens that are mostly digits and separators are touched, so words stay words.
 */
export function repairDigits(text: string): string {
  return text.replace(/(?<!\p{L})[0-9OoIl|][0-9OoIl|.,/\-\s:]{1,15}[0-9OoIl|](?!\p{L})/gu, (token) => {
    const digitsAndLetters = token.replace(/[^0-9OoIl|]/g, '');
    const digits = token.replace(/\D/g, '');
    if (digits.length < 2 || digits.length < Math.ceil(digitsAndLetters.length * 0.5)) return token;
    return token.replace(/[Oo]/g, '0').replace(/[Il|]/g, '1');
  });
}

function fullYear(raw: string, today: Date): number | null {
  const n = Number(raw);
  const year = raw.length === 2 ? 2000 + n : n;
  if (year < today.getFullYear() - YEARS_BACK || year > today.getFullYear() + YEARS_AHEAD) return null;
  return year;
}

/**
 * Finds best-before / production dates in text read from a label. Handles the formats common on
 * Polish and EU packaging: 12.05.2027, 12/05/27, 05.2027, 05/27, 2027-05-12, 2027-05, 2027.
 * The result is deduplicated and ordered by certainty, then by date.
 */
export function extractDates(text: string, today = new Date()): DateCandidate[] {
  // Matched text is blanked out, so "12 05 2027" is one full date and not also "05 2027" and "2027"
  let rest = repairDigits(text);
  const found = new Map<string, DateCandidate>();
  const consume = (pattern: RegExp, toDate: (m: RegExpMatchArray) => ApproximateDate | null, score: number) => {
    for (const m of [...rest.matchAll(pattern)]) {
      const date = toDate(m);
      if (!date) continue;
      const key = `${date.year}-${date.month ?? ''}-${date.day ?? ''}`;
      const existing = found.get(key);
      if (!existing || existing.score < score) found.set(key, { date, raw: m[0].trim(), score });
      rest = rest.slice(0, m.index) + ' '.repeat(m[0].length) + rest.slice(m.index! + m[0].length);
    }
  };
  const fullDate = (year: number | null, month: number, day: number): ApproximateDate | null =>
    year && month >= 1 && month <= 12 && day >= 1 && day <= daysInMonth(year, month) ? { year, month, day } : null;
  const monthYear = (year: number | null, month: number): ApproximateDate | null =>
    year && month >= 1 && month <= 12 ? { year, month, day: null } : null;

  // DD.MM.YYYY, DD.MM.YY, DD/MM/YYYY, DD-MM-YYYY, DD MM YYYY
  consume(/(?<![\d])(\d{1,2})[./\-\s](\d{1,2})[./\-\s](\d{4}|\d{2})(?![\d])/g, (m) => fullDate(fullYear(m[3], today), Number(m[2]), Number(m[1])), 3);
  // YYYY-MM-DD, YYYY.MM.DD, YYYY/MM/DD
  consume(/(?<![\d])(\d{4})[./-](\d{1,2})[./-](\d{1,2})(?![\d])/g, (m) => fullDate(fullYear(m[1], today), Number(m[2]), Number(m[3])), 3);
  // MM.YYYY, MM/YYYY, MM-YYYY, MM YYYY
  consume(/(?<![\d./-])(\d{1,2})[./\-\s](\d{4})(?![\d./-])/g, (m) => monthYear(fullYear(m[2], today), Number(m[1])), 2);
  // YYYY-MM, YYYY/MM
  consume(/(?<![\d./-])(\d{4})[./-](\d{1,2})(?![\d./-])/g, (m) => monthYear(fullYear(m[1], today), Number(m[2])), 2);
  // MM/YY, MM.YY – only when the two digits read as a plausible future year, so "05.27" is May 2027, not a day
  consume(
    /(?<![\d./-])(\d{2})[./-](\d{2})(?![\d./-])/g,
    (m) => {
      const year = fullYear(m[2], today);
      return year && year >= today.getFullYear() ? monthYear(year, Number(m[1])) : null;
    },
    1.5,
  );
  // A lone year, e.g. the vintage written on a jar lid
  consume(/(?<![\d./-])(20\d{2})(?![\d./-])/g, (m) => {
      const year = fullYear(m[1], today);
      return year ? { year, month: null, day: null } : null;
    },
    1,
  );

  return [...found.values()].sort(
    (a, b) => b.score - a.score || a.date.year - b.date.year || (a.date.month ?? 0) - (b.date.month ?? 0) || (a.date.day ?? 0) - (b.date.day ?? 0),
  );
}
