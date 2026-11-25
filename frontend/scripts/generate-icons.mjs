// Renders the app icon (a preserving jar on a dark brown tile) into every file the PWA needs.
// Run `npm run icons` after changing the artwork below; the PNGs in public/ are committed.
//
// - favicon.svg                     rounded tile, scalable (browser tab)
// - icon-192.png / icon-512.png     rounded tile with transparent corners ("any": desktop shortcuts, install prompts)
// - icon-maskable-*.png             full-bleed tile, the jar inside the safe zone ("maskable": Android home screen,
//                                   which cuts its own circle/squircle — without this Chrome pads the icon on white)
// - apple-touch-icon.png            full-bleed 180px; iOS rounds the corners itself
import { chromium } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const outDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../public');

/**
 * @param {{ rounded: boolean, scale: number }} options
 *   rounded – corner radius on the tile (false = full-bleed square for masked icons)
 *   scale   – size of the books relative to the tile; maskable icons keep them inside the central 80% circle
 */
function iconSvg({ rounded, scale }) {
  const radius = rounded ? 14 : 0;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64" role="img" aria-label="KMA Larder">
  <defs>
    <radialGradient id="bg" cx="50%" cy="40%" r="70%">
      <stop offset="0" stop-color="#4a2f20"/>
      <stop offset="0.6" stop-color="#2a1d14"/>
      <stop offset="1" stop-color="#16120f"/>
    </radialGradient>
  </defs>
  <rect width="64" height="64" rx="${radius}" fill="url(#bg)"/>${
    rounded
      ? `
  <rect x="1" y="1" width="62" height="62" rx="${radius - 1}" fill="none" stroke="#ec986c" stroke-opacity="0.35" stroke-width="1"/>`
      : ''
  }
  <g transform="translate(32 32) scale(${scale}) translate(-32 -32)">
    <rect x="20" y="7" width="24" height="8" rx="2" fill="#ec986c"/>
    <path d="M18 17h28v33a6 6 0 0 1-6 6H24a6 6 0 0 1-6-6z" fill="#f5cdb7"/>
    <path d="M18 31h28v19a6 6 0 0 1-6 6H24a6 6 0 0 1-6-6z" fill="#f59e0b"/>
    <rect x="22" y="35" width="20" height="12" rx="1.5" fill="#fffdfa" opacity="0.92"/>
    <rect x="25" y="39" width="14" height="2" fill="#b44d26" opacity="0.7"/>
    <rect x="25" y="43" width="9" height="2" fill="#b44d26" opacity="0.5"/>
  </g>
</svg>
`;
}

const outputs = [
  { file: 'favicon.svg', rounded: true, scale: 1 },
  { file: 'icon-192.png', size: 192, rounded: true, scale: 1 },
  { file: 'icon-512.png', size: 512, rounded: true, scale: 1 },
  { file: 'icon-maskable-192.png', size: 192, rounded: false, scale: 0.78 },
  { file: 'icon-maskable-512.png', size: 512, rounded: false, scale: 0.78 },
  { file: 'apple-touch-icon.png', size: 180, rounded: false, scale: 0.9 },
];

await mkdir(outDir, { recursive: true });
// CHROMIUM_PATH lets an environment with a preinstalled browser skip `playwright install`
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
try {
  for (const output of outputs) {
    const svg = iconSvg(output);
    const target = path.join(outDir, output.file);
    if (!output.size) {
      await writeFile(target, svg);
    } else {
      const page = await browser.newPage({ viewport: { width: output.size, height: output.size }, deviceScaleFactor: 1 });
      await page.setContent(
        `<!doctype html><html><body style="margin:0;background:transparent">${svg.replace(
          'width="64" height="64"',
          `width="${output.size}" height="${output.size}"`,
        )}</body></html>`,
      );
      await page.screenshot({ path: target, omitBackground: true, type: 'png' });
      await page.close();
    }
    console.log(`wrote ${path.relative(process.cwd(), target)}`);
  }
} finally {
  await browser.close();
}
