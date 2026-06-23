import { Fragment, type ReactNode } from 'react';

/**
 * Minimal, dependency-free, XSS-safe Markdown → React renderer (v0.0.45). Supports headings (# ## ###),
 * **bold**, *italic*, `code`, unordered (-, *) and ordered (1.) lists, and blank-line paragraphs with
 * soft line breaks. Builds React nodes directly (never dangerouslySetInnerHTML), so user content cannot
 * inject markup. Used to preview 流转产出物 (《商机调研报告》/《决策评审纪要》) as rich text.
 */
function renderInline(text: string): ReactNode[] {
  const out: ReactNode[] = [];
  const re = /(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`)/g;
  let last = 0;
  let k = 0;
  let m: RegExpExecArray | null;
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) out.push(text.slice(last, m.index));
    const tok = m[0];
    if (tok.startsWith('**')) {
      out.push(<strong key={k++}>{tok.slice(2, -2)}</strong>);
    } else if (tok.startsWith('`')) {
      out.push(
        <code
          key={k++}
          style={{ background: 'var(--rainier-bg-hover)', padding: '0 4px', borderRadius: 3 }}
        >
          {tok.slice(1, -1)}
        </code>,
      );
    } else {
      out.push(<em key={k++}>{tok.slice(1, -1)}</em>);
    }
    last = m.index + tok.length;
  }
  if (last < text.length) out.push(text.slice(last));
  return out;
}

const HEADING = /^(#{1,3})\s+(.*)$/;
const ULI = /^\s*[-*]\s+/;
const OLI = /^\s*\d+\.\s+/;
const BLANK = /^\s*$/;

export interface MarkdownViewProps {
  content?: string | null;
  testId?: string;
}

export function MarkdownView({ content, testId }: MarkdownViewProps) {
  const lines = (content ?? '').split('\n');
  const blocks: ReactNode[] = [];
  let i = 0;
  let key = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (BLANK.test(line)) {
      i++;
      continue;
    }
    const h = HEADING.exec(line);
    if (h) {
      const level = h[1].length;
      const Tag = (['h3', 'h4', 'h5'] as const)[level - 1];
      blocks.push(
        <Tag key={key++} style={{ margin: '8px 0 4px', fontSize: 16 - level }}>
          {renderInline(h[2])}
        </Tag>,
      );
      i++;
      continue;
    }
    if (ULI.test(line)) {
      const items: ReactNode[] = [];
      while (i < lines.length && ULI.test(lines[i])) {
        items.push(<li key={items.length}>{renderInline(lines[i].replace(ULI, ''))}</li>);
        i++;
      }
      blocks.push(
        <ul key={key++} style={{ margin: '4px 0', paddingLeft: 18 }}>
          {items}
        </ul>,
      );
      continue;
    }
    if (OLI.test(line)) {
      const items: ReactNode[] = [];
      while (i < lines.length && OLI.test(lines[i])) {
        items.push(<li key={items.length}>{renderInline(lines[i].replace(OLI, ''))}</li>);
        i++;
      }
      blocks.push(
        <ol key={key++} style={{ margin: '4px 0', paddingLeft: 18 }}>
          {items}
        </ol>,
      );
      continue;
    }
    const para: string[] = [];
    while (
      i < lines.length &&
      !BLANK.test(lines[i]) &&
      !HEADING.test(lines[i]) &&
      !ULI.test(lines[i]) &&
      !OLI.test(lines[i])
    ) {
      para.push(lines[i]);
      i++;
    }
    blocks.push(
      <p key={key++} style={{ margin: '4px 0' }}>
        {para.map((l, idx) => (
          <Fragment key={idx}>
            {idx > 0 ? <br /> : null}
            {renderInline(l)}
          </Fragment>
        ))}
      </p>,
    );
  }
  return (
    <div data-testid={testId} className="rainier-markdown" style={{ fontSize: 13, lineHeight: 1.6 }}>
      {blocks}
    </div>
  );
}
