import type { ButtonHTMLAttributes } from 'react';
import './Button.css';

export function Button({ className = '', ...rest }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button className={`rainier-button ${className}`.trim()} {...rest} />;
}
