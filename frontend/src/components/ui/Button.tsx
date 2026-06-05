import type { ButtonHTMLAttributes } from 'react';
import './Button.css';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary';
}

export function Button({ className = '', variant = 'primary', ...rest }: ButtonProps) {
  return (
    <button
      className={`rainier-button rainier-button-${variant} ${className}`.trim()}
      {...rest}
    />
  );
}
