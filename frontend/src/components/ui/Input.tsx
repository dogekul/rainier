import type { InputHTMLAttributes } from 'react';
import './Input.css';

export function Input({ className = '', ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={`rainier-input ${className}`.trim()} {...rest} />;
}
