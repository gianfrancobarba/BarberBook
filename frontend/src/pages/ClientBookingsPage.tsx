import { useMyBookings, useCancelBooking, useRebook } from "@/hooks/useBookings";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/common/Spinner";
import { StatusBadge } from "@/components/common/StatusBadge";
import { EmptyState } from "@/components/common/EmptyState";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { toast } from "sonner";
import { 
  Calendar, 
  Clock, 
  Scissors, 
  XCircle, 
  RefreshCcw 
} from "lucide-react";
import { format } from "date-fns";
import { it } from "date-fns/locale";
import { useState } from "react";
import { type BookingResponseDto } from "@/types/booking";

export default function ClientBookingsPage() {
  const [activeTab, setActiveTab] = useState("upcoming");
  const [bookingToCancel, setBookingToCancel] = useState<number | null>(null);

  const { data: upcoming, isLoading: isLoadingUpcoming } = useMyBookings("ACCETTATA"); // Semplificato per demo, o mix di IN_ATTESA e ACCETTATA
  const { data: past, isLoading: isLoadingPast } = useMyBookings("PASSATA");
  
  const cancelBooking = useCancelBooking();
  const rebookMutation = useRebook();

  const handleCancel = async () => {
    if (bookingToCancel) {
      try {
        await cancelBooking.mutateAsync({ id: bookingToCancel, reason: "Annullata dal cliente" });
        toast.success("Prenotazione annullata");
        setBookingToCancel(null);
      } catch (error) {
        toast.error("Errore durante l'annullamento");
      }
    }
  };

  const handleRebook = async (id: number) => {
    try {
      await rebookMutation.mutateAsync(id);
      toast.success("Servizio aggiunto al nuovo carrello (mock)");
      // Qui si potrebbe reindirizzare al wizard di prenotazione pre-compilato
    } catch (error) {
      toast.error("Errore durante il riacquisto");
    }
  };

  const renderBookingList = (bookings: BookingResponseDto[] | undefined, isLoading: boolean, type: "upcoming" | "past") => {
    if (isLoading) return <div className="flex justify-center py-12"><Spinner size={40} /></div>;
    
    if (!bookings || bookings.length === 0) {
      return (
        <EmptyState 
          title={type === "upcoming" ? "Nessuna prenotazione imminente" : "Nessuno storico trovato"}
          description={type === "upcoming" ? "Prenota il tuo prossimo appuntamento oggi!" : "I tuoi servizi passati appariranno qui."}
          icon={Calendar}
        />
      );
    }

    return (
      <div className="grid gap-4">
        {bookings.map((booking) => (
          <Card key={booking.id} className="overflow-hidden">
            <div className="flex flex-col sm:flex-row">
              <div className="flex-1 p-6">
                <div className="flex justify-between items-start mb-4">
                  <div className="space-y-1">
                    <h3 className="text-xl font-bold">{booking.servizio.nome}</h3>
                    <div className="flex items-center text-sm text-muted-foreground">
                      <Calendar className="mr-2 h-4 w-4" />
                      {format(new Date(booking.startTime), "EEEE d MMMM yyyy", { locale: it })}
                    </div>
                    <div className="flex items-center text-sm text-muted-foreground">
                      <Clock className="mr-2 h-4 w-4" />
                      alle {format(new Date(booking.startTime), "HH:mm")}
                    </div>
                  </div>
                  <StatusBadge status={booking.stato} />
                </div>
                <div className="text-sm text-muted-foreground flex items-center">
                  <Scissors className="mr-2 h-4 w-4" />
                  Prezzo: €{booking.servizio.prezzo}
                </div>
              </div>
              <div className="bg-muted/50 p-4 flex flex-row sm:flex-col justify-center gap-2 sm:w-48 border-t sm:border-t-0 sm:border-l">
                {type === "upcoming" && (booking.stato === "IN_ATTESA" || booking.stato === "ACCETTATA") && (
                  <Button 
                    variant="destructive" 
                    size="sm" 
                    className="flex-1"
                    onClick={() => setBookingToCancel(booking.id)}
                  >
                    <XCircle className="mr-2 h-4 w-4" /> Annulla
                  </Button>
                )}
                {type === "past" && (
                  <Button 
                    variant="outline" 
                    size="sm" 
                    className="flex-1 text-barber-500 border-barber-500 hover:bg-barber-50"
                    onClick={() => handleRebook(booking.id)}
                  >
                    <RefreshCcw className="mr-2 h-4 w-4" /> Riprenota
                  </Button>
                )}
                <Button variant="ghost" size="sm" className="flex-1">Dettagli</Button>
              </div>
            </div>
          </Card>
        ))}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-heading font-bold">Le mie prenotazioni</h1>
        <p className="text-muted-foreground">Gestisci i tuoi appuntamenti e lo storico dei servizi.</p>
      </header>

      <Tabs defaultValue="upcoming" value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="upcoming">In arrivo</TabsTrigger>
          <TabsTrigger value="past">Passate</TabsTrigger>
        </TabsList>
        <div className="mt-6">
          <TabsContent value="upcoming">
            {renderBookingList(upcoming, isLoadingUpcoming, "upcoming")}
          </TabsContent>
          <TabsContent value="past">
            {renderBookingList(past, isLoadingPast, "past")}
          </TabsContent>
        </div>
      </Tabs>

      <ConfirmDialog 
        open={bookingToCancel !== null}
        onOpenChange={(open) => !open && setBookingToCancel(null)}
        title="Annulla Prenotazione"
        description="Sei sicuro di voler annullare questo appuntamento? L'azione non è reversibile."
        onConfirm={handleCancel}
        confirmLabel="Annulla Prenotazione"
        variant="destructive"
      />
    </div>
  );
}
