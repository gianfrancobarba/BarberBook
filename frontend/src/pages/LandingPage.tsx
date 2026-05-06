import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useServices } from "@/hooks/useServices";
import { useAuthStore } from "@/stores/authStore";
import { Logo } from "@/components/common/Logo";
import { Scissors, Clock, Star, ArrowRight, Phone, MapPin, CalendarCheck, CheckCircle } from "lucide-react";

export default function LandingPage() {
  const { isAuthenticated } = useAuthStore();
  const { data: services, isLoading } = useServices();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background">
      {/* Hero */}
      <section className="relative bg-gradient-to-br from-barber-950 via-barber-900 to-barber-800 text-white overflow-hidden">
        {/* Texture overlay */}
        <div className="absolute inset-0 opacity-5 bg-[url('data:image/svg+xml,%3Csvg width=%2260%22 height=%2260%22 viewBox=%220 0 60 60%22 xmlns=%22http://www.w3.org/2000/svg%22%3E%3Cg fill=%22none%22 fill-rule=%22evenodd%22%3E%3Cg fill=%22%23ffffff%22 fill-opacity=%221%22%3E%3Cpath d=%22M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z%22/%3E%3C/g%3E%3C/g%3E%3C/svg%3E')]" />

        <div className="container mx-auto max-w-5xl px-4 py-20 md:py-28 relative">
          <div className="flex flex-col md:flex-row items-center gap-10">
            {/* Text */}
            <div className="flex-1 text-center md:text-left space-y-6">
              <div className="inline-flex items-center gap-2 bg-barber-500/20 border border-barber-500/30 rounded-full px-4 py-1.5 text-barber-200 text-sm font-medium">
                <Scissors className="h-3.5 w-3.5" />
                Prenota online — niente telefonate
              </div>
              <h1 className="text-4xl md:text-6xl font-bold font-heading leading-tight">
                Il tuo look perfetto,{" "}
                <span className="text-barber-400">un click</span> alla volta
              </h1>
              <p className="text-barber-200 text-lg max-w-xl">
                Scegli il servizio, seleziona data e orario, conferma. In meno di 2 minuti, anche senza registrarti.
              </p>
              <div className="flex flex-col sm:flex-row gap-3 justify-center md:justify-start pt-2">
                <Button
                  size="lg"
                  className="bg-barber-500 hover:bg-barber-400 text-white font-semibold text-base px-8 h-12"
                  onClick={() => navigate("/book")}
                >
                  <CalendarCheck className="mr-2 h-5 w-5" />
                  Prenota ora
                </Button>
                {!isAuthenticated() && (
                  <Link to="/register">
                    <Button size="lg" variant="outline" className="border-barber-400/50 text-barber-100 hover:bg-barber-800 w-full sm:w-auto h-12">
                      Crea account
                      <ArrowRight className="ml-2 h-4 w-4" />
                    </Button>
                  </Link>
                )}
              </div>
            </div>

            {/* Logo */}
            <div className="flex-shrink-0">
              <div className="relative">
                <div className="absolute inset-0 bg-barber-500/20 rounded-full blur-3xl" />
                <Logo size="h-52" variant="light" className="relative drop-shadow-2xl" />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features strip */}
      <section className="border-b bg-card py-6 px-4">
        <div className="container mx-auto max-w-4xl grid grid-cols-1 sm:grid-cols-3 gap-4 text-center text-sm">
          {[
            { icon: CalendarCheck, text: "Prenotazione in 2 minuti" },
            { icon: CheckCircle,   text: "Conferma immediata via notifica" },
            { icon: Clock,         text: "Gestisci e annulla quando vuoi" },
          ].map(({ icon: Icon, text }) => (
            <div key={text} className="flex items-center justify-center gap-2 text-muted-foreground">
              <Icon className="h-4 w-4 text-barber-500 flex-shrink-0" />
              <span>{text}</span>
            </div>
          ))}
        </div>
      </section>

      {/* Servizi */}
      <section className="py-16 px-4">
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
                <Card
                  key={service.id}
                  className="hover:shadow-md transition-all hover:border-barber-200 dark:hover:border-barber-800 cursor-pointer group"
                  onClick={() => navigate("/book")}
                >
                  <CardHeader className="pb-3">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <div className="p-1.5 rounded-md bg-barber-50 dark:bg-barber-950/30 group-hover:bg-barber-100 transition-colors">
                        <Scissors className="h-4 w-4 text-barber-500" />
                      </div>
                      {service.nome}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    {service.descrizione && (
                      <p className="text-sm text-muted-foreground">{service.descrizione}</p>
                    )}
                    <div className="flex items-center justify-between pt-1">
                      <div className="flex items-center gap-1 text-sm text-muted-foreground">
                        <Clock className="h-3.5 w-3.5" />
                        <span>{service.durata} min</span>
                      </div>
                      <span className="font-bold text-lg text-barber-600">€{service.prezzo}</span>
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
              onClick={() => navigate("/book")}
            >
              <CalendarCheck className="mr-2 h-5 w-5" />
              Prenota il tuo appuntamento
            </Button>
          </div>
        </div>
      </section>

      {/* Perché sceglierci */}
      <section className="py-16 px-4 bg-muted/30">
        <div className="container mx-auto max-w-4xl">
          <div className="text-center mb-10">
            <h2 className="text-3xl font-bold font-heading mb-2">Perché prenotare online?</h2>
            <p className="text-muted-foreground">Semplice, veloce, senza sorprese</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-center">
            {[
              {
                icon: CalendarCheck,
                title: "Sempre disponibile",
                desc: "Prenota 24 ore su 24, 7 giorni su 7. Niente più attese in linea."
              },
              {
                icon: Star,
                title: "Conferma garantita",
                desc: "Ricevi notifica appena Tony conferma il tuo slot. Nessuna incertezza."
              },
              {
                icon: Clock,
                title: "Gestione flessibile",
                desc: "Disdici o riprenota quando vuoi direttamente dal tuo profilo."
              },
            ].map(({ icon: Icon, title, desc }) => (
              <div key={title} className="space-y-3">
                <div className="mx-auto w-14 h-14 rounded-2xl bg-barber-50 dark:bg-barber-950/30 flex items-center justify-center">
                  <Icon className="h-7 w-7 text-barber-500" />
                </div>
                <h3 className="font-semibold text-lg">{title}</h3>
                <p className="text-muted-foreground text-sm">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA finale */}
      <section className="py-16 px-4 bg-gradient-to-r from-barber-900 to-barber-700 text-white">
        <div className="container mx-auto max-w-2xl text-center space-y-6">
          <h2 className="text-3xl font-bold font-heading">Pronto per il tuo prossimo look?</h2>
          <p className="text-barber-200">Prenota adesso — è gratis e senza registrazione</p>
          <Button
            size="lg"
            className="bg-white text-barber-900 hover:bg-barber-100 font-bold text-base px-10 h-12"
            onClick={() => navigate("/book")}
          >
            <CalendarCheck className="mr-2 h-5 w-5" />
            Prenota ora
          </Button>
        </div>
      </section>

      {/* Info */}
      <section className="py-10 px-4 bg-card border-t">
        <div className="container mx-auto max-w-4xl flex flex-col sm:flex-row items-center justify-between gap-6 text-sm text-muted-foreground">
          <div className="flex items-center gap-3">
            <Logo size="h-12" variant="auto" />
            <div>
              <p className="font-semibold text-foreground">Hair Man Tony</p>
              <p>Barbiere dal 1998</p>
            </div>
          </div>
          <div className="flex flex-col sm:flex-row gap-4">
            <span className="flex items-center gap-1.5">
              <MapPin className="h-4 w-4 text-barber-500" /> Via Roma 1, Città
            </span>
            <span className="flex items-center gap-1.5">
              <Phone className="h-4 w-4 text-barber-500" /> +39 333 123 4567
            </span>
          </div>
        </div>
      </section>
    </div>
  );
}
