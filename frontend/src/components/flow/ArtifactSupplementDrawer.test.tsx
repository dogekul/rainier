import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ArtifactSupplementDrawer } from './ArtifactSupplementDrawer';
import { createOpportunityArtifact } from '../../api/opportunityArtifact';

vi.mock('../../api/opportunityArtifact', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunityArtifact')>()),
  createOpportunityArtifact: vi.fn(() => Promise.resolve({ id: 1 } as never)),
}));

describe('ArtifactSupplementDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('does not render fields when opportunityId is null', () => {
    render(
      <ArtifactSupplementDrawer
        opportunityId={null}
        missingTypes={['POC_SCORE']}
        testIdPrefix="presale-supp"
        onClose={() => undefined}
        onAdvance={() => undefined}
      />,
    );
    expect(screen.queryByTestId('presale-supp-POC_SCORE')).toBeNull();
  });

  it('blocks submit when a required report body is empty', async () => {
    const onAdvance = vi.fn();
    render(
      <ArtifactSupplementDrawer
        opportunityId={11}
        missingTypes={['POC_SCORE']}
        testIdPrefix="presale-supp"
        onClose={() => undefined}
        onAdvance={onAdvance}
      />,
    );
    expect(screen.getByTestId('presale-supp-POC_SCORE')).toBeTruthy();
    fireEvent.click(screen.getByTestId('presale-supp-save'));
    await waitFor(() => {
      expect(screen.getByTestId('presale-supp-error').textContent).toMatch(/POC 得分表/);
    });
    expect(onAdvance).not.toHaveBeenCalled();
    expect(createOpportunityArtifact).not.toHaveBeenCalled();
  });

  it('blocks submit when a required link list is empty', async () => {
    render(
      <ArtifactSupplementDrawer
        opportunityId={22}
        missingTypes={['SURVEY_ATTACHMENT']}
        testIdPrefix="delivery-supp"
        onClose={() => undefined}
        onAdvance={() => undefined}
      />,
    );
    fireEvent.click(screen.getByTestId('delivery-supp-save'));
    await waitFor(() => {
      expect(screen.getByTestId('delivery-supp-error').textContent).toMatch(/链接/);
    });
    expect(createOpportunityArtifact).not.toHaveBeenCalled();
  });

  it('persists every missing artifact then calls onAdvance with the id', async () => {
    const onAdvance = vi.fn();
    render(
      <ArtifactSupplementDrawer
        opportunityId={77}
        missingTypes={['SURVEY_REPORT', 'SURVEY_ATTACHMENT']}
        testIdPrefix="delivery-supp"
        onClose={() => undefined}
        onAdvance={onAdvance}
      />,
    );
    // report body
    fireEvent.change(screen.getByTestId('delivery-supp-content-SURVEY_REPORT'), {
      target: { value: '现场调研笔记' },
    });
    // attachment link
    fireEvent.change(screen.getByTestId('delivery-supp-link-SURVEY_ATTACHMENT-0'), {
      target: { value: 'https://files.example.com/a.pdf' },
    });
    fireEvent.click(screen.getByTestId('delivery-supp-save'));
    await waitFor(() => {
      expect(onAdvance).toHaveBeenCalledWith(77);
    });
    expect(createOpportunityArtifact).toHaveBeenCalledTimes(2);
    expect(createOpportunityArtifact).toHaveBeenCalledWith(77, {
      type: 'SURVEY_REPORT',
      title: undefined,
      content: '现场调研笔记',
    });
    expect(createOpportunityArtifact).toHaveBeenCalledWith(77, {
      type: 'SURVEY_ATTACHMENT',
      link: 'https://files.example.com/a.pdf',
    });
  });

  it('supports multiple links per artifact (add / remove)', async () => {
    render(
      <ArtifactSupplementDrawer
        opportunityId={88}
        missingTypes={['SURVEY_ATTACHMENT']}
        testIdPrefix="delivery-supp"
        onClose={() => undefined}
        onAdvance={() => undefined}
      />,
    );
    fireEvent.click(screen.getByTestId('delivery-supp-addlink-SURVEY_ATTACHMENT'));
    expect(screen.getByTestId('delivery-supp-link-SURVEY_ATTACHMENT-1')).toBeTruthy();
    // remove the new one
    fireEvent.click(screen.getByTestId('delivery-supp-rmlink-SURVEY_ATTACHMENT-1'));
    expect(screen.queryByTestId('delivery-supp-link-SURVEY_ATTACHMENT-1')).toBeNull();
  });
});
