import { useEffect, useState } from "react";
import { api, type Health } from "./api/client";

type State =
  | { kind: "loading" }
  | { kind: "ok"; health: Health }
  | { kind: "error"; message: string };

export default function App() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    api
      .health()
      .then((health) => setState({ kind: "ok", health }))
      .catch((error: Error) => setState({ kind: "error", message: error.message }));
  }, []);

  return (
    <main style={styles.main}>
      <h1 style={styles.title}>Receitas que Sabem</h1>
      <p style={styles.subtitle}>Fase 0 — verificação de ligação</p>

      {state.kind === "loading" && (
        <p style={styles.line}>A contactar a API…</p>
      )}

      {state.kind === "error" && (
        <div style={styles.card}>
          <p style={styles.bad}>A API não respondeu</p>
          <p style={styles.line}>{state.message}</p>
          <p style={styles.hint}>
            O backend está a correr? Se estiver em produção, pode estar a acordar —
            tente outra vez daqui a um minuto.
          </p>
        </div>
      )}

      {state.kind === "ok" && (
        <div style={styles.card}>
          <p style={styles.good}>API a responder</p>
          <dl style={styles.list}>
            <dt style={styles.key}>Base de dados</dt>
            <dd style={styles.value}>{state.health.database}</dd>
            <dt style={styles.key}>Registo lido</dt>
            <dd style={styles.value}>{state.health.app ?? "—"}</dd>
            <dt style={styles.key}>Hora do servidor</dt>
            <dd style={styles.value}>{state.health.timestamp}</dd>
          </dl>
        </div>
      )}
    </main>
  );
}

const styles: Record<string, React.CSSProperties> = {
  main: {
    fontFamily: "system-ui, sans-serif",
    maxWidth: "32rem",
    margin: "0 auto",
    padding: "3rem 1.25rem",
  },
  title: { fontSize: "1.75rem", margin: 0 },
  subtitle: { color: "#666", marginTop: "0.25rem" },
  card: {
    border: "1px solid #ddd",
    borderRadius: "0.5rem",
    padding: "1rem 1.25rem",
    marginTop: "1.5rem",
  },
  good: { fontWeight: 600, color: "#1a7f37", margin: 0 },
  bad: { fontWeight: 600, color: "#b3261e", margin: 0 },
  line: { margin: "0.5rem 0 0" },
  hint: { color: "#666", fontSize: "0.875rem", marginTop: "0.75rem" },
  list: { display: "grid", gridTemplateColumns: "auto 1fr", gap: "0.35rem 1rem", marginTop: "0.75rem" },
  key: { color: "#666" },
  value: { margin: 0, wordBreak: "break-all" },
};
