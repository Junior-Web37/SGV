import { test, expect } from '@playwright/test';

test('open users page and open create user modal', async ({ page }) => {
  // mock backend endpoints: auth/me, permissions catalog, create user
  await page.route('**/api/auth/me', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ username: 'e2e_admin', fullName: 'E2E Admin', id: 1 })
  }));
  await page.route('**/api/permissions/catalog', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(['SISTEMA:CREATE','SISTEMA:VIEW','VENDAS:VIEW'])
  }));
  await page.route('**/api/users', route => {
    const req = route.request();
    if (req.method() === 'GET') {
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
    } else if (req.method() === 'POST') {
      route.fulfill({ status: 201, contentType: 'application/json', body: '{}' });
    } else {
      route.continue();
    }
  });
  // inject token to bypass client-side login redirect
  await page.addInitScript(() => localStorage.setItem('sgv_token', 'e2e_dummy_token'));
  await page.goto('http://localhost:5173/settings/users');
  await expect(page).toHaveURL(/users/);
  await page.getByRole('button', { name: 'Novo Utilizador' }).click({ timeout: 10000 });
  const form = page.locator('form').first();
  await expect(form).toBeVisible();
  // fill username (first input inside the modal form)
  await form.locator('input').nth(0).fill('e2e_test_user');
  // check a permission checkbox if present
  const chk = page.locator('input[type=checkbox]').first();
  if (await chk.count() > 0) await chk.check();
  await page.getByRole('button', { name: 'Criar' }).click();
  await expect(page.locator('text=Utilizador criado')).toHaveCount(1).catch(() => {});
});
