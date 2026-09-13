import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, type Activation, type RecipeDetail } from "../api/client";
import { formatDuration, formatIngredient } from "../lib/time";

export default function RecipeViewPage() {
  const { id } = useParams();
  const recipeId = Number(id);

  const [recipe, setRecipe] = useState<RecipeDetail | null>(null);
  const [activation, setActivation] = useState<Activation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const carregar = useCallback(async () => {
    try {
      const detail = await api.getRecipe(recipeId);
      setRecipe(detail);
      if (detail.activationId) {
        const todas = await api.listActivations();
        setActivation(todas.find((a) => a.id === detail.activationId) ?? null);
      } else {
        setActivation(null);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível abrir a receita.");
    }
  }, [recipeId]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  async function ativar() {
    setError(null);
    setBusy(true);
    try {
      const nova = await api.activate(recipeId);
      setActivation(nova);
      setRecipe((r) => (r ? { ...r, activationId: nova.id } : r));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível ativar.");
    } finally {
      setBusy(false);
    }
  }

  async function desativar() {
    if (!activation) return;
    setBusy(true);
    try {
      await api.deactivate(activation.id);
      setActivation(null);
      setRecipe((r) => (r ? { ...r, activationId: null } : r));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível desativar.");
    } finally {
      setBusy(false);
    }
  }

  async function alternar(ingredientId: number, available: boolean) {
    if (!activation) return;
    try {
      setActivation(await api.setAvailability(activation.id, ingredientId, available));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível atualizar.");
    }
  }

  if (error && !recipe) return <div className="page"><div className="error">{error}</div></div>;
  if (!recipe) return <p className="muted centered">A carregar…</p>;

  return (
    <div className="page">
      <div className="topbar">
        <h1>{recipe.name}</h1>
        <Link to="/recipes">Voltar</Link>
      </div>
      <p className="muted small" style={{ marginTop: "-1rem" }}>por {recipe.authorUsername}</p>

      {error && <div className="error">{error}</div>}

      {activation ? (
        <>
          <div className="notice">
            Receita ativa. Desligue o que não tem em casa — vai direto para a lista de compras.
          </div>
          <button onClick={desativar} disabled={busy}>Desativar receita</button>
        </>
      ) : (
        <button className="primary" onClick={ativar} disabled={busy}>
          Vou fazer esta receita
        </button>
      )}

      <h2>Ingredientes</h2>
      {recipe.ingredients.length === 0 && <p className="muted">Sem ingredientes.</p>}

      {activation ? (
        <div className="card">
          {activation.ingredients.map((ing) => (
            <label className="toggle" key={ing.ingredientId}>
              <input
                type="checkbox"
                checked={ing.available}
                onChange={(e) => alternar(ing.ingredientId, e.target.checked)}
              />
              <span className="grow">{formatIngredient(ing.quantity, ing.unit, ing.item)}</span>
              <span className={`small ${ing.available ? "have" : "missing"}`}>
                {ing.available ? "tenho" : "falta"}
              </span>
            </label>
          ))}
        </div>
      ) : (
        <ul className="plain card">
          {recipe.ingredients.map((ing) => (
            <li key={ing.id}>{formatIngredient(ing.quantity, ing.unit, ing.item)}</li>
          ))}
        </ul>
      )}

      <h2>Modo de preparo</h2>
      {recipe.sections.length === 0 && <p className="muted">Sem passos registados.</p>}
      {recipe.sections.map((section) => {
        const duracao = formatDuration(section.totalMinutes, section.partial);
        return (
          <div className="sub-block" key={section.id}>
            <h3>{section.title}</h3>
            {duracao && <p className="muted small" style={{ margin: 0 }}>{duracao}</p>}
            <ol className="steps">
              {section.steps.map((step) => (
                <li key={step.id}>
                  {step.instruction}
                  {step.durationMinutes != null && (
                    <span className="muted small"> ({step.durationMinutes} min)</span>
                  )}
                </li>
              ))}
            </ol>
          </div>
        );
      })}

      {recipe.utensils.length > 0 && (
        <>
          <h2>Utensílios</h2>
          <ul className="plain card">
            {recipe.utensils.map((u, i) => <li key={i}>{u}</li>)}
          </ul>
        </>
      )}

      {recipe.notes && (
        <>
          <h2>Dicas e notas</h2>
          <div className="card" style={{ whiteSpace: "pre-wrap" }}>{recipe.notes}</div>
        </>
      )}
    </div>
  );
}
