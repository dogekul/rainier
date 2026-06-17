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
  /**
   * v0.0.22 board-kit — 4-tier status colors (Feishu-style) shared by every v1.0 dashboard.
   * 红=逾期/BLOCKED/MISSED, 黄=进行中/待办/规划, 绿=完成/交付/达成, 灰=草稿/取消/关闭.
   */
  status: {
    red: '#F54A45',
    redBg: '#FDECEC',
    yellow: '#FAAD14',
    yellowBg: '#FFF7E6',
    green: '#34C724',
    greenBg: '#E8F7E8',
    gray: '#8F959E',
    grayBg: '#F0F1F2',
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
