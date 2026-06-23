import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OperationBoard } from './OperationBoard';
import {
  advanceOperation,
  createOperation,
  listOperations,
  type Operation,
} from '../../api/operation';

vi.mock('../../api/operation', async (orig) => ({
  ...(await orig<typeof import('../../api/operation')>()),
  listOperations: vi.fn(),
  advanceOperation: vi.fn(() => Promise.resolve({} as Operation)),
  createOperation: vi.fn(() => Promise.resolve({} as Operation)),
}));
vi.mock('../../api/user', () => ({
  listUsers: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));

function op(id: number, stage: Operation['stage']): Operation {
  return { id, customerName: 'X 集团', title: '运维', stage, status: 'ACTIVE' };
}

function page(rows: Operation[]) {
  return { content: rows, total: rows.length, page: 0, size: 200 };
}

function renderBoard() {
  return render(
    <MemoryRouter>
      <OperationBoard />
    </MemoryRouter>,
  );
}

describe('OperationBoard', () => {
  beforeEach(() => {
    vi.mocked(advanceOperation).mockClear();
    vi.mocked(createOperation).mockClear();
    vi.mocked(listOperations).mockReset();
  });

  /** TC-OPRB-01: renders the after-sales pipeline + advance. */
  it('renders the operation pipeline and advances (TC-OPRB-01)', async () => {
    vi.mocked(listOperations)
      .mockResolvedValueOnce(page([op(3, 'MAINTENANCE')]))
      .mockResolvedValueOnce(page([op(3, 'OPERATING')]));
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opr-card-3')).toBeInTheDocument());
    expect(listOperations).toHaveBeenCalledWith({ size: 100 }); // PageParams size cap regression guard
    expect(screen.getByTestId('opr-col-REPURCHASE')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('opr-advance-3'));

    await waitFor(() => expect(advanceOperation).toHaveBeenCalledWith(3));
    expect(listOperations).toHaveBeenCalledTimes(2);
  });

  /** TC-OPRB-02: 新建运营单 form drawer creates with filled fields. */
  it('creates an operation via the form drawer (TC-OPRB-02)', async () => {
    vi.mocked(listOperations).mockResolvedValue(page([]));
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opr-summary')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opr-new-btn'));
    await waitFor(() => expect(screen.getByTestId('opr-new-customer')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('opr-new-customer'), { target: { value: '远方集团' } });
    fireEvent.change(screen.getByTestId('opr-new-title'), { target: { value: '运维' } });
    fireEvent.click(screen.getByTestId('opr-save-btn'));
    await waitFor(() =>
      expect(createOperation).toHaveBeenCalledWith(
        expect.objectContaining({ customerName: '远方集团', title: '运维' }),
      ),
    );
  });
});
