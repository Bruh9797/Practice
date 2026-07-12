import { useState } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { clearCsrf } from '../api/client.js';
import { AuthProvider, useAuth } from './AuthContext.jsx';

const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

function Harness() {
  const auth = useAuth();
  const [error, setError] = useState('');
  return <><output>{auth.status}:{auth.user?.username ?? 'none'}</output><button onClick={() => auth.login({ username: 'demo', password: 'demo12345' }).catch((e) => setError(e.message))}>login</button><span>{error}</span></>;
}

describe('AuthProvider', () => {
  beforeEach(() => { clearCsrf(); });
  afterEach(() => vi.unstubAllGlobals());

  it('восстанавливает гостя и отправляет CSRF при входе', async () => {
    const fetchMock = vi.fn(async (url, options = {}) => {
      if (url === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'token-1' });
      if (url === '/api/auth/me') return json({ message: 'Требуется вход' }, 401);
      if (url === '/api/auth/login') return json({ id: 1, username: 'demo', role: 'USER', enabled: true });
      throw new Error(`unexpected ${url}`);
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<AuthProvider><Harness /></AuthProvider>);
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('guest:none'));
    fireEvent.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('authenticated:demo'));
    const loginCall = fetchMock.mock.calls.find(([url]) => url === '/api/auth/login');
    expect(loginCall[1].credentials).toBe('include');
    expect(loginCall[1].headers.get('X-CSRF-TOKEN')).toBe('token-1');
  });
});
