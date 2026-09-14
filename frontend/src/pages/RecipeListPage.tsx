import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, type RecipeSummary } from "../api/client";
import SideMenu from "../components/SideMenu";

export default function RecipeListPage() {
  const [scope, setScope] = useState<"all" | "mine">("all");
  const [recipes, setRecipes] = useState<RecipeSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    api
      .listRecipes(scope)
      .then(setRecipes)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [scope]);

  async function remover(id: number, name: string) {
    if (!confirm(`Apagar "${name}"? Não dá para desfazer.`)) return;
    try {
      await api.deleteRecipe(id);
      setRecipes((current) => current.filter((r) => r.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível apagar.");
    }
  }

  return (
    <div className="page">
      <div className="topbar">
        <div className="row">
          <SideMenu />
          <h1>Receitas</h1>
        </div>
        <Link to="/">Voltar</Link>
      </div>

      <div className="filters">
        <button className={scope === "all" ? "on" : ""} onClick={() => setScope("all")}>
          Todas
        </button>
        <button className={scope === "mine" ? "on" : ""} onClick={() => setScope("mine")}>
          Só as minhas
        </button>
        <button className="primary push-right" onClick={() => navigate("/recipes/new")}>
          + Criar receita
        </button>
      </div>

      {error && <div className="error">{error}</div>}
      {loading && <p className="muted">A carregar…</p>}

      {!loading && recipes.length === 0 && (
        <div className="card">
          <p style={{ marginTop: 0 }}>Ainda não há receitas por aqui.</p>
          <button className="primary" onClick={() => navigate("/recipes/new")}>
            Criar a primeira
          </button>
        </div>
      )}

      {recipes.map((recipe) => (
        <div className="card" key={recipe.id}>
          <div className="row-between">
            <div className="grow">
              <strong>{recipe.name}</strong>
              <div className="muted small">
                por {recipe.authorUsername} · {recipe.ingredientCount} ingredientes
              </div>
            </div>
            {recipe.active && <span className="badge">ativa</span>}
          </div>

          <div className="row wrap" style={{ marginTop: "0.75rem" }}>
            <button onClick={() => navigate(`/recipes/${recipe.id}`)}>Abrir</button>
            {recipe.mine && (
              <>
                <button onClick={() => navigate(`/recipes/${recipe.id}/edit`)}>Editar</button>
                <button className="danger" onClick={() => remover(recipe.id, recipe.name)}>
                  Apagar
                </button>
              </>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
