import { render } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import App from '../App';
import { TOKEN_STORAGE_KEY, useAuthStore } from '../store/auth';

/**
 * Covers TC-FES-003: feishu-style design tokens are exposed as CSS variables on :root and stay
 * stable across refactors. Reads computed style so the assertion does not depend on className or
 * component implementation.
 */
describe('design tokens on :root', () => {
  beforeEach(() => {
    // Reset shared state so this test does not depend on the order other tests ran in.
    useAuthStore.setState({ token: null, user: null });
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  });

  it('exposes feishu-style variables (primary color, button/card radius)', () => {
    render(<App />);
    const style = getComputedStyle(document.documentElement);

    expect(style.getPropertyValue('--rainier-color-primary').trim().toUpperCase()).toBe('#3370FF');
    expect(style.getPropertyValue('--rainier-radius-button').trim()).toBe('6px');
    expect(style.getPropertyValue('--rainier-radius-card').trim()).toBe('8px');
  });

  /** v0.0.22 board-kit: the 4-tier status colors are exposed on :root. */
  it('exposes the 4-tier board status colors', () => {
    render(<App />);
    const style = getComputedStyle(document.documentElement);
    expect(style.getPropertyValue('--rainier-status-red').trim().toUpperCase()).toBe('#F54A45');
    expect(style.getPropertyValue('--rainier-status-yellow').trim().toUpperCase()).toBe('#FAAD14');
    expect(style.getPropertyValue('--rainier-status-green').trim().toUpperCase()).toBe('#34C724');
    expect(style.getPropertyValue('--rainier-status-gray').trim().toUpperCase()).toBe('#8F959E');
  });
});
