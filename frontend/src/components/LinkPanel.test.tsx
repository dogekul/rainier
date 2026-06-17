import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LinkPanel } from './LinkPanel';
import { createLink, deleteLink, listLinks } from '../api/link';

vi.mock('../api/link', async (orig) => ({
  ...(await orig<typeof import('../api/link')>()),
  listLinks: vi
    .fn()
    .mockResolvedValue([
      { id: 1, targetType: 'TASK', targetId: 7, linkType: 'PR', label: 'MR !12', url: 'https://gitlab/12' },
    ]),
  createLink: vi.fn().mockResolvedValue({ id: 2 }),
  deleteLink: vi.fn().mockResolvedValue(undefined),
}));

describe('LinkPanel', () => {
  beforeEach(() => vi.clearAllMocks());

  /** TC-LP-01: renders existing links with type chip + count + url link. */
  it('renders existing links (TC-LP-01)', async () => {
    render(<LinkPanel targetType="TASK" targetId={7} />);
    await waitFor(() => expect(screen.getByTestId('link-item-1')).toBeInTheDocument());
    expect(listLinks).toHaveBeenCalledWith('TASK', 7);
    expect(screen.getByTestId('link-panel')).toHaveTextContent('关联产物（1）');
    expect(screen.getByTestId('link-item-1').querySelector('a')).toHaveAttribute('href', 'https://gitlab/12');
  });

  /** TC-LP-02: add a link sends the typed body; URL required (button disabled when empty). */
  it('adds a link with the chosen type + url (TC-LP-02)', async () => {
    render(<LinkPanel targetType="STORY" targetId={9} />);
    await waitFor(() => expect(screen.getByTestId('link-add-btn')).toBeInTheDocument());
    expect(screen.getByTestId('link-add-btn')).toBeDisabled(); // url empty
    fireEvent.change(screen.getByTestId('link-add-type'), { target: { value: 'DESIGN' } });
    fireEvent.change(screen.getByTestId('link-add-url'), { target: { value: 'https://figma/abc' } });
    fireEvent.click(screen.getByTestId('link-add-btn'));
    await waitFor(() => expect(createLink).toHaveBeenCalledTimes(1));
    expect(createLink).toHaveBeenCalledWith(
      expect.objectContaining({ targetType: 'STORY', targetId: 9, linkType: 'DESIGN', url: 'https://figma/abc' }),
    );
  });

  /** TC-LP-03: delete removes a link. */
  it('deletes a link (TC-LP-03)', async () => {
    render(<LinkPanel targetType="TASK" targetId={7} />);
    await waitFor(() => expect(screen.getByTestId('link-delete-1')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('link-delete-1'));
    await waitFor(() => expect(deleteLink).toHaveBeenCalledWith(1));
  });
});
