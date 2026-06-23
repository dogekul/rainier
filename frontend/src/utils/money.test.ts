import { describe, expect, it } from 'vitest';
import { formatCNY } from './money';

describe('formatCNY (TC-MON-01)', () => {
  it('null/undefined → 占位', () => {
    expect(formatCNY(null)).toBe('—');
    expect(formatCNY(undefined)).toBe('—');
  });

  it('< 1万 → 千分位￥', () => {
    expect(formatCNY(0)).toBe('¥0');
    expect(formatCNY(5000)).toBe('¥5,000');
    expect(formatCNY(9999)).toBe('¥9,999');
  });

  it('< 1亿 → 万（去尾 0 / ≤1 位小数）', () => {
    expect(formatCNY(10000)).toBe('¥1万');
    expect(formatCNY(200000)).toBe('¥20万');
    expect(formatCNY(25000)).toBe('¥2.5万');
    expect(formatCNY(2000000)).toBe('¥200万');
  });

  it('≥ 1亿 → 亿', () => {
    expect(formatCNY(100000000)).toBe('¥1亿');
    expect(formatCNY(120000000)).toBe('¥1.2亿');
  });

  it('万→亿 进位边界（舍入后再选单位）', () => {
    expect(formatCNY(99999999)).toBe('¥1亿'); // 不应为「¥10000万」
    expect(formatCNY(99994999)).toBe('¥9999.5万'); // 仍 < 1亿
  });

  it('负数保留符号', () => {
    expect(formatCNY(-25000)).toBe('-¥2.5万');
    expect(formatCNY(-5000)).toBe('-¥5,000');
  });
});
