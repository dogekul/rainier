import type { HTMLAttributes } from 'react';
import './Card.css';

export function Card({ className = '', ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={`rainier-card ${className}`.trim()} {...rest} />;
}
