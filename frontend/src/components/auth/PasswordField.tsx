import { Eye, EyeOff, LockKeyhole } from 'lucide-react';
import { useState } from 'react';
import type { ChangeEventHandler } from 'react';

interface PasswordFieldProps {
  id: string;
  value: string;
  onChange: ChangeEventHandler<HTMLInputElement>;
  disabled: boolean;
  error?: string;
}

export function PasswordField({ id, value, onChange, disabled, error }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="login-field">
      <label htmlFor={id}>Senha</label>
      <div className={`login-field__control${error ? ' login-field__control--error' : ''}`}>
        <LockKeyhole aria-hidden="true" className="login-field__icon" size={19} strokeWidth={1.8} />
        <input
          id={id}
          name="password"
          type={visible ? 'text' : 'password'}
          autoComplete="current-password"
          value={value}
          onChange={onChange}
          disabled={disabled}
          required
          aria-required="true"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
        />
        <button
          className="password-visibility"
          type="button"
          disabled={disabled}
          aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
          aria-pressed={visible}
          onClick={() => setVisible((current) => !current)}
        >
          {visible
            ? <EyeOff aria-hidden="true" size={19} strokeWidth={1.8} />
            : <Eye aria-hidden="true" size={19} strokeWidth={1.8} />}
        </button>
      </div>
      {error && <p className="field-error" id={`${id}-error`}>{error}</p>}
    </div>
  );
}