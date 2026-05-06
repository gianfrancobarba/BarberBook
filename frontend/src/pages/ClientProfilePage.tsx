import { useState } from "react";
import { useAuthStore } from "@/stores/authStore";
import { useUpdateProfile } from "@/hooks/useAuth";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { User, Mail, Phone, Save, Loader2, Pencil } from "lucide-react";
import { type UpdateUserRequestDto } from "@/types/auth";

const profileSchema = z.object({
  nome: z.string().min(1, "Il nome è obbligatorio").max(100),
  cognome: z.string().min(1, "Il cognome è obbligatorio").max(100),
  email: z.string().email("Email non valida"),
  telefono: z.string().max(20).optional(),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export default function ClientProfilePage() {
  const { user } = useAuthStore();
  const updateProfile = useUpdateProfile();
  const [isEditing, setIsEditing] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      nome: user?.nome ?? "",
      cognome: user?.cognome ?? "",
      email: user?.email ?? "",
      telefono: user?.telefono ?? "",
    },
  });

  const onSubmit = async (data: ProfileFormValues) => {
    const payload: UpdateUserRequestDto = {};
    if (data.nome !== user?.nome) payload.nome = data.nome;
    if (data.cognome !== user?.cognome) payload.cognome = data.cognome;
    if (data.email !== user?.email) payload.email = data.email;
    if (data.telefono !== (user?.telefono ?? "")) payload.telefono = data.telefono || undefined;

    try {
      await updateProfile.mutateAsync(payload);
      toast.success("Profilo aggiornato con successo");
      setIsEditing(false);
    } catch (error: any) {
      const msg = error?.response?.data?.message ?? "Errore durante l'aggiornamento";
      toast.error(msg);
    }
  };

  const handleCancel = () => {
    reset({
      nome: user?.nome ?? "",
      cognome: user?.cognome ?? "",
      email: user?.email ?? "",
      telefono: user?.telefono ?? "",
    });
    setIsEditing(false);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      <header>
        <h1 className="text-3xl font-heading font-bold">Il mio Profilo</h1>
        <p className="text-muted-foreground">Gestisci le tue informazioni personali.</p>
      </header>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <User className="h-5 w-5 text-barber-500" />
                Dati Personali
              </CardTitle>
              <CardDescription>Le informazioni del tuo account</CardDescription>
            </div>
            {!isEditing && (
              <Button variant="outline" size="sm" onClick={() => setIsEditing(true)}>
                <Pencil className="mr-2 h-4 w-4" />
                Modifica
              </Button>
            )}
          </div>
        </CardHeader>

        {isEditing ? (
          <form onSubmit={handleSubmit(onSubmit)}>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="nome">Nome</Label>
                  <Input id="nome" {...register("nome")} />
                  {errors.nome && <p className="text-xs text-destructive">{errors.nome.message}</p>}
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="cognome">Cognome</Label>
                  <Input id="cognome" {...register("cognome")} />
                  {errors.cognome && <p className="text-xs text-destructive">{errors.cognome.message}</p>}
                </div>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="email" className="flex items-center gap-2">
                  <Mail className="h-3.5 w-3.5" /> Email
                </Label>
                <Input id="email" type="email" {...register("email")} />
                {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
              </div>
              <div className="grid gap-2">
                <Label htmlFor="telefono" className="flex items-center gap-2">
                  <Phone className="h-3.5 w-3.5" /> Telefono
                </Label>
                <Input id="telefono" type="tel" placeholder="Opzionale" {...register("telefono")} />
                {errors.telefono && <p className="text-xs text-destructive">{errors.telefono.message}</p>}
              </div>
            </CardContent>
            <CardFooter className="flex gap-3">
              <Button
                type="submit"
                className="bg-barber-500 hover:bg-barber-600"
                disabled={isSubmitting || updateProfile.isPending}
              >
                {(isSubmitting || updateProfile.isPending) ? (
                  <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Salvataggio...</>
                ) : (
                  <><Save className="mr-2 h-4 w-4" /> Salva modifiche</>
                )}
              </Button>
              <Button type="button" variant="outline" onClick={handleCancel}>
                Annulla
              </Button>
            </CardFooter>
          </form>
        ) : (
          <CardContent className="space-y-4">
            <div className="space-y-1">
              <Label className="text-muted-foreground font-normal">Nome e Cognome</Label>
              <p className="font-semibold text-lg">{user?.nome} {user?.cognome}</p>
            </div>
            <div className="space-y-1">
              <Label className="text-muted-foreground font-normal flex items-center gap-2">
                <Mail className="h-3.5 w-3.5" /> Email
              </Label>
              <p className="font-medium">{user?.email}</p>
            </div>
            <div className="space-y-1">
              <Label className="text-muted-foreground font-normal flex items-center gap-2">
                <Phone className="h-3.5 w-3.5" /> Telefono
              </Label>
              <p className="font-medium">{user?.telefono || <span className="text-muted-foreground italic">Non specificato</span>}</p>
            </div>
          </CardContent>
        )}
      </Card>
    </div>
  );
}
