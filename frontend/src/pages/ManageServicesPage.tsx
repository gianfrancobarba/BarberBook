import { useState } from "react";
import { useServices, useCreateService, useUpdateService, useDeleteService } from "@/hooks/useServices";
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from "@/components/ui/table";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
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
  Scissors, 
  Clock, 
  Euro 
} from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { type ServiceResponseDto } from "@/types/service";

const serviceSchema = z.object({
  nome: z.string().min(2, "Il nome deve avere almeno 2 caratteri"),
  descrizione: z.string().min(5, "La descrizione deve avere almeno 5 caratteri"),
  durata: z.number().min(5, "La durata minima è 5 minuti"),
  prezzo: z.number().min(0, "Il prezzo non può essere negativo"),
  attivo: z.boolean(),
});

type ServiceFormValues = z.infer<typeof serviceSchema>;

export default function ManageServicesPage() {
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingService, setEditingService] = useState<ServiceResponseDto | null>(null);
  const [serviceToDelete, setServiceToDelete] = useState<number | null>(null);

  const { data: services, isLoading } = useServices();
  const createMutation = useCreateService();
  const updateMutation = useUpdateService();
  const deleteMutation = useDeleteService();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ServiceFormValues>({
    resolver: zodResolver(serviceSchema) as any,
    defaultValues: { 
      nome: "",
      descrizione: "",
      durata: 30,
      prezzo: 0,
      attivo: true 
    },
  });

  const onSubmit = async (data: ServiceFormValues) => {
    try {
      if (editingService) {
        await updateMutation.mutateAsync({ id: editingService.id, data });
        toast.success("Servizio aggiornato con successo");
      } else {
        await createMutation.mutateAsync(data);
        toast.success("Servizio creato con successo");
      }
      handleClose();
    } catch (error) {
      toast.error("Errore durante il salvataggio");
    }
  };

  const handleEdit = (service: ServiceResponseDto) => {
    setEditingService(service);
    reset({
      nome: service.nome,
      descrizione: service.descrizione,
      durata: service.durata,
      prezzo: service.prezzo,
      attivo: service.attivo,
    });
    setIsAddOpen(true);
  };

  const handleDelete = async () => {
    if (serviceToDelete) {
      try {
        await deleteMutation.mutateAsync(serviceToDelete);
        toast.success("Servizio eliminato");
        setServiceToDelete(null);
      } catch (error) {
        toast.error("Errore durante l'eliminazione");
      }
    }
  };

  const handleClose = () => {
    setIsAddOpen(false);
    setEditingService(null);
    reset();
  };

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-heading font-bold">Gestione Servizi</h1>
          <p className="text-muted-foreground">Aggiungi o modifica i servizi offerti dal salone.</p>
        </div>
        <Button className="bg-barber-500 hover:bg-barber-600" onClick={() => setIsAddOpen(true)}>
          <Plus className="mr-2 h-4 w-4" /> Nuovo Servizio
        </Button>
      </header>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex justify-center py-20"><Spinner size={40} /></div>
          ) : !services || services.length === 0 ? (
            <EmptyState 
              title="Nessun servizio" 
              description="Inizia aggiungendo il primo servizio del tuo salone." 
              icon={Scissors}
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Durata</TableHead>
                  <TableHead>Prezzo</TableHead>
                  <TableHead>Stato</TableHead>
                  <TableHead className="text-right">Azioni</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {services.map((service) => (
                  <TableRow key={service.id}>
                    <TableCell>
                      <div>
                        <p className="font-bold">{service.nome}</p>
                        <p className="text-xs text-muted-foreground line-clamp-1">{service.descrizione}</p>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center text-sm">
                        <Clock className="mr-2 h-3 w-3 text-muted-foreground" />
                        {service.durata} min
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center font-semibold">
                        <Euro className="mr-1 h-3 w-3 text-barber-500" />
                        {service.prezzo}
                      </div>
                    </TableCell>
                    <TableCell>
                      <span className={`inline-flex items-center rounded-full px-2 py-1 text-[10px] font-bold uppercase tracking-wider ${
                        service.attivo ? 'bg-green-100 text-green-700' : 'bg-zinc-100 text-zinc-500'
                      }`}>
                        {service.attivo ? 'Attivo' : 'Inattivo'}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button variant="ghost" size="icon" onClick={() => handleEdit(service)}>
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" className="text-destructive" onClick={() => setServiceToDelete(service.id)}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Add/Edit Dialog */}
      <Dialog open={isAddOpen} onOpenChange={(open) => !open && handleClose()}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>{editingService ? 'Modifica Servizio' : 'Nuovo Servizio'}</DialogTitle>
            <DialogDescription>
              Inserisci i dettagli del servizio. I servizi attivi sono visibili ai clienti nel wizard di prenotazione.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-4">
            <div className="grid gap-2">
              <Label htmlFor="nome">Nome Servizio</Label>
              <Input id="nome" placeholder="es. Taglio Classico" {...register("nome")} />
              {errors.nome && <p className="text-xs text-destructive">{errors.nome.message}</p>}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="descrizione">Descrizione</Label>
              <Textarea id="descrizione" placeholder="Descrivi il servizio..." {...register("descrizione")} />
              {errors.descrizione && <p className="text-xs text-destructive">{errors.descrizione.message}</p>}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="durata">Durata (min)</Label>
                <Input id="durata" type="number" {...register("durata", { valueAsNumber: true })} />
                {errors.durata && <p className="text-xs text-destructive">{errors.durata.message}</p>}
              </div>
              <div className="grid gap-2">
                <Label htmlFor="prezzo">Prezzo (€)</Label>
                <Input id="prezzo" type="number" step="0.01" {...register("prezzo", { valueAsNumber: true })} />
                {errors.prezzo && <p className="text-xs text-destructive">{errors.prezzo.message}</p>}
              </div>
            </div>
            <div className="flex items-center justify-between space-x-2 rounded-lg border p-4">
              <div className="space-y-0.5">
                <Label htmlFor="attivo">Stato Servizio</Label>
                <p className="text-xs text-muted-foreground">Disattiva per nasconderlo temporaneamente.</p>
              </div>
              <Switch 
                id="attivo" 
                checked={watch("attivo")} 
                onCheckedChange={(checked) => setValue("attivo", checked)} 
              />
            </div>
            <DialogFooter className="pt-4">
              <Button type="button" variant="ghost" onClick={handleClose}>Annulla</Button>
              <Button type="submit" className="bg-barber-500" disabled={isSubmitting}>
                {isSubmitting ? <Spinner size={16} className="mr-2 text-white" /> : null}
                {editingService ? 'Salva Modifiche' : 'Crea Servizio'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog 
        open={serviceToDelete !== null}
        onOpenChange={(open) => !open && setServiceToDelete(null)}
        title="Elimina Servizio"
        description="Sei sicuro di voler eliminare questo servizio? Le prenotazioni esistenti rimarranno intatte, ma il servizio non sarà più selezionabile."
        onConfirm={handleDelete}
        variant="destructive"
      />
    </div>
  );
}
