import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertCircle, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

interface Props {
  children?: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught error:", error, errorInfo);
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: undefined });
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;

      return (
        <div className="flex min-h-[400px] flex-col items-center justify-center p-8 text-center">
          <div className="mb-4 rounded-full bg-red-100 p-4 dark:bg-red-900/20">
            <AlertCircle className="h-10 w-10 text-red-600 dark:text-red-500" />
          </div>
          <h2 className="text-2xl font-bold">Qualcosa è andato storto</h2>
          <p className="mt-2 text-muted-foreground">
            Si è verificato un errore imprevisto. Prova a ricaricare la pagina.
          </p>
          <Button 
            onClick={this.handleReset}
            className="mt-6"
            variant="outline"
          >
            <RefreshCw className="mr-2 h-4 w-4" />
            Ricarica Pagina
          </Button>
          {import.meta.env.DEV && (
            <pre className="mt-8 max-w-full overflow-auto rounded bg-muted p-4 text-left text-xs text-red-500">
              {this.state.error?.message}
            </pre>
          )}
        </div>
      );
    }

    return this.props.children;
  }
}
