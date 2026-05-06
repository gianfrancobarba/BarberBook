import { useRouteError, isRouteErrorResponse, Link } from "react-router-dom";
import { AlertCircle, Home, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function RouteErrorPage() {
  const error = useRouteError();

  let title = "Qualcosa è andato storto";
  let description = "Si è verificato un errore imprevisto. Prova a ricaricare la pagina.";

  if (isRouteErrorResponse(error)) {
    if (error.status === 404) {
      title = "Pagina non trovata";
      description = "La pagina che stai cercando non esiste o è stata spostata.";
    } else if (error.status === 403) {
      title = "Accesso negato";
      description = "Non hai i permessi per accedere a questa pagina.";
    } else if (error.status >= 500) {
      title = "Errore del server";
      description = "Si è verificato un errore sul server. Riprova più tardi.";
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-8 text-center bg-background">
      <div className="mb-6 rounded-full bg-red-100 p-5 dark:bg-red-900/20">
        <AlertCircle className="h-12 w-12 text-red-600 dark:text-red-500" />
      </div>
      <h1 className="text-3xl font-bold font-heading">{title}</h1>
      <p className="mt-3 max-w-md text-muted-foreground">{description}</p>
      <div className="mt-8 flex flex-col sm:flex-row gap-3">
        <Link to="/">
          <Button>
            <Home className="mr-2 h-4 w-4" />
            Torna alla Home
          </Button>
        </Link>
        <Button variant="outline" onClick={() => window.location.reload()}>
          <RefreshCw className="mr-2 h-4 w-4" />
          Ricarica Pagina
        </Button>
      </div>
      {import.meta.env.DEV && error instanceof Error && (
        <pre className="mt-10 max-w-2xl overflow-auto rounded bg-muted p-4 text-left text-xs text-red-500">
          {error.message}
          {"\n"}
          {error.stack}
        </pre>
      )}
    </div>
  );
}
