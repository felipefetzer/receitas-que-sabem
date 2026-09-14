import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import SideMenu from "../components/SideMenu";

export default function HomePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function sair() {
    await logout();
    navigate("/login");
  }

  return (
    <div className="page">
      <div className="topbar">
        <div className="row">
          <SideMenu />
          <div>
            <h1>Receitas que Sabem</h1>
            <p className="muted small" style={{ margin: 0 }}>Olá, {user?.username}</p>
          </div>
        </div>
        <button onClick={sair}>Sair</button>
      </div>

      <nav className="menu">
        <Link className="menu-item" to="/recipes">
          <strong>Ver receitas</strong>
          <span>As suas e as de toda a gente</span>
        </Link>
        <Link className="menu-item" to="/recipes/new">
          <strong>Criar receita</strong>
          <span>Ingredientes, preparação e notas</span>
        </Link>
        <Link className="menu-item" to="/shopping-list">
          <strong>Lista de compras</strong>
          <span>O que falta para as receitas ativas</span>
        </Link>
        <Link className="menu-item" to="/about">
          <strong>Sobre</strong>
          <span>Como funciona a aplicação</span>
        </Link>
      </nav>
    </div>
  );
}
