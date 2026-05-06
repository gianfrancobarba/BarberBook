import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/authStore";
import { Logo } from "@/components/common/Logo";
import { Home } from "lucide-react";

export default function NotFoundPage() {
  const { isAuthenticated, isBarber } = useAuthStore();

  const homeLink = isAuthenticated()
    ? isBarber() ? "/barber" : "/my"
    : "/";

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4 text-center">
      <Logo size="h-24" variant="auto" className="mb-8 opacity-30" />
      <h1 className="text-8xl font-black font-heading text-barber-500 mb-2 leading-none">404</h1>
      <h2 className="text-2xl font-semibold mb-3">Pagina non trovata</h2>
      <p className="text-muted-foreground mb-8 max-w-sm">
        La pagina che cerchi non esiste o è stata spostata.
      </p>
      <Link to={homeLink}>
        <Button className="bg-barber-500 hover:bg-barber-600 h-11 px-6">
          <Home className="mr-2 h-4 w-4" />
          Torna alla home
        </Button>
      </Link>
    </div>
  );
}
