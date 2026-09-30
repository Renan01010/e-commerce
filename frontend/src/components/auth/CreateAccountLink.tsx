import { Link } from 'react-router-dom';

export function CreateAccountLink() {
  return (
    <p className="create-account-link">
      Ainda não tem uma conta? <Link to="/register">Criar conta</Link>
    </p>
  );
}