import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { AccountServiceError, accountService } from '../services/accountService';
import { useAuthStore } from '../store/authStore';
import type { UserProfile } from '../types/auth';
import './AccountPages.css';

export function AccountPage() {
  const profile = useAuthStore((state) => state.profile);
  const setProfile = useAuthStore((state) => state.setProfile);
  const [name, setName] = useState(profile?.name ?? '');
  const [isLoading, setIsLoading] = useState(!profile);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    void accountService.getProfile().then((loadedProfile) => {
      if (!active) return;
      setProfile(loadedProfile);
      setName(loadedProfile.name ?? '');
    }).catch((loadError: unknown) => {
      if (active) setError(loadError instanceof AccountServiceError
        ? loadError.message
        : 'Não foi possível carregar os dados da conta.');
    }).finally(() => {
      if (active) setIsLoading(false);
    });
    return () => { active = false; };
  }, [setProfile]);

  async function saveName(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedName = name.trim();
    if (normalizedName.length < 1 || normalizedName.length > 100) {
      setError('O nome deve ter entre 1 e 100 caracteres.');
      return;
    }
    setError(null);
    setMessage(null);
    setIsSaving(true);
    try {
      const updatedProfile = await accountService.updateName(normalizedName);
      setProfile(updatedProfile);
      setName(updatedProfile.name ?? '');
      setMessage('Nome atualizado.');
    } catch (saveError) {
      setError(saveError instanceof AccountServiceError ? saveError.message : 'Não foi possível salvar seu nome.');
    } finally {
      setIsSaving(false);
    }
  }

  const accountProfile: UserProfile | null = profile;

  return (
    <main className="account-page">
      <header className="account-page__heading">
        <p className="eyebrow">SUA CONTA</p>
        <h1>Minha conta</h1>
        <p>Consulte seus dados e mantenha seu perfil atualizado.</p>
      </header>
      <nav className="account-tabs" aria-label="Seções da conta">
        <span aria-current="page">Perfil</span>
        <Link to="/account/security">Segurança</Link>
      </nav>
      <section className="account-card" aria-labelledby="profile-heading">
        <h2 id="profile-heading">Dados do perfil</h2>
        {isLoading ? <p role="status">Carregando seu perfil...</p> : accountProfile ? (
          <>
            <form className="account-form" onSubmit={saveName}>
              <label htmlFor="account-name">Nome</label>
              <input id="account-name" name="name" type="text" maxLength={100} required value={name}
                onChange={(event) => setName(event.target.value)} disabled={isSaving} />
              <label htmlFor="account-email">E-mail</label>
              <input id="account-email" type="email" value={accountProfile.email} readOnly />
              <p className="account-verification">
                E-mail {accountProfile.emailVerified ? 'confirmado' : 'ainda não confirmado'}
              </p>
              {error && <p className="account-error" role="alert">{error}</p>}
              {message && <p className="account-success" role="status">{message}</p>}
              <button className="account-button" type="submit" disabled={isSaving}>
                {isSaving ? 'Salvando...' : 'Salvar nome'}
              </button>
            </form>
          </>
        ) : <p className="account-error" role="alert">{error ?? 'Não foi possível carregar o perfil.'}</p>}
      </section>
    </main>
  );
}
