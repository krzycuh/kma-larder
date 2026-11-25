import { expect, test } from '@playwright/test';

/**
 * The household's everyday path, on a phone, against a running backend (dev profile, admin@example.com):
 * add a home-made product with a batch → eat one jar → see it among the expiry dates → a product below its
 * minimum shows up on the shopping list.
 */
const unique = Date.now().toString(36);

test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({ page }) => {
  await page.goto('/login');
  await page.fill('#email', 'admin@example.com');
  await page.fill('#password', 'admin123');
  await page.click('button[type=submit]');
  await page.waitForURL('/');
});

test('adds a home-made batch, consumes one unit and lists it by expiry', async ({ page }) => {
  await page.goto('/add');
  await page.fill('input[role=combobox]', `Dżem ${unique}`);
  await page.click('button[role=radio]:has-text("Domowy")');
  await page.selectOption('#product-category', 'PRESERVES');
  await page.fill('#product-shelf-life', '12');
  await page.fill('#batch-quantity', '3');
  const thisYear = new Date().getFullYear();
  await page.fill('#produced-on-month', '7');
  await page.fill('#produced-on-year', String(thisYear));
  await page.click('button[type=submit]');

  await page.waitForURL(/\/products\//);
  await expect(page.getByRole('heading', { name: new RegExp(`Dżem ${unique}`) })).toBeVisible();
  await expect(page.getByText('3', { exact: true }).first()).toBeVisible();
  await expect(page.getByText(`do 31 lip ${thisYear + 1}`)).toBeVisible();

  await page.click('button:has-text("Zużyj 1")');
  await expect(page.getByText('Zużyto')).toBeVisible();
  await expect(page.locator('section').filter({ hasText: 'Historia' }).getByText('zużyto')).toBeVisible();

  await page.goto('/expiring');
  await expect(page.getByRole('link', { name: new RegExp(`Dżem ${unique}`) })).toBeVisible();
});

test('a product below its minimum appears on the shopping list', async ({ page }) => {
  await page.goto('/add');
  await page.fill('input[role=combobox]', `Woda ${unique}`);
  await page.selectOption('#product-category', 'BEVERAGES');
  await page.fill('#batch-quantity', '1');
  await page.click('button[type=submit]');
  await page.waitForURL(/\/products\//);

  await page.click('a:has-text("Edytuj")');
  await page.waitForURL(/\/edit$/);
  await page.fill('#product-min', '6');
  await page.click('button[type=submit]');
  await page.waitForURL(/\/products\/[^/]+$/);
  await expect(page.getByText('brakuje 5 do minimum')).toBeVisible();

  await page.goto('/replenish');
  const row = page.locator('li').filter({ hasText: `Woda ${unique}` });
  await expect(row).toBeVisible();
  await expect(row.getByText('kup 5')).toBeVisible();
});
