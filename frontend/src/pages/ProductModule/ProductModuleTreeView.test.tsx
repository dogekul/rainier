import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { ProductModuleTreeView } from './ProductModuleTreeView';
import type { ProductModule } from '../../api/productModule';

/**
 * Minimal DataTransfer stub — jsdom doesn't implement it. Mirrors only the
 * surface area used by ProductModuleTreeView's drag handlers.
 */
function makeDataTransfer(): DataTransfer {
  const store: Record<string, string> = {};
  return {
    dropEffect: 'none',
    effectAllowed: 'none',
    files: [] as unknown as FileList,
    items: [] as unknown as DataTransferItemList,
    types: [] as unknown as ReadonlyArray<string>,
    clearData: () => {},
    getData: (k: string) => store[k] ?? '',
    setData: (k: string, v: string) => {
      store[k] = v;
    },
    setDragImage: () => {},
  } as unknown as DataTransfer;
}

function dragRow(rowEl: HTMLElement, dt: DataTransfer) {
  fireEvent.dragStart(rowEl, { dataTransfer: dt });
}
function dropOnRow(rowEl: HTMLElement, dt: DataTransfer) {
  fireEvent.dragOver(rowEl, { dataTransfer: dt });
  fireEvent.drop(rowEl, { dataTransfer: dt });
}

const baseMods: ProductModule[] = [
  { id: 1, code: 'M1', name: '钱包', status: 'ACTIVE', productId: 1, parentId: null, ownerUserId: 1 },
  { id: 2, code: 'M2', name: '余额', status: 'ACTIVE', productId: 1, parentId: 1, ownerUserId: 1 },
  { id: 3, code: 'M3', name: '明细', status: 'ACTIVE', productId: 1, parentId: 2, ownerUserId: 1 },
  { id: 4, code: 'M4', name: '风控', status: 'ACTIVE', productId: 1, parentId: null, ownerUserId: 1 },
  { id: 5, code: 'M5', name: '外部', status: 'ACTIVE', productId: 2, parentId: null, ownerUserId: 1 },
];

function getRow(nodeId: number): HTMLElement {
  return screen
    .getByTestId(`module-tree-node-${nodeId}`)
    .querySelector('.module-tree-row') as HTMLElement;
}

describe('ProductModuleTreeView drag reparent', () => {
  /** TC-FES-PMOD-DND-001 */
  it('drop M4 onto M2 → onReparent(4, 2)', () => {
    const onReparent = vi.fn();
    render(
      <ProductModuleTreeView
        modules={baseMods}
        onEdit={() => {}}
        onDelete={() => {}}
        onReparent={onReparent}
      />,
    );
    const dt = makeDataTransfer();
    dragRow(getRow(4), dt);
    dropOnRow(getRow(2), dt);
    expect(onReparent).toHaveBeenCalledTimes(1);
    expect(onReparent).toHaveBeenCalledWith(4, 2);
  });

  /** TC-FES-PMOD-DND-002 */
  it('drop M2 onto root dropzone → onReparent(2, null)', () => {
    const onReparent = vi.fn();
    render(
      <ProductModuleTreeView
        modules={baseMods}
        onEdit={() => {}}
        onDelete={() => {}}
        onReparent={onReparent}
      />,
    );
    const dt = makeDataTransfer();
    dragRow(getRow(2), dt);
    const dz = screen.getByTestId('product-module-root-dropzone');
    fireEvent.dragOver(dz, { dataTransfer: dt });
    fireEvent.drop(dz, { dataTransfer: dt });
    expect(onReparent).toHaveBeenCalledTimes(1);
    expect(onReparent).toHaveBeenCalledWith(2, null);
  });

  /** TC-FES-PMOD-DND-003 — cross-product drop short-circuited. */
  it('drop M5 (productId=2) onto M1 (productId=1) → no callback', () => {
    const onReparent = vi.fn();
    render(
      <ProductModuleTreeView
        modules={baseMods}
        onEdit={() => {}}
        onDelete={() => {}}
        onReparent={onReparent}
      />,
    );
    const dt = makeDataTransfer();
    dragRow(getRow(5), dt);
    dropOnRow(getRow(1), dt);
    expect(onReparent).not.toHaveBeenCalled();
  });

  /** TC-FES-PMOD-DND-004 — cycle (drop onto own descendant) short-circuited. */
  it('drop M1 onto descendant M3 → no callback', () => {
    const onReparent = vi.fn();
    render(
      <ProductModuleTreeView
        modules={baseMods}
        onEdit={() => {}}
        onDelete={() => {}}
        onReparent={onReparent}
      />,
    );
    const dt = makeDataTransfer();
    dragRow(getRow(1), dt);
    dropOnRow(getRow(3), dt);
    expect(onReparent).not.toHaveBeenCalled();
  });

  /** Self-drop guard. */
  it('drop M2 onto itself → no callback', () => {
    const onReparent = vi.fn();
    render(
      <ProductModuleTreeView
        modules={baseMods}
        onEdit={() => {}}
        onDelete={() => {}}
        onReparent={onReparent}
      />,
    );
    const dt = makeDataTransfer();
    dragRow(getRow(2), dt);
    dropOnRow(getRow(2), dt);
    expect(onReparent).not.toHaveBeenCalled();
  });
});
