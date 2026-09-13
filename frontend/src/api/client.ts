const API_URL = (import.meta.env.VITE_API_URL ?? "").replace(/\/+$/, "");

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// ---------------------------------------------------------------
// CSRF
// O servidor guarda o token num cookie e espera-o de volta num
// cabeçalho. Como o frontend vive noutro domínio, não consegue ler
// esse cookie — por isso vamos buscar o token a um endpoint.
// O cookie continua a ser enviado automaticamente e é assim que o
// servidor confirma que os dois valores coincidem.
// ---------------------------------------------------------------
let csrfToken: string | null = null;
let csrfHeader = "X-XSRF-TOKEN";

async function loadCsrfToken(): Promise<void> {
  const response = await fetch(`${API_URL}/api/auth/csrf`, { credentials: "include" });
  if (!response.ok) return;
  const data = (await response.json()) as { token: string; headerName: string };
  csrfToken = data.token;
  csrfHeader = data.headerName || "X-XSRF-TOKEN";
}

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
};

async function request<T>(path: string, options: RequestOptions = {}, canRetry = true): Promise<T> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = { "Content-Type": "application/json" };

  if (method !== "GET") {
    if (!csrfToken) await loadCsrfToken();
    if (csrfToken) headers[csrfHeader] = csrfToken;
  }

  const response = await fetch(`${API_URL}${path}`, {
    method,
    credentials: "include",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  // Token expirado (a sessão mudou): pede um novo e tenta uma vez.
  if (response.status === 403 && method !== "GET" && canRetry) {
    csrfToken = null;
    await loadCsrfToken();
    return request<T>(path, options, false);
  }

  if (!response.ok) {
    let detail = `Erro ${response.status}`;
    try {
      const problem = (await response.json()) as { detail?: string };
      if (problem.detail) detail = problem.detail;
    } catch {
      // resposta sem corpo JSON
    }
    throw new ApiError(response.status, detail);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

// ---------------------------------------------------------------
// Tipos
// ---------------------------------------------------------------
export type User = { id: number; username: string };

export type Ingredient = {
  id: number;
  position: number;
  quantity: string | null;
  unit: string | null;
  item: string;
};

export type Step = {
  id: number;
  position: number;
  instruction: string;
  durationMinutes: number | null;
};

export type Section = {
  id: number;
  position: number;
  title: string;
  steps: Step[];
  totalMinutes: number;
  partial: boolean;
};

export type RecipeSummary = {
  id: number;
  name: string;
  authorUsername: string;
  mine: boolean;
  active: boolean;
  ingredientCount: number;
};

export type RecipeDetail = {
  id: number;
  name: string;
  notes: string | null;
  authorUsername: string;
  mine: boolean;
  activationId: number | null;
  ingredients: Ingredient[];
  sections: Section[];
  utensils: string[];
};

export type RecipePayload = {
  name: string;
  notes: string | null;
  ingredients: { quantity: string | null; unit: string | null; item: string }[];
  sections: { title: string; steps: { instruction: string; durationMinutes: number | null }[] }[];
  utensils: string[];
};

export type ActivationIngredient = {
  ingredientId: number;
  position: number;
  quantity: string | null;
  unit: string | null;
  item: string;
  available: boolean;
};

export type Activation = {
  id: number;
  recipeId: number;
  recipeName: string;
  authorUsername: string;
  ingredients: ActivationIngredient[];
};

export type ShoppingItem = {
  activationId: number;
  ingredientId: number;
  recipeName: string;
  quantity: string | null;
  unit: string | null;
  item: string;
};

export type ShoppingGroup = {
  activationId: number;
  recipeName: string;
  items: ShoppingItem[];
};

// ---------------------------------------------------------------
// API
// ---------------------------------------------------------------
export const api = {
  health: () => request<{ status: string; database: string }>("/api/health"),

  me: () => request<User>("/api/auth/me"),
  login: (username: string, password: string) =>
    request<User>("/api/auth/login", { method: "POST", body: { username, password } }),
  register: (username: string, password: string) =>
    request<User>("/api/auth/register", { method: "POST", body: { username, password } }),
  logout: () => request<void>("/api/auth/logout", { method: "POST" }),

  listRecipes: (scope: "all" | "mine") => request<RecipeSummary[]>(`/api/recipes?scope=${scope}`),
  getRecipe: (id: number) => request<RecipeDetail>(`/api/recipes/${id}`),
  createRecipe: (payload: RecipePayload) =>
    request<RecipeDetail>("/api/recipes", { method: "POST", body: payload }),
  updateRecipe: (id: number, payload: RecipePayload) =>
    request<RecipeDetail>(`/api/recipes/${id}`, { method: "PUT", body: payload }),
  deleteRecipe: (id: number) => request<void>(`/api/recipes/${id}`, { method: "DELETE" }),

  listActivations: () => request<Activation[]>("/api/activations"),
  activate: (recipeId: number) =>
    request<Activation>("/api/activations", { method: "POST", body: { recipeId } }),
  deactivate: (activationId: number) =>
    request<void>(`/api/activations/${activationId}`, { method: "DELETE" }),
  setAvailability: (activationId: number, ingredientId: number, available: boolean) =>
    request<Activation>(`/api/activations/${activationId}/ingredients/${ingredientId}`, {
      method: "PATCH",
      body: { available },
    }),

  shoppingList: () => request<ShoppingGroup[]>("/api/shopping-list"),
};
