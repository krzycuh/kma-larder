// Copies the Tesseract.js worker and WebAssembly cores from node_modules into public/ocr, so the OCR runs
// from our own origin (the CSP allows no CDN). Runs before `vite` and `vite build` (predev / prebuild).
// The language data (public/ocr/lang/pol.traineddata.gz, tessdata_fast) is committed, these files are not.
import { copyFile, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const out = path.join(root, 'public', 'ocr');
const core = path.join(root, 'node_modules', 'tesseract.js-core');

// LSTM-only cores: the app uses OEM LSTM_ONLY, so the legacy engine is never loaded
const coreFiles = ['lstm', 'simd-lstm', 'relaxedsimd-lstm'].flatMap((variant) => [
  `tesseract-core-${variant}.wasm.js`,
  `tesseract-core-${variant}.wasm`,
]);

await mkdir(path.join(out, 'core'), { recursive: true });
await copyFile(path.join(root, 'node_modules', 'tesseract.js', 'dist', 'worker.min.js'), path.join(out, 'worker.min.js'));
for (const file of coreFiles) {
  await copyFile(path.join(core, file), path.join(out, 'core', file));
}
console.log(`OCR assets copied to ${path.relative(process.cwd(), out)}`);
