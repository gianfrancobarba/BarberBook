import { Outlet, Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/authStore";
import { Scissors } from "lucide-react";

export function PublicLayout() {
  const { isAuthenticated, isBarber } = useAuthStore();

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto flex h-16 items-center justify-between px-4">
          <Link to="/" className="flex items-center space-x-2">
            <div className="rounded-lg bg-barber-500 p-1.5 text-white">
              <Scissors className="h-5 w-5" />
            </div>
            <span className="font-heading text-xl font-bold tracking-tight">
              Barber<span className="text-barber-500">Book</span>
            </span>
          </Link>

          <nav className="flex items-center space-x-4">
            <Link to="/book">
              <Button variant="ghost" className="hidden sm:inline-flex">Prenota</Button>
            </Link>
            
            {isAuthenticated() ? (
              <Link to={isBarber() ? "/barber" : "/my"}>
                <Button className="bg-barber-500 hover:bg-barber-600">Area Personale</Button>
              </Link>
            ) : (
              <Link to="/login">
                <Button variant="outline">Accedi</Button>
              </Link>
            )}
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t py-8">
        <div className="container mx-auto px-4 text-center text-sm text-muted-foreground">
          © {new Date().getFullYear()} BarberBook — Hair Man Tony. Tutti i diritti riservati.
        </div>
      </footer>
    </div>
  );
}
