import { useEffect, useRef } from 'react';
import { Trash2 } from 'lucide-react';

interface ClearCartConfirmationProps {
  pending: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

export function ClearCartConfirmation({ pending, onCancel, onConfirm }: ClearCartConfirmationProps) {
  const confirmButton = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    confirmButton.current?.focus();
  }, []);

  return (
    <div className="cart-dialog-backdrop" onKeyDown={(event) => {
      if (event.key === 'Escape' && !pending) onCancel();
    }}>
      <section className="cart-dialog" role="dialog" aria-modal="true" aria-labelledby="clear-cart-title">
        <span className="cart-dialog__icon"><Trash2 size={20} aria-hidden="true" /></span>
        <h2 id="clear-cart-title">Limpar carrinho?</h2>
        <p>Todos os produtos serão removidos do seu carrinho.</p>
        <div className="cart-dialog__actions">
          <button className="button button--outline" type="button" disabled={pending} onClick={onCancel}>Cancelar</button>
          <button ref={confirmButton} className="button button--danger" type="button" disabled={pending} onClick={onConfirm}>
            {pending ? 'Limpando…' : 'Confirmar limpeza'}
          </button>
        </div>
      </section>
    </div>
  );
}
