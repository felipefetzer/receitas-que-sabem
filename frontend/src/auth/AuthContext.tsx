import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, type User } from "../api/client";

type AuthState = {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Ao abrir a app, pergunta ao servidor se o cookie de sessão ainda vale.
  useEffect(() => {
    api
      .me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const value: AuthState = {
    user,
    loading,
    login: async (username, password) => setUser(await api.login(username, password)),
    register: async (username, password) => {
      await api.register(username, password);
      setUser(await api.login(username, password));
    },
    logout: async () => {
      await api.logout();
      setUser(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth tem de estar dentro de AuthProvider");
  return context;
}
