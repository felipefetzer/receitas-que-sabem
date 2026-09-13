import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, type RecipePayload } from "../api/client";

type IngredientForm = { quantity: string; unit: string; item: string };
type StepForm = { instruction: string; durationMinutes: string };
type SectionForm = { title: string; steps: StepForm[] };

function mover<T>(lista: T[], de: number, para: number): T[] {
  if (para < 0 || para >= lista.length) return lista;
  const copia = [...lista];
  const [item] = copia.splice(de, 1);
  copia.splice(para, 0, item);
  return copia;
}

export default function RecipeFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [notes, setNotes] = useState("");
  const [ingredients, setIngredients] = useState<IngredientForm[]>([
    { quantity: "", unit: "", item: "" },
  ]);
  const [sections, setSections] = useState<SectionForm[]>([
    { title: "Preparação", steps: [{ instruction: "", durationMinutes: "" }] },
  ]);
  const [utensils, setUtensils] = useState<string[]>([""]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!editing) return;
    api
      .getRecipe(Number(id))
      .then((r) => {
        setName(r.name);
        setNotes(r.notes ?? "");
        setIngredients(
          r.ingredients.length
            ? r.ingredients.map((i) => ({
                quantity: i.quantity ?? "",
                unit: i.unit ?? "",
                item: i.item,
              }))
            : [{ quantity: "", unit: "", item: "" }],
        );
        setSections(
          r.sections.length
            ? r.sections.map((s) => ({
                title: s.title,
                steps: s.steps.length
                  ? s.steps.map((p) => ({
                      instruction: p.instruction,
                      durationMinutes: p.durationMinutes?.toString() ?? "",
                    }))
                  : [{ instruction: "", durationMinutes: "" }],
              }))
            : [{ title: "Preparação", steps: [{ instruction: "", durationMinutes: "" }] }],
        );
        setUtensils(r.utensils.length ? r.utensils : [""]);
      })
      .catch((e: Error) => setError(e.message));
  }, [editing, id]);

  async function guardar(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const payload: RecipePayload = {
      name: name.trim(),
      notes: notes.trim() || null,
      ingredients: ingredients
        .filter((i) => i.item.trim())
        .map((i) => ({
          quantity: i.quantity.trim() || null,
          unit: i.unit.trim() || null,
          item: i.item.trim(),
        })),
      sections: sections
        .filter((s) => s.title.trim())
        .map((s) => ({
          title: s.title.trim(),
          steps: s.steps
            .filter((p) => p.instruction.trim())
            .map((p) => ({
              instruction: p.instruction.trim(),
              durationMinutes: p.durationMinutes.trim() ? Number(p.durationMinutes) : null,
            })),
        })),
      utensils: utensils.map((u) => u.trim()).filter(Boolean),
    };

    if (!payload.name) {
      setError("A receita precisa de um nome.");
      return;
    }

    setBusy(true);
    try {
      const saved = editing
        ? await api.updateRecipe(Number(id), payload)
        : await api.createRecipe(payload);
      navigate(`/recipes/${saved.id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível guardar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="topbar">
        <h1>{editing ? "Editar receita" : "Nova receita"}</h1>
        <Link to="/recipes">Cancelar</Link>
      </div>

      {error && <div className="error">{error}</div>}

      <form onSubmit={guardar}>
        <div className="field">
          <label htmlFor="name">Nome</label>
          <input id="name" type="text" value={name} onChange={(e) => setName(e.target.value)} />
        </div>

        <h2>Ingredientes</h2>
        {ingredients.map((ing, index) => (
          <div className="card" key={index}>
            <div className="row wrap">
              <input
                type="text"
                placeholder="1"
                aria-label="Quantidade"
                style={{ maxWidth: "5rem" }}
                value={ing.quantity}
                onChange={(e) =>
                  setIngredients((l) =>
                    l.map((x, i) => (i === index ? { ...x, quantity: e.target.value } : x)),
                  )
                }
              />
              <input
                type="text"
                placeholder="colher"
                aria-label="Tipo de quantidade"
                style={{ maxWidth: "8rem" }}
                value={ing.unit}
                onChange={(e) =>
                  setIngredients((l) =>
                    l.map((x, i) => (i === index ? { ...x, unit: e.target.value } : x)),
                  )
                }
              />
              <input
                type="text"
                placeholder="açúcar"
                aria-label="Item"
                className="grow"
                value={ing.item}
                onChange={(e) =>
                  setIngredients((l) =>
                    l.map((x, i) => (i === index ? { ...x, item: e.target.value } : x)),
                  )
                }
              />
            </div>
            <div className="row" style={{ marginTop: "0.5rem" }}>
              <button type="button" className="tiny" onClick={() => setIngredients((l) => mover(l, index, index - 1))}>
                ↑
              </button>
              <button type="button" className="tiny" onClick={() => setIngredients((l) => mover(l, index, index + 1))}>
                ↓
              </button>
              <button
                type="button"
                className="tiny danger"
                onClick={() => setIngredients((l) => l.filter((_, i) => i !== index))}
              >
                Remover
              </button>
            </div>
          </div>
        ))}
        <button
          type="button"
          onClick={() => setIngredients((l) => [...l, { quantity: "", unit: "", item: "" }])}
        >
          Adicionar ingrediente
        </button>

        <h2>Modo de preparo</h2>
        {sections.map((section, sIndex) => (
          <div className="card" key={sIndex}>
            <div className="field">
              <label>Parte da preparação</label>
              <input
                type="text"
                placeholder="massa, recheio, molho…"
                value={section.title}
                onChange={(e) =>
                  setSections((l) =>
                    l.map((x, i) => (i === sIndex ? { ...x, title: e.target.value } : x)),
                  )
                }
              />
            </div>

            {section.steps.map((step, pIndex) => (
              <div className="row wrap" key={pIndex} style={{ marginBottom: "0.5rem" }}>
                <input
                  type="number"
                  min={0}
                  placeholder="min"
                  aria-label="Tempo em minutos"
                  style={{ maxWidth: "5.5rem" }}
                  value={step.durationMinutes}
                  onChange={(e) =>
                    setSections((l) =>
                      l.map((s, i) =>
                        i === sIndex
                          ? {
                              ...s,
                              steps: s.steps.map((p, j) =>
                                j === pIndex ? { ...p, durationMinutes: e.target.value } : p,
                              ),
                            }
                          : s,
                      ),
                    )
                  }
                />
                <input
                  type="text"
                  placeholder="pique a cebola"
                  aria-label="Instrução"
                  className="grow"
                  value={step.instruction}
                  onChange={(e) =>
                    setSections((l) =>
                      l.map((s, i) =>
                        i === sIndex
                          ? {
                              ...s,
                              steps: s.steps.map((p, j) =>
                                j === pIndex ? { ...p, instruction: e.target.value } : p,
                              ),
                            }
                          : s,
                      ),
                    )
                  }
                />
                <button
                  type="button"
                  className="tiny"
                  onClick={() =>
                    setSections((l) =>
                      l.map((s, i) =>
                        i === sIndex ? { ...s, steps: mover(s.steps, pIndex, pIndex - 1) } : s,
                      ),
                    )
                  }
                >
                  ↑
                </button>
                <button
                  type="button"
                  className="tiny"
                  onClick={() =>
                    setSections((l) =>
                      l.map((s, i) =>
                        i === sIndex ? { ...s, steps: mover(s.steps, pIndex, pIndex + 1) } : s,
                      ),
                    )
                  }
                >
                  ↓
                </button>
                <button
                  type="button"
                  className="tiny danger"
                  onClick={() =>
                    setSections((l) =>
                      l.map((s, i) =>
                        i === sIndex
                          ? { ...s, steps: s.steps.filter((_, j) => j !== pIndex) }
                          : s,
                      ),
                    )
                  }
                >
                  ×
                </button>
              </div>
            ))}

            <div className="row wrap">
              <button
                type="button"
                className="tiny"
                onClick={() =>
                  setSections((l) =>
                    l.map((s, i) =>
                      i === sIndex
                        ? { ...s, steps: [...s.steps, { instruction: "", durationMinutes: "" }] }
                        : s,
                    ),
                  )
                }
              >
                Adicionar passo
              </button>
              <button type="button" className="tiny" onClick={() => setSections((l) => mover(l, sIndex, sIndex - 1))}>
                ↑ parte
              </button>
              <button type="button" className="tiny" onClick={() => setSections((l) => mover(l, sIndex, sIndex + 1))}>
                ↓ parte
              </button>
              <button
                type="button"
                className="tiny danger"
                onClick={() => setSections((l) => l.filter((_, i) => i !== sIndex))}
              >
                Remover parte
              </button>
            </div>
          </div>
        ))}
        <button
          type="button"
          onClick={() =>
            setSections((l) => [...l, { title: "", steps: [{ instruction: "", durationMinutes: "" }] }])
          }
        >
          Adicionar parte
        </button>

        <h2>Utensílios</h2>
        {utensils.map((u, index) => (
          <div className="row" key={index} style={{ marginBottom: "0.5rem" }}>
            <input
              type="text"
              placeholder="batedeira"
              aria-label="Utensílio"
              className="grow"
              value={u}
              onChange={(e) =>
                setUtensils((l) => l.map((x, i) => (i === index ? e.target.value : x)))
              }
            />
            <button
              type="button"
              className="tiny danger"
              onClick={() => setUtensils((l) => l.filter((_, i) => i !== index))}
            >
              ×
            </button>
          </div>
        ))}
        <button type="button" onClick={() => setUtensils((l) => [...l, ""])}>
          Adicionar utensílio
        </button>

        <h2>Dicas e notas</h2>
        <div className="field">
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>

        <button className="primary" type="submit" disabled={busy}>
          {busy ? "A guardar…" : "Guardar receita"}
        </button>
      </form>
    </div>
  );
}
