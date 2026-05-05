import { useAuthStore } from "@/stores/authStore";
import { useUpcomingBookings } from "@/hooks/useBookings";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/common/Spinner";
import { StatusBadge } from "@/components/common/StatusBadge";
import { EmptyState } from "@/components/common/EmptyState";
import { Link } from "react-router-dom";
import { 
  Calendar, 
  Clock, 
  Scissors, 
  MapPin, 
  PlusCircle, 
  History, 
  UserCircle 
} from "lucide-react";
import { format } from "date-fns";
import { it } from "date-fns/locale";

export default function ClientHomepage() {
  const { user } = useAuthStore();
  const { data: upcoming, isLoading } = useUpcomingBookings();

  const nextBooking = upcoming?.[0];

  return (
    <div className="space-y-8">
      {/* Welcome Section */}
      <section>
        <h1 className="text-3xl font-heading font-bold">Ciao, {user?.nome}! 👋</h1>
        <p className="text-muted-foreground">Bentornato nel tuo salone preferito.</p>
      </section>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {/* Next Appointment Card */}
        <Card className="md:col-span-2 border-barber-100 dark:border-barber-900/30">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-barber-500" />
              Prossimo Appuntamento
            </CardTitle>
            <CardDescription>I dettagli della tua prossima visita</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="flex justify-center py-8"><Spinner /></div>
            ) : nextBooking ? (
              <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 bg-muted/30 rounded-xl p-6">
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-barber-500 text-white">
                      <Scissors className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="font-bold text-lg">{nextBooking.servizio.nome}</p>
                      <p className="text-sm text-muted-foreground">{nextBooking.servizio.durata} minuti</p>
                    </div>
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-sm">
                      <Calendar className="h-4 w-4 text-muted-foreground" />
                      <span>{format(new Date(nextBooking.startTime), "EEEE d MMMM", { locale: it })}</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm">
                      <Clock className="h-4 w-4 text-muted-foreground" />
                      <span>alle {format(new Date(nextBooking.startTime), "HH:mm")}</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm">
                      <MapPin className="h-4 w-4 text-muted-foreground" />
                      <span>Poltrona: {nextBooking.poltrona.nome}</span>
                    </div>
                  </div>
                </div>
                <div className="flex flex-col gap-2 w-full md:w-auto">
                  <StatusBadge status={nextBooking.stato} className="text-center py-1 text-sm" />
                  <Link to={`/my/bookings`}>
                    <Button variant="outline" className="w-full">Gestisci</Button>
                  </Link>
                </div>
              </div>
            ) : (
              <EmptyState 
                title="Nessun appuntamento" 
                description="Non hai prenotazioni imminenti. Che ne dici di un nuovo taglio?"
                action={
                  <Link to="/book">
                    <Button className="bg-barber-500">Prenota Ora</Button>
                  </Link>
                }
              />
            )}
          </CardContent>
        </Card>

        {/* Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle>Azioni Rapide</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4">
            <Link to="/book">
              <Button className="w-full justify-start bg-barber-500 hover:bg-barber-600 h-12">
                <PlusCircle className="mr-3 h-5 w-5" />
                Nuova Prenotazione
              </Button>
            </Link>
            <Link to="/my/bookings">
              <Button variant="outline" className="w-full justify-start h-12">
                <History className="mr-3 h-5 w-5" />
                Storico Completo
              </Button>
            </Link>
            <Link to="/my/profile">
              <Button variant="outline" className="w-full justify-start h-12">
                <UserCircle className="mr-3 h-5 w-5" />
                Il mio Profilo
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>

      {/* Stats/Info Section (Optional but nice for premium feel) */}
      <section className="grid gap-6 md:grid-cols-3">
        <Card className="bg-gradient-to-br from-barber-500/10 to-transparent">
          <CardContent className="pt-6">
            <div className="text-2xl font-bold text-barber-500">Gold Member</div>
            <p className="text-sm text-muted-foreground">Grazie per la tua fedeltà!</p>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
