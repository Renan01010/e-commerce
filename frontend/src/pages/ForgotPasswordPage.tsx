import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { AccountAuthShell } from '../components/auth/AccountAuthShell';
import { AuthServiceError, authService } from '../services/authService';
import './LoginPage.css';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedEmail = email.trim();
    if (!EMAIL_PATTERN.test(normalizedEmail)) {
      setError('Informe um e-mail válido.');
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await authService.requestPasswordReset(normalizedEmail);
      setMessage('Se houver uma conta associada a esse endereço, você receberá instruções para redefinir a senha.');
    } catch (submitError) {
      setError(submitError instanceof AuthServiceError
        ? submitError.message
        : 'Não foi possível processar a solicitação. Tente novamente.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AccountAuthShell eyebrow="RECUPERAÇÃO DE CONTA" title="Esqueceu sua senha?"
      description="Informe o e-mail da sua conta para solicitar instruções de redefinição.">
      {message ? <p className="account-flow-result" role="status">{message}</p> : (
        <form className="login-form account-flow-form" noValidate onSubmit={submit}>
          <div className="login-field">
            <label htmlFor="forgot-email">E-mail</label>
            <div className="login-field__control">
              <input id="forgot-email" type="email" autoComplete="email" inputMode="email" required value={email}
                onChange={(event) => setEmail(event.target.value)} disabled={isSubmitting} />
            </div>
          </div>
          {error && <p className="login-error" role="alert">{error}</p>}
          <button className="login-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Enviando solicitação...' : 'Solicitar redefinição'}
          </button>
        </form>
      )}
      <p className="account-flow-footer"><Link to="/login">Voltar ao login</Link></p>
    </AccountAuthShell>
  );
}
