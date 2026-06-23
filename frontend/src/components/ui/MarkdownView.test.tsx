import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { MarkdownView } from './MarkdownView';

describe('MarkdownView (safe markdown → rich text)', () => {
  it('renders headings, bold, lists and paragraphs', () => {
    const md = ['# 决策评审纪要', '', '结论：**通过**。', '', '- 第一点', '- 第二点'].join('\n');
    render(<MarkdownView content={md} testId="md" />);
    const root = screen.getByTestId('md');
    expect(root.querySelector('h3')?.textContent).toBe('决策评审纪要');
    expect(root.querySelector('strong')?.textContent).toBe('通过');
    expect(root.querySelectorAll('li')).toHaveLength(2);
  });

  it('renders ordered lists', () => {
    render(<MarkdownView content={'1. 一\n2. 二\n3. 三'} testId="md" />);
    const root = screen.getByTestId('md');
    expect(root.querySelector('ol')).not.toBeNull();
    expect(root.querySelectorAll('ol li')).toHaveLength(3);
  });

  it('does NOT inject raw HTML (XSS-safe)', () => {
    render(<MarkdownView content={'<img src=x onerror=alert(1)> **safe**'} testId="md" />);
    const root = screen.getByTestId('md');
    // the <img> is rendered as literal text, not an element
    expect(root.querySelector('img')).toBeNull();
    expect(root.textContent).toContain('<img src=x onerror=alert(1)>');
    expect(root.querySelector('strong')?.textContent).toBe('safe');
  });

  it('handles empty content without crashing', () => {
    render(<MarkdownView content={''} testId="md" />);
    expect(screen.getByTestId('md')).toBeInTheDocument();
  });
});
