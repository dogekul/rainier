import type { ReactNode } from 'react';

/** v0.0.34 — tiny 16px line-icon set for the Sider nav. stroke=currentColor so it inherits item color. */
const PATHS: Record<string, ReactNode> = {
  home: <path d="M2.5 7.5 8 3l5.5 4.5M4 6.7V13h8V6.7" />,
  edit: (
    <>
      <path d="M3 13h10" />
      <path d="M3.5 10.5 10 4l2 2-6.5 6.5-2.5.5z" />
    </>
  ),
  users: (
    <>
      <circle cx="6" cy="6" r="2.2" />
      <path d="M2.5 13c0-2 1.6-3.2 3.5-3.2" />
      <path d="M10.5 4.2A2.2 2.2 0 0 1 11 8.6M11 9.8c1.7.1 2.6 1.3 2.6 3.2" />
    </>
  ),
  map: <path d="M6 2 2 4v10l4-2 4 2 4-2V2l-4 2-4-2zM6 2v10M10 4v10" />,
  sitemap: (
    <>
      <rect x="6" y="2" width="4" height="3" rx="0.5" />
      <rect x="2" y="11" width="4" height="3" rx="0.5" />
      <rect x="10" y="11" width="4" height="3" rx="0.5" />
      <path d="M8 5v3M4 11V8h8v3" />
    </>
  ),
  user: (
    <>
      <circle cx="8" cy="5.5" r="2.4" />
      <path d="M3.5 13.5c0-2.5 2-4 4.5-4s4.5 1.5 4.5 4" />
    </>
  ),
  link: (
    <>
      <path d="M6.5 9.5 9.5 6.5" />
      <path d="M7 4.5 8.5 3a2.5 2.5 0 0 1 3.5 3.5L10.5 8" />
      <path d="M9 11.5 7.5 13A2.5 2.5 0 0 1 4 9.5L5.5 8" />
    </>
  ),
  box: <path d="M8 2.5 13 5v6l-5 2.5L3 11V5z M3 5l5 2.5L13 5M8 7.5V13" />,
  layers: <path d="M8 2.5 14 5 8 7.5 2 5zM2.5 8.2 8 10.7l5.5-2.5M2.5 11 8 13.5 13.5 11" />,
  star: <path d="M8 2.5l1.7 3.5 3.8.5-2.8 2.6.7 3.8L8 11.1 4.6 12.9l.7-3.8-2.8-2.6 3.8-.5z" />,
  gauge: (
    <>
      <path d="M2.5 11a5.5 5.5 0 0 1 11 0" />
      <path d="M8 11l3-3" />
    </>
  ),
  folder: <path d="M2 4.5h4L7.5 6H14v7.5H2z" />,
  loop: (
    <>
      <path d="M3 8a5 5 0 0 1 8.5-3.5L13 6" />
      <path d="M13 8a5 5 0 0 1-8.5 3.5L3 10" />
      <path d="M13 3v3h-3M3 13v-3h3" />
    </>
  ),
  check: (
    <>
      <rect x="2.5" y="2.5" width="11" height="11" rx="2" />
      <path d="M5.5 8l1.8 1.8L11 6" />
    </>
  ),
  inbox: <path d="M2.5 9 4 3.5h8L13.5 9v4.5h-11zM2.5 9h3.5l1 1.8h2L10 9h3.5" />,
  doc: <path d="M4 2.5h5l3 3v8H4zM9 2.5V5.5h3M6 8.5h4M6 11h4" />,
  badge: (
    <>
      <rect x="3" y="2.5" width="10" height="11" rx="1.5" />
      <circle cx="8" cy="6.5" r="1.8" />
      <path d="M5.5 11.5c.6-1.4 4.4-1.4 5 0" />
    </>
  ),
  key: (
    <>
      <circle cx="5.5" cy="5.5" r="2.8" />
      <path d="M7.5 7.5 13 13M11 11l1.5-1.5M9.5 9.5 11 8" />
    </>
  ),
  shield: <path d="M8 2.5l5 1.8v4c0 3-2.2 4.8-5 5.7-2.8-.9-5-2.7-5-5.7v-4z" />,
};

export function NavIcon({ name }: { name?: string }) {
  const path = (name && PATHS[name]) || PATHS.doc;
  return (
    <svg
      className="rainier-nav-icon"
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {path}
    </svg>
  );
}
