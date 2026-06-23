import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CustomerPage } from './CustomerPage';
import { createCustomer, listCustomers, type Customer } from '../../api/customer';

vi.mock('../../api/customer', async (orig) => ({
  ...(await orig<typeof import('../../api/customer')>()),
  listCustomers: vi.fn(),
  createCustomer: vi.fn(() => Promise.resolve({} as Customer)),
  deleteCustomer: vi.fn(() => Promise.resolve()),
}));

function cust(id: number, name: string, over: Partial<Customer> = {}): Customer {
  return { id, name, industry: '金融', contactName: '张三', ...over };
}

function page(rows: Customer[]) {
  return { content: rows, total: rows.length, page: 0, size: 20 };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <CustomerPage />
    </MemoryRouter>,
  );
}

describe('CustomerPage (客户 CRUD)', () => {
  beforeEach(() => {
    vi.mocked(createCustomer).mockClear();
    vi.mocked(listCustomers).mockReset();
  });

  /** TC-CUS-FE-01: renders the customer list + new button. */
  it('renders the customer list (TC-CUS-FE-01)', async () => {
    vi.mocked(listCustomers).mockResolvedValue(page([cust(1, '中信集团'), cust(2, '招商银行')]));
    renderPage();
    await waitFor(() => expect(screen.getByText('中信集团')).toBeInTheDocument());
    expect(screen.getByText('招商银行')).toBeInTheDocument();
    expect(screen.getByTestId('customers-new-btn')).toBeInTheDocument();
  });

  /** TC-CUS-FE-02: 新建客户 drawer creates with the filled fields. */
  it('creates a customer via the drawer (TC-CUS-FE-02)', async () => {
    vi.mocked(listCustomers).mockResolvedValue(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('customers-new-btn')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('customers-new-btn'));
    await waitFor(() => expect(screen.getByTestId('customer-name')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('customer-name'), { target: { value: '新华集团' } });
    fireEvent.change(screen.getByTestId('customer-industry'), { target: { value: '能源' } });
    fireEvent.click(screen.getByTestId('customer-save'));
    await waitFor(() =>
      expect(createCustomer).toHaveBeenCalledWith(
        expect.objectContaining({ name: '新华集团', industry: '能源' }),
      ),
    );
  });

  /** TC-CUS-FE-03: blank name → form error, no create. */
  it('blocks save when name is empty (TC-CUS-FE-03)', async () => {
    vi.mocked(listCustomers).mockResolvedValue(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('customers-new-btn')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('customers-new-btn'));
    await waitFor(() => expect(screen.getByTestId('customer-save')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('customer-save'));
    await waitFor(() => expect(screen.getByTestId('customer-form-error')).toBeInTheDocument());
    expect(createCustomer).not.toHaveBeenCalled();
  });
});
