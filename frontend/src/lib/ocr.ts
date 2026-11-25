import { createWorker, OEM, type Worker } from 'tesseract.js';

/**
 * Tesseract.js in the browser: the photo never leaves the phone. The worker, the WebAssembly core and the
 * Polish language data are served from our own origin (/ocr, see scripts/copy-ocr-assets.mjs); everything
 * is fetched only when the OCR is first used (~8 MB, cached by the browser afterwards).
 */
export type OcrProgress = (status: 'loading' | 'recognizing', progress: number) => void;

const LANG = 'pol';
let workerPromise: Promise<Worker> | null = null;
let progressListener: OcrProgress | null = null;

async function getWorker(onProgress?: OcrProgress): Promise<Worker> {
  progressListener = onProgress ?? null;
  workerPromise ??= createWorker(LANG, OEM.LSTM_ONLY, {
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
    workerPromise = null; // allow a retry, e.g. after a network hiccup while loading the core
    throw error;
  });
  return workerPromise;
}

/** Longest edge of the image handed to the OCR; phone photos are far bigger than the engine needs */
const MAX_EDGE = 1600;

/**
 * Draws the photo onto a canvas at a size the OCR copes with, honouring the EXIF orientation,
 * and returns a grayscale, contrast-stretched version: cheap, and it helps with inkjet dates on cans.
 */
export async function prepareImage(file: Blob): Promise<HTMLCanvasElement> {
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  const scale = Math.min(1, MAX_EDGE / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  const ctx = canvas.getContext('2d', { willReadFrequently: true })!;
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();

  const image = ctx.getImageData(0, 0, canvas.width, canvas.height);
  const data = image.data;
  let min = 255;
  let max = 0;
  for (let i = 0; i < data.length; i += 4) {
    const gray = Math.round(0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2]);
    data[i] = gray;
    if (gray < min) min = gray;
    if (gray > max) max = gray;
  }
  const range = Math.max(1, max - min);
  for (let i = 0; i < data.length; i += 4) {
    const stretched = Math.round(((data[i] - min) / range) * 255);
    data[i] = data[i + 1] = data[i + 2] = stretched;
  }
  ctx.putImageData(image, 0, 0);
  return canvas;
}

/** Text found on the photo (lines separated by newlines). */
export async function recognizeText(file: Blob, onProgress?: OcrProgress): Promise<string> {
  const [worker, canvas] = await Promise.all([getWorker(onProgress), prepareImage(file)]);
  const result = await worker.recognize(canvas);
  return result.data.text;
}
