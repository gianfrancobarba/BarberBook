import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";
import { type UserRole } from "@/types/auth";

interface ProtectedRouteProps {
  requiredRole?: UserRole;
}

export function ProtectedRoute({ requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuthStore();
  const location = useLocation();

  if (!isAuthenticated()) {
    // Reindirizza al login salvando la posizione attuale per il redirect post-login
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredRole && user?.ruolo !== requiredRole) {
    // Se il ruolo non coincide, reindirizza alla home appropriata
    const redirectPath = user?.ruolo === "BARBER" ? "/barber" : "/";
    return <Navigate to={redirectPath} replace />;
  }

  return <Outlet />;
}
