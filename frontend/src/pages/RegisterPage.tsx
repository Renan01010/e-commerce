import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { AccountAuthShell } from '../components/auth/AccountAuthShell';
import { AuthServiceError, authService } from '../services/authService';
import './LoginPage.css';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_MIN_LENGTH = 8;
const PASSWORD_MAX_LENGTH = 72;

export function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting) return;
    const normalizedName = name.trim();
    const normalizedEmail = email.trim();
    if (!normalizedName || normalizedName.length > 100) {
      setError('Informe um nome entre 1 e 100 caracteres.');
      return;
    }
    if (!EMAIL_PATTERN.test(normalizedEmail) || normalizedEmail.length > 254) {
      setError('Informe um e-mail válido.');
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

    setIsSubmitting(true);
    setError(null);
    try {
      const response = await authService.register({
        name: normalizedName,
        email: normalizedEmail,
        password,
        confirmPassword: confirmation,
      });
      setSuccess(response.emailSent === false || response.emailDeliveryStatus?.toLowerCase() === 'failed'
        ? 'Conta criada, mas não foi possível enviar as instruções agora. Solicite um novo link de confirmação.'
        : 'Conta criada. A confirmação do e-mail está pendente. Você pode solicitar um novo link se necessário.');
    } catch (submitError) {
      setError(submitError instanceof AuthServiceError
        ? submitError.message
        : 'Não foi possível criar sua conta. Tente novamente.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AccountAuthShell
      eyebrow="NOVA CONTA"
      title="Crie sua conta"
      description="Informe seus dados para começar a usar os recursos da TechStore."
    >
      {success ? (
        <div className="account-flow-result" role="status">
          <p>{success}</p>
          <Link className="login-submit account-flow-link-button" to="/verify-email/resend">Solicitar confirmação</Link>
          <Link className="account-flow-secondary-link" to="/login">Voltar ao login</Link>
        </div>
      ) : (
        <form className="login-form account-flow-form" noValidate onSubmit={submit}>
          <div className="login-field">
            <label htmlFor="register-name">Nome</label>
            <div className="login-field__control">
              <input id="register-name" name="name" type="text" autoComplete="name" maxLength={100} required
                value={name} onChange={(event) => setName(event.target.value)} disabled={isSubmitting} />
            </div>
          </div>
          <div className="login-field">
            <label htmlFor="register-email">E-mail</label>
            <div className="login-field__control">
              <input id="register-email" name="email" type="email" autoComplete="email" maxLength={254}
                inputMode="email" required value={email} onChange={(event) => setEmail(event.target.value)}
                disabled={isSubmitting} />
            </div>
          </div>
          <div className="login-field">
            <label htmlFor="register-password">Senha</label>
            <div className="login-field__control">
              <input id="register-password" name="password" type="password" autoComplete="new-password"
                minLength={PASSWORD_MIN_LENGTH} maxLength={PASSWORD_MAX_LENGTH} required value={password}
                onChange={(event) => setPassword(event.target.value)} disabled={isSubmitting} />
            </div>
          </div>
          <div className="login-field">
            <label htmlFor="register-password-confirmation">Confirmar senha</label>
            <div className="login-field__control">
              <input id="register-password-confirmation" name="passwordConfirmation" type="password"
                autoComplete="new-password" required value={confirmation}
                onChange={(event) => setConfirmation(event.target.value)} disabled={isSubmitting} />
            </div>
          </div>
          {error && <p className="login-error" role="alert">{error}</p>}
          <button className="login-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Criando conta...' : 'Criar conta'}
          </button>
          {isSubmitting && <span className="visually-hidden" role="status">Criando sua conta</span>}
        </form>
      )}
      <p className="account-flow-footer">Já tem uma conta? <Link to="/login">Entrar</Link></p>
    </AccountAuthShell>
  );
}
