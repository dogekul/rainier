import type { InputHTMLAttributes, ReactNode } from 'react';
import './Input.css';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: ReactNode;
}

export function Input({ className = '', label, ...rest }: InputProps) {
  const input = (
    <input className={`rainier-input ${className}`.trim()} {...rest} />
  );
  if (label === undefined) return input;
  return (
    <label className="rainier-input-wrap">
      <span className="rainier-input-label">{label}</span>
      {input}
    </label>
  );
}
