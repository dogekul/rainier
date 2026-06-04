/**
 * Feishu-style (Lark Project) design tokens. Mirrored in {@link ../styles/global.css} as CSS
 * variables on {@code :root}. Update both together — tests assert the CSS values directly.
 *
 * <p>Custom-named ({@code --rainier-*}) rather than {@code --lark-*}; we are not claiming any Lark
 * brand. See design.md §6.
 */
export const tokens = {
  color: {
    primary: '#3370FF',
    text1: '#1F2329',
    text2: '#646A73',
    text3: '#8F959E',
  },
  bg: {
    page: '#F5F6F7',
    card: '#FFFFFF',
  },
  radius: {
    button: '6px',
    card: '8px',
  },
  shadow: {
    card: '0 4px 16px 0 rgba(0, 0, 0, 0.08)',
  },
  font: 'PingFang SC, -apple-system, "Helvetica Neue", Arial, sans-serif',
} as const;
