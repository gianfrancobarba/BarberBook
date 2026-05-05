import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useNavigate, Link } from "react-router-dom";
import { useRegister } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";
import { Scissors, Loader2 } from "lucide-react";

const registerSchema = z.object({
  nome: z.string().min(2, "Il nome deve avere almeno 2 caratteri"),
  cognome: z.string().min(2, "Il cognome deve avere almeno 2 caratteri"),
  email: z.string().email("Inserisci un indirizzo email valido"),
  telefono: z.string().regex(/^\+?[0-9\s-]{8,20}$/, "Inserisci un numero di telefono valido"),
  password: z.string().min(8, "La password deve avere almeno 8 caratteri"),
  confirmPassword: z.string()
}).refine((data) => data.password === data.confirmPassword, {
  message: "Le password non coincidono",
  path: ["confirmPassword"],
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const navigate = useNavigate();
  const registerMutation = useRegister();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormValues) => {
    try {
      await registerMutation.mutateAsync({
        nome: data.nome,
        cognome: data.cognome,
        email: data.email,
        telefono: data.telefono,
        password: data.password,
      });

      toast.success("Account creato con successo! Benvenuto in BarberBook.");
      navigate("/my", { replace: true });
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Errore durante la registrazione");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <div className="flex justify-center mb-4">
            <div className="rounded-xl bg-barber-500 p-2 text-white">
              <Scissors className="h-6 w-6" />
            </div>
          </div>
          <CardTitle className="text-2xl font-heading font-bold tracking-tight">Crea un account</CardTitle>
          <CardDescription>
            Unisciti a BarberBook per gestire le tue prenotazioni
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="nome">Nome</Label>
                <Input id="nome" placeholder="Mario" {...register("nome")} />
                {errors.nome && <p className="text-xs text-destructive">{errors.nome.message}</p>}
              </div>
              <div className="grid gap-2">
                <Label htmlFor="cognome">Cognome</Label>
                <Input id="cognome" placeholder="Rossi" {...register("cognome")} />
                {errors.cognome && <p className="text-xs text-destructive">{errors.cognome.message}</p>}
              </div>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" placeholder="mario.rossi@esempio.it" {...register("email")} />
              {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="telefono">Telefono</Label>
              <Input id="telefono" type="tel" placeholder="333 1234567" {...register("telefono")} />
              {errors.telefono && <p className="text-xs text-destructive">{errors.telefono.message}</p>}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="password">Password</Label>
              <Input id="password" type="password" {...register("password")} />
              {errors.password && <p className="text-xs text-destructive">{errors.password.message}</p>}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="confirmPassword">Conferma Password</Label>
              <Input id="confirmPassword" type="password" {...register("confirmPassword")} />
              {errors.confirmPassword && <p className="text-xs text-destructive">{errors.confirmPassword.message}</p>}
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
                  Creazione account...
                </>
              ) : (
                "Registrati"
              )}
            </Button>
            <div className="text-center text-sm text-muted-foreground">
              Hai già un account?{" "}
              <Link to="/login" className="text-barber-500 hover:underline">
                Accedi
              </Link>
            </div>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
