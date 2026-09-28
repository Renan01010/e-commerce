import { ArrowRight, LoaderCircle, Mail } from 'lucide-react';
import { useState } from 'react';
import type { FormEvent } from 'react';
import type { LoginCredentials } from '../../types/auth';
import { PasswordField } from './PasswordField';

interface LoginFormProps {
  onSubmit: (credentials: LoginCredentials) => Promise<void> | void;
  isSubmitting?: boolean;
  errorMessage?: string | null;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function LoginForm({ onSubmit, isSubmitting = false, errorMessage = null }: LoginFormProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState<string | undefined>();
  const [passwordError, setPasswordError] = useState<string | undefined>();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting) return;

    const normalizedEmail = email.trim();
    const nextEmailError = normalizedEmail.length === 0
      ? 'Informe seu e-mail.'
      : EMAIL_PATTERN.test(normalizedEmail) ? undefined : 'Informe um e-mail válido.';
    const nextPasswordError = password.length === 0 ? 'Informe sua senha.' : undefined;
    setEmailError(nextEmailError);
    setPasswordError(nextPasswordError);

    if (nextEmailError || nextPasswordError) return;
    await onSubmit({ email: normalizedEmail, password });
  }

  return (
    <form className="login-form" noValidate onSubmit={handleSubmit}>
      <div className="login-form__heading">
        <p className="login-eyebrow">BEM-VINDO DE VOLTA</p>
        <h1>Acesse sua conta</h1>
        <p>Entre para continuar explorando tecnologia.</p>
      </div>

      <div className="login-field">
        <label htmlFor="login-email">E-mail</label>
        <div className={`login-field__control${emailError ? ' login-field__control--error' : ''}`}>
          <Mail aria-hidden="true" className="login-field__icon" size={19} strokeWidth={1.8} />
          <input
            id="login-email"
            name="email"
            type="email"
            autoComplete="username"
            inputMode="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            disabled={isSubmitting}
            required
            aria-required="true"
            aria-invalid={Boolean(emailError)}
            aria-describedby={emailError ? 'login-email-error' : undefined}
            placeholder="seu@email.com"
          />
        </div>
        {emailError && <p className="field-error" id="login-email-error">{emailError}</p>}
      </div>

      <PasswordField
        id="login-password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        disabled={isSubmitting}
        error={passwordError}
      />

      {errorMessage && <p className="login-error" role="alert">{errorMessage}</p>}

      <button className="login-submit" type="submit" disabled={isSubmitting}>
        {isSubmitting ? (
          <><LoaderCircle aria-hidden="true" className="login-submit__spinner" size={18} /> Entrando...</>
        ) : (
          <>Entrar <ArrowRight aria-hidden="true" size={19} strokeWidth={2} /></>
        )}
      </button>
      {isSubmitting && <span className="visually-hidden" role="status">Autenticando sua conta</span>}
    </form>
  );
}