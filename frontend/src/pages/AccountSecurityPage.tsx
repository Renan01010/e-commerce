import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AccountServiceError, accountService } from '../services/accountService';
import { useAuthStore } from '../store/authStore';
import './AccountPages.css';

const PASSWORD_MIN_LENGTH = 8;
const PASSWORD_MAX_LENGTH = 72;

export function AccountSecurityPage() {
  const navigate = useNavigate();
  const clearSession = useAuthStore((state) => state.clearSession);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentPassword) {
      setError('Informe sua senha atual.');
      return;
    }
    if (newPassword.length < PASSWORD_MIN_LENGTH || newPassword.length > PASSWORD_MAX_LENGTH) {
      setError('A nova senha deve ter entre 8 e 72 caracteres.');
      return;
    }
    if (newPassword !== confirmation) {
      setError('As senhas não coincidem.');
      return;
    }
    setError(null);
    setIsSaving(true);
    try {
      await accountService.changePassword(currentPassword, newPassword, confirmation);
      clearSession();
      navigate('/login', { replace: true });
    } catch (saveError) {
      setError(saveError instanceof AccountServiceError
        ? saveError.message
        : 'Não foi possível alterar a senha. Tente novamente.');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <main className="account-page">
      <header className="account-page__heading">
        <p className="eyebrow">SUA CONTA</p>
        <h1>Segurança</h1>
        <p>Altere sua senha para manter sua conta protegida.</p>
      </header>
      <nav className="account-tabs" aria-label="Seções da conta">
        <Link to="/account">Perfil</Link>
        <span aria-current="page">Segurança</span>
      </nav>
      <section className="account-card" aria-labelledby="security-heading">
        <h2 id="security-heading">Alterar senha</h2>
        <form className="account-form" onSubmit={changePassword}>
          <label htmlFor="current-password">Senha atual</label>
          <input id="current-password" type="password" autoComplete="current-password" required
            value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} disabled={isSaving} />
          <label htmlFor="new-password">Nova senha</label>
          <input id="new-password" type="password" autoComplete="new-password" minLength={PASSWORD_MIN_LENGTH}
            maxLength={PASSWORD_MAX_LENGTH} required value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)} disabled={isSaving} />
          <label htmlFor="confirm-new-password">Confirmar nova senha</label>
          <input id="confirm-new-password" type="password" autoComplete="new-password" required value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)} disabled={isSaving} />
          {error && <p className="account-error" role="alert">{error}</p>}
          <p className="account-hint">Após a alteração, entre novamente com sua nova senha.</p>
          <button className="account-button" type="submit" disabled={isSaving}>
            {isSaving ? 'Atualizando...' : 'Alterar senha'}
          </button>
        </form>
      </section>
    </main>
  );
}
