import { useState } from "react";
import { useDailyDashboard } from "@/hooks/useDashboard";
import { useAcceptBooking, useRejectBooking, usePendingBookings } from "@/hooks/useBookings";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/common/Spinner";
import { StatusBadge } from "@/components/common/StatusBadge";
import { EmptyState } from "@/components/common/EmptyState";
import { ScrollArea } from "@/components/ui/scroll-area";
import { toast } from "sonner";
import {
  Calendar as CalendarIcon,
  Clock,
  Users,
  TrendingUp,
  Check,
  X,
  Scissors,
  Armchair
} from "lucide-react";
import { format } from "date-fns";
import { it } from "date-fns/locale";
import { cn } from "@/lib/utils";

export default function DailyDashboardPage() {
  const [selectedDate, setSelectedDate] = useState(new Date());
  const dateStr = format(selectedDate, "yyyy-MM-dd");

  const { data: dashboard, isLoading: isLoadingDash } = useDailyDashboard(dateStr);
  const { data: pending, isLoading: isLoadingPending } = usePendingBookings();

  const acceptMutation = useAcceptBooking();
  const rejectMutation = useRejectBooking();

  const allBookings = dashboard?.chairs.flatMap(c => c.bookings) ?? [];
  const totalBookings = allBookings.length;
  const pendingCount = allBookings.filter(b => b.stato === "IN_ATTESA").length;
  const confirmedCount = allBookings.filter(b => b.stato === "ACCETTATA").length;
  const expectedRevenue = allBookings
    .filter(b => b.stato === "ACCETTATA")
    .reduce((sum, b) => sum + (b.servizio.prezzo ?? 0), 0);

  const handleAccept = async (id: number) => {
    try {
      await acceptMutation.mutateAsync(id);
      toast.success("Prenotazione accettata");
    } catch {
      toast.error("Errore durante l'approvazione");
    }
  };

  const handleReject = async (id: number) => {
    try {
      await rejectMutation.mutateAsync(id);
      toast.success("Prenotazione rifiutata");
    } catch {
      toast.error("Errore durante il rifiuto");
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold">Agenda Giornaliera</h1>
          <p className="text-muted-foreground">{format(selectedDate, "EEEE d MMMM yyyy", { locale: it })}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => setSelectedDate(new Date())}>Oggi</Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatsCard title="Totale Prenotazioni" value={totalBookings} icon={CalendarIcon} />
        <StatsCard title="In Attesa" value={pendingCount} icon={Clock} className="border-amber-500/50 bg-amber-500/5" />
        <StatsCard title="Confermate" value={confirmedCount} icon={Check} className="border-green-500/50 bg-green-500/5" />
        <StatsCard title="Incasso Previsto" value={`€${expectedRevenue}`} icon={TrendingUp} />
      </div>

      <div className="grid gap-8 lg:grid-cols-3">
        <div className="lg:col-span-1 space-y-4">
          <h2 className="text-xl font-heading font-semibold flex items-center gap-2">
            <Users className="h-5 w-5 text-amber-500" />
            Richieste Pendenti
          </h2>
          <ScrollArea className="h-[600px] rounded-xl border bg-card">
            <div className="p-4 space-y-4">
              {isLoadingPending ? (
                <div className="flex justify-center py-12"><Spinner /></div>
              ) : !pending || pending.length === 0 ? (
                <p className="text-center text-muted-foreground py-12 text-sm">Nessuna richiesta in attesa</p>
              ) : (
                pending.map((p) => (
                  <Card key={p.id} className="border-l-4 border-l-amber-500 overflow-hidden shadow-sm">
                    <CardContent className="p-4 space-y-3">
                      <div className="flex justify-between items-start">
                        <div>
                          <p className="font-bold">{p.client?.nome || p.guestNome} {p.client?.cognome || p.guestCognome}</p>
                          <p className="text-xs text-muted-foreground">{p.servizio.nome}</p>
                        </div>
                        <span className="text-xs font-mono font-bold bg-muted px-2 py-1 rounded">
                          {format(new Date(p.startTime), "HH:mm")}
                        </span>
                      </div>
                      <div className="flex gap-2 pt-2">
                        <Button
                          size="sm"
                          className="flex-1 bg-green-600 hover:bg-green-700 h-8"
                          onClick={() => handleAccept(p.id)}
                          disabled={acceptMutation.isPending}
                        >
                          <Check className="h-4 w-4 mr-1" /> Accetta
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          className="flex-1 h-8 text-destructive hover:bg-destructive/10 border-destructive/20"
                          onClick={() => handleReject(p.id)}
                          disabled={rejectMutation.isPending}
                        >
                          <X className="h-4 w-4 mr-1" /> Rifiuta
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </ScrollArea>
        </div>

        <div className="lg:col-span-2 space-y-4">
          <h2 className="text-xl font-heading font-semibold flex items-center gap-2">
            <Armchair className="h-5 w-5 text-barber-500" />
            Postazioni
          </h2>
          {isLoadingDash ? (
            <div className="flex justify-center py-12"><Spinner size={40} /></div>
          ) : !dashboard || dashboard.chairs.length === 0 ? (
            <EmptyState title="Nessuna poltrona configurata" description="Aggiungi delle poltrone nelle impostazioni." />
          ) : (
            <div className="grid gap-6">
              {dashboard.chairs.map((chair) => (
                <Card key={chair.chairId} className="overflow-hidden">
                  <CardHeader className="bg-muted/30 py-3">
                    <CardTitle className="text-sm font-medium flex items-center gap-2">
                      <div className="h-2 w-2 rounded-full bg-barber-500" />
                      {chair.chairName}
                      {chair.freeSlots.length > 0 && (
                        <span className="ml-auto text-[10px] text-green-600 font-normal">
                          {chair.freeSlots.length} slot liberi
                        </span>
                      )}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="p-0">
                    {chair.bookings.length === 0 ? (
                      <div className="p-8 text-center text-sm text-muted-foreground italic">
                        Nessun appuntamento programmato
                      </div>
                    ) : (
                      <div className="divide-y">
                        {chair.bookings.map((b) => (
                          <div key={b.id} className="flex items-center gap-4 p-4 hover:bg-muted/20 transition-colors">
                            <div className="w-16 text-center font-mono font-bold text-sm">
                              {format(new Date(b.startTime), "HH:mm")}
                            </div>
                            <div className="flex-1">
                              <p className="font-semibold text-sm">
                                {b.client?.nome || b.guestNome} {b.client?.cognome || b.guestCognome}
                                {b.guestNome && <span className="ml-2 text-[10px] bg-zinc-100 dark:bg-zinc-800 px-1.5 py-0.5 rounded text-muted-foreground uppercase tracking-wider">Guest</span>}
                              </p>
                              <p className="text-xs text-muted-foreground flex items-center gap-1">
                                <Scissors className="h-3 w-3" /> {b.servizio.nome} ({b.servizio.durata}m)
                              </p>
                            </div>
                            <StatusBadge status={b.stato} className="text-[10px] h-5" />
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function StatsCard({ title, value, icon: Icon, className }: { title: string; value: string | number; icon: React.ElementType; className?: string }) {
  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <Icon className="h-4 w-4 text-muted-foreground" />
        </div>
        <div className="mt-2">
          <h3 className="text-2xl font-bold">{value}</h3>
        </div>
      </CardContent>
    </Card>
  );
}
