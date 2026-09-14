import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const LINKS = [
  { to: "/", label: "Início", end: true },
  { to: "/recipes", label: "Ver receitas", end: false },
  { to: "/recipes/new", label: "Criar receita", end: true },
  { to: "/shopping-list", label: "Lista de compras", end: true },
  { to: "/about", label: "Sobre", end: true },
];

// O botão e a gaveta vivem no mesmo componente: a gaveta é `position: fixed`,
// por isso não importa onde fica na árvore. Cada página só tem de pôr
// <SideMenu /> na sua topbar.
export default function SideMenu() {
  const [open, setOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Fechar com Esc e travar o scroll do fundo enquanto a gaveta está aberta.
  useEffect(() => {
    if (!open) return;
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("keydown", onKey);
    const overflowAnterior = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = overflowAnterior;
    };
  }, [open]);

  async function sair() {
    setOpen(false);
    await logout();
    navigate("/login");
  }

  return (
    <>
      <button
        className="menu-toggle"
        type="button"
        aria-label="Abrir menu"
        aria-expanded={open}
        onClick={() => setOpen(true)}
      >
        ☰
      </button>

      {open && (
        <div className="drawer-overlay" onClick={() => setOpen(false)}>
          <nav
            className="drawer"
            aria-label="Menu principal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="drawer-head">
              <div>
                <strong>Receitas que Sabem</strong>
                {user && <div className="muted small">{user.username}</div>}
              </div>
              <button
                className="tiny"
                type="button"
                aria-label="Fechar menu"
                onClick={() => setOpen(false)}
              >
                ✕
              </button>
            </div>

            {LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) => (isActive ? "drawer-link on" : "drawer-link")}
                onClick={() => setOpen(false)}
              >
                {link.label}
              </NavLink>
            ))}

            <button className="drawer-link as-button" type="button" onClick={sair}>
              Sair
            </button>
          </nav>
        </div>
      )}
    </>
  );
}
