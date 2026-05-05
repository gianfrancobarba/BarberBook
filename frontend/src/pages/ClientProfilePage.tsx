import { useAuthStore } from "@/stores/authStore";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { User, Mail, Phone, Lock, Save, Loader2 } from "lucide-react";

const passwordSchema = z.object({
  currentPassword: z.string().min(1, "La password attuale è obbligatoria"),
  newPassword: z.string().min(8, "La nuova password deve avere almeno 8 caratteri"),
  confirmPassword: z.string()
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Le password non coincidono",
  path: ["confirmPassword"],
});

type PasswordFormValues = z.infer<typeof passwordSchema>;

export default function ClientProfilePage() {
  const { user } = useAuthStore();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
  });

  const onPasswordSubmit = async (_data: PasswordFormValues) => {
    try {
      // Mock API call
      await new Promise(resolve => setTimeout(resolve, 1500));
      toast.success("Password aggiornata con successo");
      reset();
    } catch (error) {
      toast.error("Errore durante l'aggiornamento della password");
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      <header>
        <h1 className="text-3xl font-heading font-bold">Il mio Profilo</h1>
        <p className="text-muted-foreground">Gestisci le tue informazioni personali e la sicurezza.</p>
      </header>

      <div className="grid gap-8 md:grid-cols-2">
        {/* Personal Info */}
        <Card className="h-fit">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5 text-barber-500" />
              Dati Personali
            </CardTitle>
            <CardDescription>Le informazioni del tuo account</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1">
              <Label className="text-muted-foreground font-normal">Nome e Cognome</Label>
              <p className="font-semibold text-lg">{user?.nome} {user?.cognome}</p>
            </div>
            <div className="space-y-1">
              <Label className="text-muted-foreground font-normal flex items-center gap-2">
                <Mail className="h-3 w-3" /> Email
              </Label>
              <p className="font-medium">{user?.email}</p>
            </div>
            <div className="space-y-1">
              <Label className="text-muted-foreground font-normal flex items-center gap-2">
                <Phone className="h-3 w-3" /> Telefono
              </Label>
              <p className="font-medium">{user?.telefono || "Non specificato"}</p>
            </div>
            <div className="pt-4">
              <Button variant="outline" className="w-full" disabled>
                Modifica Dati (Coming Soon)
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Security / Password Change */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Lock className="h-5 w-5 text-barber-500" />
              Sicurezza
            </CardTitle>
            <CardDescription>Aggiorna la tua password</CardDescription>
          </CardHeader>
          <form onSubmit={handleSubmit(onPasswordSubmit)}>
            <CardContent className="space-y-4">
              <div className="grid gap-2">
                <Label htmlFor="currentPassword">Password Attuale</Label>
                <Input id="currentPassword" type="password" {...register("currentPassword")} />
                {errors.currentPassword && <p className="text-xs text-destructive">{errors.currentPassword.message}</p>}
              </div>
              <div className="grid gap-2">
                <Label htmlFor="newPassword">Nuova Password</Label>
                <Input id="newPassword" type="password" {...register("newPassword")} />
                {errors.newPassword && <p className="text-xs text-destructive">{errors.newPassword.message}</p>}
              </div>
              <div className="grid gap-2">
                <Label htmlFor="confirmPassword">Conferma Nuova Password</Label>
                <Input id="confirmPassword" type="password" {...register("confirmPassword")} />
                {errors.confirmPassword && <p className="text-xs text-destructive">{errors.confirmPassword.message}</p>}
              </div>
            </CardContent>
            <CardFooter>
              <Button type="submit" className="w-full bg-barber-500 hover:bg-barber-600" disabled={isSubmitting}>
                {isSubmitting ? (
                  <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Salvataggio...</>
                ) : (
                  <><Save className="mr-2 h-4 w-4" /> Aggiorna Password</>
                )}
              </Button>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
}
