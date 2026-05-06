import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useServices } from "@/hooks/useServices";
import { useAuthStore } from "@/stores/authStore";
import { Scissors, Clock, Star, ArrowRight, Phone, MapPin, CalendarCheck } from "lucide-react";

export default function LandingPage() {
  const { isAuthenticated, isBarber, user } = useAuthStore();
  const { data: services, isLoading } = useServices();
  const navigate = useNavigate();

  const handleBookNow = () => {
    navigate("/book");
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b bg-card">
        <div className="container mx-auto flex items-center justify-between px-4 py-4">
          <div className="flex items-center gap-2">
            <Scissors className="h-6 w-6 text-barber-500" />
            <span className="text-xl font-bold font-heading">Hair Man Tony</span>
          </div>
          <nav className="flex items-center gap-3">
            {isAuthenticated() ? (
              <>
                <Link
                  to={isBarber() ? "/barber" : "/my"}
                  className="text-sm font-medium hover:text-barber-500 transition-colors"
                >
                  {isBarber() ? "Dashboard" : `Ciao, ${user?.nome}`}
                </Link>
                <Button size="sm" onClick={handleBookNow}>
                  Prenota
                </Button>
              </>
            ) : (
              <>
                <Link to="/login">
                  <Button variant="ghost" size="sm">Accedi</Button>
                </Link>
                <Link to="/register">
                  <Button size="sm" className="bg-barber-500 hover:bg-barber-600">Registrati</Button>
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>

      {/* Hero */}
      <section className="bg-gradient-to-br from-barber-950 via-barber-900 to-barber-800 text-white py-20 px-4">
        <div className="container mx-auto max-w-4xl text-center space-y-6">
          <Badge className="bg-barber-500/20 text-barber-200 border-barber-500/30 text-sm">
            Hair Man Tony — Barbiere dal 1998
          </Badge>
          <h1 className="text-4xl md:text-6xl font-bold font-heading leading-tight">
            Il tuo look perfetto,{" "}
            <span className="text-barber-400">un click</span> alla volta
          </h1>
          <p className="text-barber-200 text-lg md:text-xl max-w-2xl mx-auto">
            Prenota il tuo appuntamento online in pochi secondi. Niente telefonate, niente attese.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center pt-2">
            <Button
              size="lg"
              className="bg-barber-500 hover:bg-barber-600 text-white font-semibold text-lg px-8"
              onClick={handleBookNow}
            >
              <CalendarCheck className="mr-2 h-5 w-5" />
              Prenota ora
            </Button>
            {!isAuthenticated() && (
              <Link to="/register">
                <Button size="lg" variant="outline" className="border-barber-400 text-barber-200 hover:bg-barber-800 w-full sm:w-auto">
                  Crea account
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Button>
              </Link>
            )}
          </div>
        </div>
      </section>

      {/* Servizi */}
      <section className="py-16 px-4 bg-muted/30">
        <div className="container mx-auto max-w-5xl">
          <div className="text-center mb-10">
            <h2 className="text-3xl font-bold font-heading mb-2">I nostri servizi</h2>
            <p className="text-muted-foreground">Qualità e cura per ogni esigenza</p>
          </div>

          {isLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {[...Array(3)].map((_, i) => (
                <Card key={i} className="animate-pulse">
                  <CardContent className="p-6 space-y-3">
                    <div className="h-4 bg-muted rounded w-3/4" />
                    <div className="h-3 bg-muted rounded w-full" />
                    <div className="h-3 bg-muted rounded w-1/2" />
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {services?.filter(s => s.attivo).map((service) => (
                <Card key={service.id} className="hover:shadow-md transition-shadow">
                  <CardHeader className="pb-3">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <Scissors className="h-4 w-4 text-barber-500 flex-shrink-0" />
                      {service.nome}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    {service.descrizione && (
                      <p className="text-sm text-muted-foreground">{service.descrizione}</p>
                    )}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1 text-sm text-muted-foreground">
                        <Clock className="h-3.5 w-3.5" />
                        <span>{service.durata} min</span>
                      </div>
                      <span className="font-bold text-barber-600">€{service.prezzo}</span>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}

          <div className="text-center mt-10">
            <Button
              size="lg"
              className="bg-barber-500 hover:bg-barber-600"
              onClick={handleBookNow}
            >
              <CalendarCheck className="mr-2 h-5 w-5" />
              Prenota il tuo appuntamento
            </Button>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-16 px-4">
        <div className="container mx-auto max-w-4xl">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-center">
            <div className="space-y-3">
              <div className="mx-auto w-12 h-12 rounded-full bg-barber-100 flex items-center justify-center">
                <CalendarCheck className="h-6 w-6 text-barber-600" />
              </div>
              <h3 className="font-semibold text-lg">Prenotazione rapida</h3>
              <p className="text-muted-foreground text-sm">Prenota in meno di 2 minuti, anche senza account</p>
            </div>
            <div className="space-y-3">
              <div className="mx-auto w-12 h-12 rounded-full bg-barber-100 flex items-center justify-center">
                <Star className="h-6 w-6 text-barber-600" />
              </div>
              <h3 className="font-semibold text-lg">Conferma immediata</h3>
              <p className="text-muted-foreground text-sm">Ricevi notifica appena il barbiere conferma il tuo appuntamento</p>
            </div>
            <div className="space-y-3">
              <div className="mx-auto w-12 h-12 rounded-full bg-barber-100 flex items-center justify-center">
                <Clock className="h-6 w-6 text-barber-600" />
              </div>
              <h3 className="font-semibold text-lg">Gestisci i tuoi appuntamenti</h3>
              <p className="text-muted-foreground text-sm">Visualizza, modifica o annulla quando vuoi dal tuo profilo</p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t bg-card py-8 px-4">
        <div className="container mx-auto max-w-5xl flex flex-col md:flex-row items-center justify-between gap-4 text-sm text-muted-foreground">
          <div className="flex items-center gap-2">
            <Scissors className="h-4 w-4 text-barber-500" />
            <span className="font-semibold text-foreground">Hair Man Tony</span>
          </div>
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1">
              <MapPin className="h-3.5 w-3.5" /> Via Roma 1, Città
            </span>
            <span className="flex items-center gap-1">
              <Phone className="h-3.5 w-3.5" /> +39 000 000 0000
            </span>
          </div>
          <div className="flex items-center gap-4">
            <Link to="/login" className="hover:text-barber-500 transition-colors">Accedi</Link>
            <Link to="/register" className="hover:text-barber-500 transition-colors">Registrati</Link>
            <Link to="/book" className="hover:text-barber-500 transition-colors">Prenota</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
