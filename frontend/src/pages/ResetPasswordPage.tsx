import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { useResetPassword } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";
import { Scissors, Loader2 } from "lucide-react";

const resetSchema = z.object({
  password: z.string().min(8, "La password deve avere almeno 8 caratteri"),
  confirmPassword: z.string()
}).refine((data) => data.password === data.confirmPassword, {
  message: "Le password non coincidono",
  path: ["confirmPassword"],
});

type ResetFormValues = z.infer<typeof resetSchema>;

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const resetMutation = useResetPassword();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetFormValues>({
    resolver: zodResolver(resetSchema),
  });

  const onSubmit = async (data: ResetFormValues) => {
    if (!token) {
      toast.error("Token di reset mancante o non valido");
      return;
    }

    try {
      await resetMutation.mutateAsync({
        token,
        newPassword: data.password,
      });

      toast.success("Password aggiornata con successo! Ora puoi accedere.");
      navigate("/login", { replace: true });
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Errore durante il reset della password");
    }
  };

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md border-destructive">
          <CardHeader>
            <CardTitle className="text-destructive text-center">Link non valido</CardTitle>
            <CardDescription className="text-center">
              Il link di reset della password è mancante o scaduto.
            </CardDescription>
          </CardHeader>
          <CardFooter>
            <Link to="/forgot-password" title="Richiedi nuovo link" className="w-full">
              <Button variant="outline" className="w-full">Richiedi nuovo link</Button>
            </Link>
          </CardFooter>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <div className="flex justify-center mb-4">
            <div className="rounded-xl bg-barber-500 p-2 text-white">
              <Scissors className="h-6 w-6" />
            </div>
          </div>
          <CardTitle className="text-2xl font-heading font-bold tracking-tight">Nuova Password</CardTitle>
          <CardDescription>
            Scegli una nuova password sicura per il tuo account
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="grid gap-4">
            <div className="grid gap-2">
              <Label htmlFor="password">Nuova Password</Label>
              <Input id="password" type="password" {...register("password")} />
              {errors.password && <p className="text-xs text-destructive">{errors.password.message}</p>}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="confirmPassword">Conferma Password</Label>
              <Input id="confirmPassword" type="password" {...register("confirmPassword")} />
              {errors.confirmPassword && <p className="text-xs text-destructive">{errors.confirmPassword.message}</p>}
            </div>
          </CardContent>
          <CardFooter>
            <Button 
              type="submit" 
              className="w-full bg-barber-500 hover:bg-barber-600" 
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Aggiornamento...
                </>
              ) : (
                "Reimposta password"
              )}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
