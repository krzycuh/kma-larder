import { chromium } from '@playwright/test';
import { readFileSync, writeFileSync } from 'node:fs';
const [,, photo, outPrefix, variantsJson] = process.argv;
const variants = JSON.parse(variantsJson);
const base = 'http://localhost:5173';
const browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium' });
const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'pl-PL' });
const page = await ctx.newPage();
page.on('pageerror', (e) => console.log('pageerror', e.message));
await page.goto(base + '/login');
await page.fill('#email', 'admin@example.com'); await page.fill('#password', 'admin123'); await page.click('button[type=submit]');
await page.waitForURL(base + '/');
await page.goto(base + '/add');
const bytes = readFileSync(photo).toString('base64');
for (const v of variants) {
  const started = Date.now();
  const result = await page.evaluate(async ({ b64, v }) => {
    const mod = await import('/src/lib/ocr.ts');
    const parser = await import('/src/utils/dateOcr.ts');
    const bin = atob(b64); const arr = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
    const file = new Blob([arr], { type: 'image/jpeg' });
    const canvas = await mod.prepareImage(file, v);
    const text = await mod.recognizeText(file, undefined, { ...v, whitelist: v.whitelist === 'all' ? '' : undefined, psm: v.psm });
    return { text, dates: parser.extractDates(text), preview: canvas.toDataURL('image/png') };
  }, { b64: bytes, v });
  console.log('=== variant', JSON.stringify(v), 'time', Math.round((Date.now() - started) / 1000), 's');
  console.log(result.text.trim().split('\n').filter((l) => l.trim()).join(' | ').slice(0, 300));
  console.log('dates:', JSON.stringify(result.dates.map((d) => [d.raw, d.score])));
  if (v.dump) writeFileSync(`${outPrefix}-${v.dump}.png`, Buffer.from(result.preview.split(',')[1], 'base64'));
}
await browser.close();
