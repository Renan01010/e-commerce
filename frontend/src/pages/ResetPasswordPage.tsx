import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AccountAuthShell } from '../components/auth/AccountAuthShell';
import { AuthServiceError, authService } from '../services/authService';
import { useAuthStore } from '../store/authStore';
import './LoginPage.css';

const PASSWORD_MIN_LENGTH = 8;
const PASSWORD_MAX_LENGTH = 72;

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token')?.trim() ?? '';
  const clearSession = useAuthStore((state) => state.clearSession);
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(token ? null : 'O link está incompleto. Solicite uma nova redefinição.');

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      setError('O link está incompleto. Solicite uma nova redefinição.');
      return;
    }
    if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) {
      setError('A senha deve ter entre 8 e 72 caracteres.');
      return;
    }
    if (password !== confirmation) {
      setError('As senhas não coincidem.');
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await authService.resetPassword(token, password, confirmation);
      clearSession();
      setMessage('Senha redefinida. Entre novamente com sua nova senha.');
    } catch (submitError) {
      setError(submitError instanceof AuthServiceError
        ? submitError.message
        : 'Não foi possível redefinir a senha. Solicite um novo link.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AccountAuthShell eyebrow="RECUPERAÇÃO DE CONTA" title="Defina uma nova senha"
      description="Escolha uma senha segura para voltar a acessar sua conta.">
      {message ? (
        <div className="account-flow-result" role="status">
          <p>{message}</p><Link to="/login">Ir para o login</Link>
        </div>
      ) : (
        <form className="login-form account-flow-form" noValidate onSubmit={submit}>
          <div className="login-field">
            <label htmlFor="reset-password">Nova senha</label>
            <div className="login-field__control">
              <input id="reset-password" type="password" autoComplete="new-password" minLength={PASSWORD_MIN_LENGTH}
                maxLength={PASSWORD_MAX_LENGTH} required value={password}
                onChange={(event) => setPassword(event.target.value)} disabled={isSubmitting || !token} />
            </div>
          </div>
          <div className="login-field">
            <label htmlFor="reset-password-confirmation">Confirmar nova senha</label>
            <div className="login-field__control">
              <input id="reset-password-confirmation" type="password" autoComplete="new-password" required
                value={confirmation} onChange={(event) => setConfirmation(event.target.value)}
                disabled={isSubmitting || !token} />
            </div>
          </div>
          {error && <p className="login-error" role="alert">{error}</p>}
          {token && <button className="login-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando senha...' : 'Redefinir senha'}
          </button>}
        </form>
      )}
      {!message && <p className="account-flow-footer"><Link to="/forgot-password">Solicitar outro link</Link></p>}
    </AccountAuthShell>
  );
}
