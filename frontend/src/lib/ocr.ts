import { createWorker, OEM, PSM, type Worker } from 'tesseract.js';
import { extractDates, type DateCandidate } from '../utils/dateOcr';

/**
 * Tesseract.js in the browser: the photo never leaves the phone. The worker, the WebAssembly core and the
 * Polish language data are served from our own origin (/ocr, see scripts/copy-ocr-assets.mjs); everything
 * is fetched only when the OCR is first used (~8 MB, cached by the browser afterwards).
 */
export type OcrProgress = (status: 'loading' | 'recognizing', progress: number, attempt?: { current: number; total: number }) => void;

const workers = new Map<string, Promise<Worker>>();
let progressListener: OcrProgress | null = null;

async function getWorker(onProgress?: OcrProgress, lang = 'pol'): Promise<Worker> {
  progressListener = onProgress ?? null;
  const existing = workers.get(lang);
  if (existing) return existing;
  const created = createWorker(lang, OEM.LSTM_ONLY, {
    workerPath: '/ocr/worker.min.js',
    corePath: '/ocr/core',
    langPath: '/ocr/lang',
    // A same-origin worker script instead of a blob: URL, so the CSP needs no worker-src blob:
    workerBlobURL: false,
    logger: (m) => {
      if (m.status === 'recognizing text') progressListener?.('recognizing', m.progress);
      else progressListener?.('loading', m.progress);
    },
  }).catch((error) => {
    workers.delete(lang); // allow a retry, e.g. after a network hiccup while loading the core
    throw error;
  });
  workers.set(lang, created);
  return created;
}

/** Longest edge of the image handed to the OCR; phone photos are far bigger than the engine needs */
const MAX_EDGE = 1600;
/** Darkness (0–255, against the local background) treated as paper grain rather than ink */
const NOISE_FLOOR = 6;
/** Never stretch less than this much darkness to black, or a blank photo turns its grain into ink */
const MIN_INK = 30;

export interface PrepareOptions {
  /** Clockwise rotation applied before OCR, for dates printed sideways */
  rotation?: 0 | 90 | 180 | 270;
  /** Radius of the blur that fuses the dots of an inkjet print into strokes (0 = off) */
  blur?: number;
  /** Black-and-white output: pixels darker than their surroundings by this much become ink (0 = keep grayscale) */
  threshold?: number;
  /** Longest edge after downscaling; the engine reads best when a line of digits is a few hundred pixels wide */
  maxEdge?: number;
}

/** Separable box blur on a grayscale plane, in place; radius in pixels. Linear time, fine for 2 Mpx. */
function boxBlur(src: Float32Array, width: number, height: number, radius: number): Float32Array {
  if (radius <= 0) return src;
  const tmp = new Float32Array(src.length);
  const out = new Float32Array(src.length);
  const window = 2 * radius + 1;
  for (let y = 0; y < height; y++) {
    const row = y * width;
    let sum = 0;
    for (let x = -radius; x <= radius; x++) sum += src[row + Math.min(width - 1, Math.max(0, x))];
    for (let x = 0; x < width; x++) {
      tmp[row + x] = sum / window;
      sum += src[row + Math.min(width - 1, x + radius + 1)] - src[row + Math.max(0, x - radius)];
    }
  }
  for (let x = 0; x < width; x++) {
    let sum = 0;
    for (let y = -radius; y <= radius; y++) sum += tmp[Math.min(height - 1, Math.max(0, y)) * width + x];
    for (let y = 0; y < height; y++) {
      out[y * width + x] = sum / window;
      sum += tmp[Math.min(height - 1, y + radius + 1) * width + x] - tmp[Math.max(0, y - radius) * width + x];
    }
  }
  return out;
}

/**
 * Draws the photo onto a canvas at a size the OCR copes with, honouring the EXIF orientation and the requested
 * rotation, then cleans it up for inkjet dates on packaging: grayscale, a small blur that joins the dots of the
 * print into strokes, and a comparison with the local background (a large blur) so that shadows, glare and
 * foil texture fall away and only ink that is darker than its surroundings stays.
 */
export async function prepareImage(file: Blob, options: PrepareOptions = {}): Promise<HTMLCanvasElement> {
  const { rotation = 0, blur = 2, threshold = 0, maxEdge = MAX_EDGE } = options;
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  const scale = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
  const w = Math.round(bitmap.width * scale);
  const h = Math.round(bitmap.height * scale);
  const sideways = rotation === 90 || rotation === 270;
  const canvas = document.createElement('canvas');
  canvas.width = sideways ? h : w;
  canvas.height = sideways ? w : h;
  const ctx = canvas.getContext('2d', { willReadFrequently: true })!;
  ctx.translate(canvas.width / 2, canvas.height / 2);
  ctx.rotate((rotation * Math.PI) / 180);
  ctx.drawImage(bitmap, -w / 2, -h / 2, w, h);
  bitmap.close();

  const width = canvas.width;
  const height = canvas.height;
  const image = ctx.getImageData(0, 0, width, height);
  const data = image.data;
  const gray = new Float32Array(width * height);
  for (let i = 0, p = 0; i < data.length; i += 4, p++) {
    gray[p] = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
  }
  const ink = boxBlur(gray, width, height, blur);
  // Local background: what the paper looks like around each pixel (window ≈ a line of text)
  const background = boxBlur(gray, width, height, Math.round(Math.max(width, height) / 40));
  const darker = new Float32Array(gray.length);
  const histogram = new Uint32Array(256);
  for (let p = 0; p < gray.length; p++) {
    const d = Math.max(0, Math.min(255, Math.round(background[p] - ink[p])));
    darker[p] = d;
    histogram[d]++;
  }
  // "Full ink" is how dark the darkest half percent of the picture is; faint inkjet prints get stretched to black
  let remaining = Math.round(gray.length * 0.005);
  let fullInk = 255;
  for (let level = 255; level > 0; level--) {
    remaining -= histogram[level];
    if (remaining <= 0) {
      fullInk = level;
      break;
    }
  }
  fullInk = Math.max(fullInk, MIN_INK);
  for (let p = 0, i = 0; p < gray.length; p++, i += 4) {
    const strength = Math.max(0, (darker[p] - NOISE_FLOOR) / (fullInk - NOISE_FLOOR));
    const value = threshold > 0 ? (darker[p] > threshold ? 0 : 255) : Math.round(255 * (1 - Math.min(1, strength)));
    data[i] = data[i + 1] = data[i + 2] = value;
  }
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.putImageData(image, 0, 0);
  return canvas;
}

export interface RecognizeOptions extends PrepareOptions {
  /** Only these characters can be recognised; dates need digits and separators, nothing else */
  whitelist?: string;
  psm?: PSM;
  lang?: string;
}

export const DATE_CHARS = '0123456789./-: ';

/** Text found on the photo (lines separated by newlines). */
export async function recognizeText(file: Blob, onProgress?: OcrProgress, options: RecognizeOptions = {}): Promise<string> {
  const { whitelist = DATE_CHARS, psm = PSM.SPARSE_TEXT, lang, ...prepare } = options;
  const [worker, canvas] = await Promise.all([getWorker(onProgress, lang), prepareImage(file, prepare)]);
  await worker.setParameters({ tessedit_char_whitelist: whitelist, tessedit_pageseg_mode: psm });
  const result = await worker.recognize(canvas);
  return result.data.text;
}

export interface DateRecognition {
  /** Distinct dates found, most certain first; a date seen in several attempts counts more */
  candidates: (DateCandidate & { votes: number })[];
  /** Text of the attempt that read best, for the user to judge */
  text: string;
  attempts: number;
}

/**
 * The attempts, in order: the photo as taken, then turned for dates printed sideways (both ways), then upside down.
 * Each orientation is read in grayscale first and, when that finds no full date, once more in black and white.
 */
const ATTEMPTS: RecognizeOptions[] = [
  { rotation: 0 },
  { rotation: 0, threshold: 12 },
  { rotation: 270 },
  { rotation: 270, threshold: 12 },
  { rotation: 90 },
  { rotation: 90, threshold: 12 },
  { rotation: 180 },
];

const FULL_DATE_SCORE = 3;

/**
 * Looks for best-before / production dates on the photo. Stops at the first attempt that reads a full date,
 * otherwise tries the other orientations and keeps everything found, so the user still gets a list to pick from.
 */
export async function recognizeDates(file: Blob, onProgress?: OcrProgress, today = new Date()): Promise<DateRecognition> {
  const found = new Map<string, DateCandidate & { votes: number }>();
  let bestText = '';
  let bestCount = -1;
  let attempts = 0;
  for (let i = 0; i < ATTEMPTS.length; i++) {
    const options = ATTEMPTS[i];
    // A black-and-white pass is only worth it when the grayscale pass of the same orientation found no full date
    if (options.threshold && [...found.values()].some((c) => c.score >= FULL_DATE_SCORE)) continue;
    attempts++;
    const attempt = { current: i + 1, total: ATTEMPTS.length };
    const text = await recognizeText(file, (status, progress) => onProgress?.(status, progress, attempt), options);
    const dates = extractDates(text, today);
    for (const candidate of dates) {
      const key = `${candidate.date.year}-${candidate.date.month ?? ''}-${candidate.date.day ?? ''}`;
      const existing = found.get(key);
      if (existing) existing.votes++;
      else found.set(key, { ...candidate, votes: 1 });
    }
    if (dates.length > bestCount) {
      bestCount = dates.length;
      bestText = text;
    }
    if (dates.some((c) => c.score >= FULL_DATE_SCORE)) break;
  }
  const candidates = [...found.values()].sort(
    (a, b) => b.score - a.score || b.votes - a.votes || a.date.year - b.date.year || (a.date.month ?? 0) - (b.date.month ?? 0) || (a.date.day ?? 0) - (b.date.day ?? 0),
  );
  return { candidates, text: bestText, attempts };
}
