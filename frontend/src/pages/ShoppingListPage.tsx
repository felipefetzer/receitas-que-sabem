import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, type ShoppingGroup } from "../api/client";
import { formatIngredient } from "../lib/time";
import SideMenu from "../components/SideMenu";

export default function ShoppingListPage() {
  const [groups, setGroups] = useState<ShoppingGroup[]>([]);
  const [riscados, setRiscados] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [copiado, setCopiado] = useState(false);

  useEffect(() => {
    api
      .shoppingList()
      .then(setGroups)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  // Riscar um item marca-o como "tenho" na receita (R19).
  // Fica visível riscado até sair da página, para não desaparecer
  // debaixo do dedo enquanto se anda pelo supermercado.
  async function riscar(activationId: number, ingredientId: number) {
    const chave = `${activationId}:${ingredientId}`;
    setRiscados((atual) => new Set(atual).add(chave));
    try {
      await api.setAvailability(activationId, ingredientId, true);
    } catch (e) {
      setRiscados((atual) => {
        const novo = new Set(atual);
        novo.delete(chave);
        return novo;
      });
      setError(e instanceof Error ? e.message : "Não foi possível atualizar.");
    }
  }

  function copiar() {
    const linhas: string[] = [];
    for (const grupo of groups) {
      linhas.push(grupo.recipeName);
      for (const item of grupo.items) {
        linhas.push(`- ${formatIngredient(item.quantity, item.unit, item.item)}`);
      }
      linhas.push("");
    }
    navigator.clipboard.writeText(linhas.join("\n").trim()).then(() => {
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    });
  }

  const vazia = groups.every((g) => g.items.length === 0);

  return (
    <div className="page">
      <div className="topbar">
        <div className="row">
          <SideMenu />
          <h1>Lista de compras</h1>
        </div>
        <Link to="/">Voltar</Link>
      </div>

      {error && <div className="error">{error}</div>}
      {loading && <p className="muted">A carregar…</p>}

      {!loading && vazia && (
        <div className="card">
          <p style={{ marginTop: 0 }}>Nada em falta.</p>
          <p className="muted small" style={{ marginBottom: 0 }}>
            Ative uma receita e marque o que não tem em casa.
          </p>
        </div>
      )}

      {!vazia && (
        <button onClick={copiar} style={{ marginBottom: "1rem" }}>
          {copiado ? "Copiado" : "Copiar lista"}
        </button>
      )}

      {groups.map((grupo) => (
        <div className="card" key={grupo.activationId}>
          <h3 style={{ marginTop: 0 }}>{grupo.recipeName}</h3>
          {grupo.items.map((item) => {
            const chave = `${item.activationId}:${item.ingredientId}`;
            const riscado = riscados.has(chave);
            return (
              <label className="toggle" key={chave}>
                <input
                  type="checkbox"
                  checked={riscado}
                  disabled={riscado}
                  onChange={() => riscar(item.activationId, item.ingredientId)}
                />
                <span className={riscado ? "struck grow" : "grow"}>
                  {formatIngredient(item.quantity, item.unit, item.item)}
                </span>
              </label>
            );
          })}
        </div>
      ))}
    </div>
  );
}
