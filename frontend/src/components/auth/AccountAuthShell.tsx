import { ShoppingCart } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';

interface AccountAuthShellProps {
  eyebrow: string;
  title: string;
  description: string;
  children: ReactNode;
}

export function AccountAuthShell({ eyebrow, title, description, children }: AccountAuthShellProps) {
  return (
    <main className="account-flow-screen">
      <section className="account-flow-panel" aria-labelledby="account-flow-title">
        <Link className="login-brand login-brand--panel" to="/" aria-label="TechStore, página inicial">
          <ShoppingCart aria-hidden="true" size={28} strokeWidth={2.4} />
          <span>Tech<span>Store</span></span>
        </Link>
        <p className="login-eyebrow account-flow-eyebrow">{eyebrow}</p>
        <h1 id="account-flow-title">{title}</h1>
        <p className="account-flow-description">{description}</p>
        {children}
      </section>
    </main>
  );
}
