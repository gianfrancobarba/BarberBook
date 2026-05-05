import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Link } from "react-router-dom";
import { useForgotPassword } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";
import { Scissors, Loader2, ArrowLeft } from "lucide-react";
import { useState } from "react";

const forgotSchema = z.object({
  email: z.string().email("Inserisci un indirizzo email valido"),
});

type ForgotFormValues = z.infer<typeof forgotSchema>;

export default function ForgotPasswordPage() {
  const [isSent, setIsSent] = useState(false);
  const forgotMutation = useForgotPassword();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotFormValues>({
    resolver: zodResolver(forgotSchema),
  });

  const onSubmit = async (data: ForgotFormValues) => {
    try {
      await forgotMutation.mutateAsync(data.email);
      setIsSent(true);
      toast.success("Email inviata con successo!");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Errore durante l'invio");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <div className="flex justify-center mb-4">
            <div className="rounded-xl bg-barber-500 p-2 text-white">
              <Scissors className="h-6 w-6" />
            </div>
          </div>
          <CardTitle className="text-2xl font-heading font-bold tracking-tight">Recupero Password</CardTitle>
          <CardDescription>
            {isSent 
              ? "Controlla la tua email per le istruzioni" 
              : "Inserisci la tua email per ricevere un link di reset"}
          </CardDescription>
        </CardHeader>
        
        {!isSent ? (
          <form onSubmit={handleSubmit(onSubmit)}>
            <CardContent className="grid gap-4">
              <div className="grid gap-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="nome@esempio.it"
                  {...register("email")}
                />
                {errors.email && (
                  <p className="text-xs text-destructive">{errors.email.message}</p>
                )}
              </div>
            </CardContent>
            <CardFooter className="flex flex-col gap-4">
              <Button 
                type="submit" 
                className="w-full bg-barber-500 hover:bg-barber-600" 
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Invio in corso...
                  </>
                ) : (
                  "Invia link di reset"
                )}
              </Button>
              <Link 
                to="/login" 
                className="flex items-center justify-center text-sm text-muted-foreground hover:text-foreground"
              >
                <ArrowLeft className="mr-2 h-4 w-4" />
                Torna al login
              </Link>
            </CardFooter>
          </form>
        ) : (
          <CardContent className="text-center pb-8">
            <p className="text-sm text-muted-foreground mb-6">
              Ti abbiamo inviato un'email all'indirizzo fornito. Se non la ricevi entro pochi minuti, controlla la cartella spam.
            </p>
            <Link to="/login">
              <Button variant="outline" className="w-full">
                Torna al login
              </Button>
            </Link>
          </CardContent>
        )}
      </Card>
    </div>
  );
}
