import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AccountAuthShell } from '../components/auth/AccountAuthShell';
import { AuthServiceError, authService } from '../services/authService';
import './LoginPage.css';

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token')?.trim() ?? '';
  const started = useRef(false);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>(token ? 'loading' : 'error');
  const [message, setMessage] = useState(token ? '' : 'O link está incompleto. Solicite uma nova confirmação.');

  useEffect(() => {
    if (!token || started.current) return;
    started.current = true;
    void authService.verifyEmail(token).then(() => {
      setStatus('success');
      setMessage('Seu e-mail foi confirmado. Agora você pode entrar na sua conta.');
    }).catch((error: unknown) => {
      setStatus('error');
      setMessage(error instanceof AuthServiceError ? error.message : 'Não foi possível confirmar o e-mail.');
    });
  }, [token]);

  return (
    <AccountAuthShell eyebrow="CONFIRMAÇÃO DE E-MAIL" title={status === 'success' ? 'E-mail confirmado' : 'Confirmando seu e-mail'}
      description={status === 'loading' ? 'Aguarde enquanto validamos o link de confirmação.' : message}>
      {status === 'loading' && <p className="account-flow-status" role="status">Validando link...</p>}
      {status === 'error' && <p className="login-error" role="alert">{message}</p>}
      <div className="account-flow-links">
        {status === 'error' && <Link to="/verify-email/resend">Solicitar novo link</Link>}
        <Link to="/login">Ir para o login</Link>
      </div>
    </AccountAuthShell>
  );
}
