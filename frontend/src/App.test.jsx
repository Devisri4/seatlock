import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import App from './App.jsx';

function mockHealth(response) {
  vi.spyOn(globalThis, 'fetch').mockImplementation(response);
}

describe('App', () => {
  it('shows the page and reports the API online when health is UP', async () => {
    mockHealth(async () => ({ ok: true, json: async () => ({ status: 'UP' }) }));

    render(<App />);

    expect(screen.getByRole('heading', { name: /pick a seat/i })).toBeInTheDocument();
    expect(await screen.findByText('API online')).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith('/actuator/health');
  });

  it('reports the API offline when the server cannot be reached', async () => {
    mockHealth(async () => {
      throw new TypeError('Failed to fetch');
    });

    render(<App />);

    expect(await screen.findByText('API offline')).toBeInTheDocument();
  });

  it('reports the API offline when health is not UP', async () => {
    mockHealth(async () => ({ ok: false, json: async () => ({}) }));

    render(<App />);

    expect(await screen.findByText('API offline')).toBeInTheDocument();
  });
});
