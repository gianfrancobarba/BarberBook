import { useState } from "react";
import { useSchedules, useAddSchedule, useDeleteSchedule } from "@/hooks/useSchedules";
import { useChairs } from "@/hooks/useChairs";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from "@/components/ui/select";
import { Spinner } from "@/components/common/Spinner";
import { EmptyState } from "@/components/common/EmptyState";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { toast } from "sonner";
import { 
  Plus, 
  Trash2, 
  Clock, 
  Calendar as CalendarIcon,
  Coffee,
  Armchair
} from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { cn } from "@/lib/utils";

const scheduleSchema = z.object({
  chairId: z.number().min(1, "Seleziona una poltrona"),
  dayOfWeek: z.string().min(1, "Seleziona un giorno"),
  startTime: z.string().regex(/^([01]\d|2[0-3]):([0-5]\d)$/, "Formato HH:mm"),
  endTime: z.string().regex(/^([01]\d|2[0-3]):([0-5]\d)$/, "Formato HH:mm"),
  isPausa: z.boolean().default(false),
});

type ScheduleFormValues = z.infer<typeof scheduleSchema>;

const DAYS_OF_WEEK = [
  { label: "Lunedì", value: "MONDAY" },
  { label: "Martedì", value: "TUESDAY" },
  { label: "Mercoledì", value: "WEDNESDAY" },
  { label: "Giovedì", value: "THURSDAY" },
  { label: "Venerdì", value: "FRIDAY" },
  { label: "Sabato", value: "SATURDAY" },
  { label: "Domenica", value: "SUNDAY" },
];

export default function ManageSchedulesPage() {
  const [selectedChairId, setSelectedChairId] = useState<string>("all");
  const [scheduleToDelete, setScheduleToDelete] = useState<number | null>(null);

  const { data: chairs } = useChairs();
  const { data: schedules, isLoading } = useSchedules(selectedChairId === "all" ? undefined : Number(selectedChairId));
  
  const createMutation = useAddSchedule();
  const deleteMutation = useDeleteSchedule();

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ScheduleFormValues>({
    resolver: zodResolver(scheduleSchema) as any,
    defaultValues: { isPausa: false },
  });

  const onSubmit = async (data: ScheduleFormValues) => {
    try {
      await createMutation.mutateAsync(data);
      toast.success(data.isPausa ? "Pausa aggiunta" : "Orario lavorativo aggiunto");
      reset({ ...data, startTime: "", endTime: "" });
    } catch (error) {
      toast.error("Errore durante il salvataggio (possibile sovrapposizione)");
    }
  };

  const handleDelete = async () => {
    if (scheduleToDelete) {
      try {
        await deleteMutation.mutateAsync(scheduleToDelete);
        toast.success("Orario eliminato");
        setScheduleToDelete(null);
      } catch (error) {
        toast.error("Errore durante l'eliminazione");
      }
    }
  };

  return (
    <div className="space-y-8">
      <header>
        <h1 className="text-3xl font-heading font-bold">Orari e Disponibilità</h1>
        <p className="text-muted-foreground">Configura gli orari di apertura e le pause per ogni postazione.</p>
      </header>

      <div className="grid gap-8 lg:grid-cols-3">
        {/* Form Column */}
        <Card className="lg:col-span-1 h-fit sticky top-6">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Plus className="h-5 w-5 text-barber-500" />
              Aggiungi Slot
            </CardTitle>
            <CardDescription>Crea un nuovo orario di lavoro o una pausa.</CardDescription>
          </CardHeader>
          <form onSubmit={handleSubmit(onSubmit)}>
            <CardContent className="space-y-4">
              <div className="grid gap-2">
                <Label>Poltrona</Label>
                <Select onValueChange={(val) => setValue("chairId", Number(val))}>
                  <SelectTrigger>
                    <SelectValue placeholder="Seleziona poltrona" />
                  </SelectTrigger>
                  <SelectContent>
                    {chairs?.map((c) => (
                      <SelectItem key={c.id} value={c.id.toString()}>{c.nome}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.chairId && <p className="text-xs text-destructive">{errors.chairId.message}</p>}
              </div>

              <div className="grid gap-2">
                <Label>Giorno</Label>
                <Select onValueChange={(val: any) => setValue("dayOfWeek", val as string)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Seleziona giorno" />
                  </SelectTrigger>
                  <SelectContent>
                    {DAYS_OF_WEEK.map((d) => (
                      <SelectItem key={d.value} value={d.value}>{d.label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.dayOfWeek && <p className="text-xs text-destructive">{errors.dayOfWeek.message}</p>}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="startTime">Inizio</Label>
                  <Input id="startTime" type="time" {...register("startTime")} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="endTime">Fine</Label>
                  <Input id="endTime" type="time" {...register("endTime")} />
                </div>
              </div>

              <div className="flex items-center gap-2 pt-2">
                <Button 
                  type="button" 
                  variant={watch("isPausa") ? "outline" : "default"}
                  className={cn("flex-1", !watch("isPausa") && "bg-barber-500")}
                  onClick={() => setValue("isPausa", false)}
                >
                  Lavoro
                </Button>
                <Button 
                  type="button" 
                  variant={watch("isPausa") ? "default" : "outline"}
                  className={cn("flex-1", watch("isPausa") && "bg-amber-500 hover:bg-amber-600")}
                  onClick={() => setValue("isPausa", true)}
                >
                  Pausa
                </Button>
              </div>
            </CardContent>
            <CardFooter>
              <Button type="submit" className="w-full bg-zinc-900" disabled={isSubmitting}>
                {isSubmitting ? <Spinner size={16} className="mr-2 text-white" /> : <Plus className="mr-2 h-4 w-4" />}
                Aggiungi
              </Button>
            </CardFooter>
          </form>
        </Card>

        {/* List Column */}
        <div className="lg:col-span-2 space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-heading font-semibold flex items-center gap-2">
              <CalendarIcon className="h-5 w-5 text-barber-500" />
              Palinsesto Attuale
            </h2>
            <Select value={selectedChairId} onValueChange={(val) => setSelectedChairId(val || "all")}>
              <SelectTrigger className="w-[200px]">
                <SelectValue placeholder="Tutte le poltrone" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tutte le poltrone</SelectItem>
                {chairs?.map((c) => (
                  <SelectItem key={c.id} value={c.id.toString()}>{c.nome}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-4">
            {isLoading ? (
              <div className="flex justify-center py-12"><Spinner size={40} /></div>
            ) : !schedules || schedules.length === 0 ? (
              <EmptyState 
                title="Nessun orario configurato" 
                description="Usa il modulo a sinistra per definire quando il salone è operativo." 
                icon={Clock}
              />
            ) : (
              <div className="grid gap-3">
                {[...schedules].sort((a: any, b: any) => (a.dayOfWeek || "").localeCompare(b.dayOfWeek || "")).map((s: any) => (
                  <Card key={s.id} className={cn(
                    "border-l-4",
                    s.isPausa ? "border-l-amber-500 bg-amber-50/30" : "border-l-barber-500"
                  )}>
                    <CardContent className="p-4 flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div className={cn(
                          "p-2 rounded-lg",
                          s.isPausa ? "bg-amber-100 text-amber-600" : "bg-barber-100 text-barber-600"
                        )}>
                          {s.isPausa ? <Coffee className="h-4 w-4" /> : <Armchair className="h-4 w-4" />}
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-sm">
                              {DAYS_OF_WEEK.find(d => d.value === s.dayOfWeek)?.label}
                            </span>
                            <span className="text-xs text-muted-foreground">•</span>
                            <span className="text-xs font-medium uppercase">{s.chair?.nome}</span>
                          </div>
                          <div className="flex items-center gap-2 mt-1">
                            <Clock className="h-3 w-3 text-muted-foreground" />
                            <span className="text-sm font-mono">{s.startTime.slice(0,5)} - {s.endTime.slice(0,5)}</span>
                            {s.isPausa && <span className="text-[10px] bg-amber-200 text-amber-800 px-1.5 py-0.5 rounded font-bold uppercase">Pausa</span>}
                          </div>
                        </div>
                      </div>
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        className="text-destructive hover:bg-destructive/10"
                        onClick={() => setScheduleToDelete(s.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <ConfirmDialog 
        open={scheduleToDelete !== null}
        onOpenChange={(open) => !open && setScheduleToDelete(null)}
        title="Elimina Orario"
        description="Sei sicuro di voler rimuovere questa fascia oraria? Le prenotazioni esistenti potrebbero essere influenzate."
        onConfirm={handleDelete}
        variant="destructive"
      />
    </div>
  );
}
