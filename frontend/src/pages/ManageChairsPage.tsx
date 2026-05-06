import { useState } from "react";
import { useChairs, useCreateChair, useUpdateChair, useDeleteChair } from "@/hooks/useChairs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { 
  Dialog, 
  DialogContent, 
  DialogDescription, 
  DialogFooter, 
  DialogHeader, 
  DialogTitle 
} from "@/components/ui/dialog";
import { Spinner } from "@/components/common/Spinner";
import { EmptyState } from "@/components/common/EmptyState";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { toast } from "sonner";
import { 
  Plus, 
  Pencil, 
  Trash2, 
  Armchair 
} from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { type ChairResponseDto } from "@/types/chair";

const chairSchema = z.object({
  nome: z.string().min(2, "Il nome deve avere almeno 2 caratteri"),
  attiva: z.boolean().default(true),
});

type ChairFormValues = z.infer<typeof chairSchema>;

export default function ManageChairsPage() {
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingChair, setEditingChair] = useState<ChairResponseDto | null>(null);
  const [chairToDelete, setChairToDelete] = useState<number | null>(null);

  const { data: chairs, isLoading } = useChairs();
  const createMutation = useCreateChair();
  const updateMutation = useUpdateChair();
  const deleteMutation = useDeleteChair();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ChairFormValues>({
    resolver: zodResolver(chairSchema) as any,
    defaultValues: { attiva: true },
  });

  const onSubmit = async (data: ChairFormValues) => {
    try {
      if (editingChair) {
        await updateMutation.mutateAsync({ id: editingChair.id, data });
        toast.success("Poltrona aggiornata");
      } else {
        await createMutation.mutateAsync(data);
        toast.success("Poltrona creata");
      }
      handleClose();
    } catch (error) {
      toast.error("Errore durante il salvataggio");
    }
  };

  const handleEdit = (chair: ChairResponseDto) => {
    setEditingChair(chair);
    reset({
      nome: chair.nome,
      attiva: chair.attiva,
    });
    setIsAddOpen(true);
  };

  const handleClose = () => {
    setIsAddOpen(false);
    setEditingChair(null);
    reset();
  };

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-heading font-bold">Gestione Poltrone</h1>
          <p className="text-muted-foreground">Configura le postazioni di lavoro del tuo salone.</p>
        </div>
        <Button className="bg-barber-500 hover:bg-barber-600" onClick={() => setIsAddOpen(true)}>
          <Plus className="mr-2 h-4 w-4" /> Nuova Poltrona
        </Button>
      </header>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {isLoading ? (
          <div className="col-span-full flex justify-center py-20"><Spinner size={40} /></div>
        ) : !chairs || chairs.length === 0 ? (
          <EmptyState 
            title="Nessuna poltrona" 
            description="Le poltrone rappresentano le postazioni dove i clienti vengono serviti." 
            icon={Armchair}
            className="col-span-full"
          />
        ) : (
          chairs.map((chair) => (
            <Card key={chair.id} className={!chair.attiva ? "opacity-60 bg-muted/30" : ""}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xl font-bold">{chair.nome}</CardTitle>
                <div className="rounded-full bg-barber-100 p-2 text-barber-600 dark:bg-barber-900/30 dark:text-barber-500">
                  <Armchair className="h-5 w-5" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-2 mt-2">
                  <div className={`h-2 w-2 rounded-full ${chair.attiva ? 'bg-green-500' : 'bg-zinc-400'}`} />
                  <span className="text-xs font-medium text-muted-foreground">
                    {chair.attiva ? 'Disponibile per prenotazioni' : 'Non operativa'}
                  </span>
                </div>
              </CardContent>
              <div className="flex items-center justify-end p-4 gap-2 border-t mt-4">
                <Button variant="ghost" size="sm" onClick={() => handleEdit(chair)}>
                  <Pencil className="mr-2 h-4 w-4" /> Modifica
                </Button>
                <Button variant="ghost" size="sm" className="text-destructive" onClick={() => setChairToDelete(chair.id)}>
                  <Trash2 className="mr-2 h-4 w-4" /> Elimina
                </Button>
              </div>
            </Card>
          ))
        )}
      </div>

      <Dialog open={isAddOpen} onOpenChange={(open) => !open && handleClose()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingChair ? 'Modifica Poltrona' : 'Aggiungi Poltrona'}</DialogTitle>
            <DialogDescription>Assegna un nome identificativo alla postazione.</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 pt-4">
            <div className="grid gap-2">
              <Label htmlFor="nome">Nome Poltrona</Label>
              <Input id="nome" placeholder="es. Postazione 1" {...register("nome")} />
              {errors.nome && <p className="text-xs text-destructive">{errors.nome.message}</p>}
            </div>
            <div className="flex items-center justify-between space-x-2 rounded-lg border p-4">
              <div className="space-y-0.5">
                <Label htmlFor="attiva">Attiva</Label>
                <p className="text-xs text-muted-foreground">Rende la poltrona selezionabile per le prenotazioni.</p>
              </div>
              <Switch 
                id="attiva" 
                checked={watch("attiva")} 
                onCheckedChange={(checked) => setValue("attiva", checked)} 
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="ghost" onClick={handleClose}>Annulla</Button>
              <Button type="submit" className="bg-barber-500" disabled={isSubmitting}>
                {editingChair ? 'Salva' : 'Crea'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog 
        open={chairToDelete !== null}
        onOpenChange={(open) => !open && setChairToDelete(null)}
        title="Elimina Poltrona"
        description="Sei sicuro? Questa azione rimuoverà permanentemente la postazione."
        onConfirm={async () => {
          if (chairToDelete) {
            try {
              await deleteMutation.mutateAsync(chairToDelete);
              toast.success("Poltrona eliminata");
              setChairToDelete(null);
            } catch (error) {
              toast.error("Errore durante l'eliminazione");
            }
          }
        }}
        variant="destructive"
      />
    </div>
  );
}
