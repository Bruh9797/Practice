import fs from 'node:fs/promises';
import path from 'node:path';

const debugUrl = process.env.CDP_URL || 'http://127.0.0.1:9223';
const baseUrl = process.env.APP_URL || 'http://localhost:8080';
const outputDir = path.resolve(process.argv[2] || 'docs/presentation/assets');

await fs.mkdir(outputDir, { recursive: true });

async function createTarget(url) {
  const response = await fetch(`${debugUrl}/json/new?${encodeURIComponent(url)}`, { method: 'PUT' });
  if (!response.ok) throw new Error(`Cannot create browser target: ${response.status}`);
  return response.json();
}

class CdpClient {
  constructor(webSocketUrl) {
    this.sequence = 0;
    this.pending = new Map();
    this.socket = new WebSocket(webSocketUrl);
  }

  async open() {
    await new Promise((resolve, reject) => {
      this.socket.addEventListener('open', resolve, { once: true });
      this.socket.addEventListener('error', reject, { once: true });
    });
    this.socket.addEventListener('message', (event) => {
      const message = JSON.parse(event.data);
      if (!message.id) return;
      const waiter = this.pending.get(message.id);
      if (!waiter) return;
      this.pending.delete(message.id);
      if (message.error) waiter.reject(new Error(message.error.message));
      else waiter.resolve(message.result);
    });
  }

  send(method, params = {}) {
    const id = ++this.sequence;
    this.socket.send(JSON.stringify({ id, method, params }));
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }));
  }

  close() {
    this.socket.close();
  }
}

const delay = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

async function evaluate(client, expression) {
  const result = await client.send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true,
  });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text || 'Browser evaluation failed');
  return result.result?.value;
}

async function waitFor(client, expression, timeout = 20_000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    if (await evaluate(client, `Boolean(${expression})`)) return;
    await delay(250);
  }
  throw new Error(`Timed out waiting for ${expression}`);
}

async function navigate(client, url) {
  await client.send('Page.navigate', { url });
  await waitFor(client, `document.readyState === 'complete'`);
  await delay(600);
}

async function screenshot(client, filename) {
  await evaluate(client, `window.scrollTo(0, 0)`);
  await delay(250);
  const result = await client.send('Page.captureScreenshot', {
    format: 'png',
    captureBeyondViewport: false,
    fromSurface: true,
  });
  await fs.writeFile(path.join(outputDir, filename), Buffer.from(result.data, 'base64'));
}

async function setInput(client, selector, value) {
  const payload = JSON.stringify(value);
  await evaluate(client, `(() => {
    const input = document.querySelector(${JSON.stringify(selector)});
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    setter.call(input, ${payload});
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
    return input.value;
  })()`);
}

async function login(client, username, password) {
  await navigate(client, `${baseUrl}/login`);
  await waitFor(client, `document.querySelector('input[autocomplete="username"]')`);
  await setInput(client, 'input[autocomplete="username"]', username);
  await setInput(client, 'input[autocomplete="current-password"]', password);
  await evaluate(client, `document.querySelector('form').requestSubmit()`);
  await waitFor(client, `location.pathname === '/catalog'`, 25_000);
  await waitFor(client, `document.querySelectorAll('.result-card').length > 0`, 25_000);
  await delay(700);
}

const target = await createTarget(`${baseUrl}/login`);
const client = new CdpClient(target.webSocketDebuggerUrl);
await client.open();
await client.send('Page.enable');
await client.send('Runtime.enable');
await client.send('Network.enable');
await client.send('Network.clearBrowserCookies');
await client.send('Emulation.setDeviceMetricsOverride', {
  width: 1440,
  height: 900,
  deviceScaleFactor: 1,
  mobile: false,
});

await login(client, 'admin', 'admin12345');
await screenshot(client, 'catalog.png');

await evaluate(client, `document.querySelector('.result-card h3 a').click()`);
await waitFor(client, `location.pathname.startsWith('/heat-exchangers/')`);
await waitFor(client, `document.querySelector('.specification-card, .detail-page, main')`);
await delay(700);
await screenshot(client, 'detail.png');

await navigate(client, `${baseUrl}/admin`);
await waitFor(client, `document.querySelector('.admin-stats')`);
await screenshot(client, 'admin.png');

await navigate(client, `${baseUrl}/admin/catalog`);
await waitFor(client, `document.querySelector('.admin-table')`);
await screenshot(client, 'admin-catalog.png');

client.close();
console.log(outputDir);
