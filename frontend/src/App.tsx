import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import AboutPage from "./pages/AboutPage";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RecipeFormPage from "./pages/RecipeFormPage";
import RecipeListPage from "./pages/RecipeListPage";
import RecipeViewPage from "./pages/RecipeViewPage";
import ShoppingListPage from "./pages/ShoppingListPage";
import type { ReactNode } from "react";

function Protected({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <p className="muted centered">A carregar…</p>;
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<Protected><HomePage /></Protected>} />
      <Route path="/recipes" element={<Protected><RecipeListPage /></Protected>} />
      <Route path="/recipes/new" element={<Protected><RecipeFormPage /></Protected>} />
      <Route path="/recipes/:id" element={<Protected><RecipeViewPage /></Protected>} />
      <Route path="/recipes/:id/edit" element={<Protected><RecipeFormPage /></Protected>} />
      <Route path="/shopping-list" element={<Protected><ShoppingListPage /></Protected>} />
      <Route path="/about" element={<Protected><AboutPage /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
