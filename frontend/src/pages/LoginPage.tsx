import { useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const { user, login, register } = useAuth();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (mode === "login") {
        await login(username.trim(), password);
      } else {
        await register(username.trim(), password);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível continuar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <h1>Receitas que Sabem</h1>
      <p className="muted">
        {mode === "login" ? "Entre para ver e guardar receitas." : "Escolha um nome e uma password."}
      </p>

      {error && <div className="error">{error}</div>}

      <form onSubmit={submit}>
        <div className="field">
          <label htmlFor="username">Utilizador</label>
          <input
            id="username"
            type="text"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={6}
          />
        </div>

        <button className="primary" type="submit" disabled={busy}>
          {busy ? "A processar…" : mode === "login" ? "Entrar" : "Criar conta"}
        </button>
      </form>

      <p className="small" style={{ marginTop: "1.5rem" }}>
        {mode === "login" ? "Ainda não tem conta? " : "Já tem conta? "}
        <button
          className="tiny"
          type="button"
          onClick={() => {
            setMode(mode === "login" ? "register" : "login");
            setUsername("");
            setPassword("");
            setError(null);
          }}
        >
          {mode === "login" ? "Criar conta" : "Entrar"}
        </button>
      </p>

      <p className="muted small" style={{ marginTop: "2rem" }}>
        Se a primeira tentativa demorar, o servidor pode estar a acordar. Tente outra vez.
      </p>
    </div>
  );
}
