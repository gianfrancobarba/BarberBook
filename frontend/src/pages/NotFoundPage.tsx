import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/authStore";
import { Scissors } from "lucide-react";

export default function NotFoundPage() {
  const { isAuthenticated, isBarber } = useAuthStore();

  const homeLink = isAuthenticated()
    ? isBarber() ? "/barber" : "/my"
    : "/";

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4 text-center">
      <Scissors className="h-16 w-16 text-barber-300 mb-6" />
      <h1 className="text-7xl font-bold font-heading text-barber-500 mb-2">404</h1>
      <h2 className="text-2xl font-semibold mb-3">Pagina non trovata</h2>
      <p className="text-muted-foreground mb-8 max-w-md">
        La pagina che stai cercando non esiste o è stata spostata.
      </p>
      <Link to={homeLink}>
        <Button className="bg-barber-500 hover:bg-barber-600">
          Torna alla home
        </Button>
      </Link>
    </div>
  );
}
