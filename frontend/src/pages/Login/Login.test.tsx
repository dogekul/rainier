import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import Login from './index';

/**
 * Covers TC-FES-004: the primary submit button uses the feishu primary color
 * (rgb(51, 112, 255)) via the {@code --rainier-color-primary} token. Reads computed style,
 * agnostic to className.
 */
describe('Login page', () => {
  beforeEach(() => {
    window.localStorage.removeItem('rainier.token');
  });

  it('submit button uses --rainier-color-primary as background-color', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );
    const button = screen.getByTestId('login-submit') as HTMLButtonElement;
    const style = window.getComputedStyle(button);
    // jsdom does NOT resolve var() inside getComputedStyle (known limitation), so we assert that
    // the button reaches for the token rather than hardcoding a color. The token → rgb(51,112,255)
    // mapping is covered by TC-FES-003 (tokens.test.tsx). End-to-end visual check is covered by
    // TC-DRT-001 browser smoke. Documented in pending-adjustments.md.
    expect(style.backgroundColor).toBe('var(--rainier-color-primary)');
  });
});
