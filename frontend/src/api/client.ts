const API_URL = import.meta.env.VITE_API_URL;

if (!API_URL) {
  throw new Error("VITE_API_URL não está definida. Copie o .env.example para .env.local.");
}

export type Health = {
  status: string;
  database: string;
  app: string | null;
  timestamp: string;
};

/**
 * Todas as chamadas à API passam por aqui.
 * credentials: "include" faz o browser enviar o cookie de sessão
 * mesmo para outro domínio — vai ser preciso na Fase 1.
 */
async function request<T>(path: string): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json" },
  });

  if (!response.ok) {
    throw new Error(`A API respondeu ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export const api = {
  health: () => request<Health>("/api/health"),
};
