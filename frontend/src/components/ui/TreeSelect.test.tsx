import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TreeSelect, type TreeNode } from './TreeSelect';

const nodes: TreeNode[] = [
  { id: 'a', name: '总公司', parentId: null },
  { id: 'b', name: '研发部', parentId: 'a' },
  { id: 'c', name: '后端组', parentId: 'b' },
];

describe('TreeSelect', () => {
  it('renders the hierarchy when opened and selects on click (TC-FES-203 / TC-ORG-018)', () => {
    const onChange = vi.fn();
    render(<TreeSelect value={null} nodes={nodes} onChange={onChange} />);
    expect(screen.queryByTestId('rainier-treeselect-panel')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button'));
    expect(screen.getByTestId('rainier-treeselect-panel')).toBeInTheDocument();
    const items = screen.getAllByTestId('rainier-treeselect-item');
    // 1 clear option + 3 nodes
    expect(items).toHaveLength(4);
    fireEvent.click(items[2]); // 研发部
    expect(onChange).toHaveBeenCalledWith('b');
  });
});
