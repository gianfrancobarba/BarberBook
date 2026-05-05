import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useServices } from "@/hooks/useServices";
import { useAvailableSlots } from "@/hooks/useAvailability";
import { useCreateBooking, useGuestBooking } from "@/hooks/useBookings";
import { useAuthStore } from "@/stores/authStore";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Calendar } from "@/components/ui/calendar";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Spinner } from "@/components/common/Spinner";
import { EmptyState } from "@/components/common/EmptyState";
import { toast } from "sonner";
import { 
  Scissors, 
  Calendar as CalendarIcon, 
  Clock, 
  User, 
  CheckCircle2, 
  ChevronRight, 
  ChevronLeft,
  CalendarCheck
} from "lucide-react";
import { format } from "date-fns";
import { it } from "date-fns/locale";
import { cn } from "@/lib/utils";
import { type ServiceResponseDto } from "@/types/service";
import { type TimeSlotDto } from "@/types/booking";

type Step = "SERVICE" | "DATE" | "DETAILS" | "SUCCESS";

export default function BookingFlowPage() {
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();
  
  // State
  const [step, setStep] = useState<Step>("SERVICE");
  const [selectedService, setSelectedService] = useState<ServiceResponseDto | null>(null);
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(new Date());
  const [selectedSlot, setSelectedSlot] = useState<TimeSlotDto | null>(null);
  
  // Guest Info
  const [guestInfo, setGuestInfo] = useState({
    nome: "",
    cognome: "",
    telefono: ""
  });

  // Queries & Mutations
  const { data: services, isLoading: isLoadingServices } = useServices();
  const { data: slots, isLoading: isLoadingSlots } = useAvailableSlots(
    selectedDate ? format(selectedDate, "yyyy-MM-dd") : null,
    selectedService?.id || null
  );
  
  const createBooking = useCreateBooking();
  const createGuestBooking = useGuestBooking();

  const handleServiceSelect = (service: ServiceResponseDto) => {
    setSelectedService(service);
    setStep("DATE");
  };


  const handleConfirm = async () => {
    if (!selectedService || !selectedSlot) return;

    try {
      if (isAuthenticated()) {
        await createBooking.mutateAsync({
          serviceId: selectedService.id,
          chairId: selectedSlot.chairId,
          startTime: selectedSlot.startTime,
        });
      } else {
        await createGuestBooking.mutateAsync({
          serviceId: selectedService.id,
          chairId: selectedSlot.chairId,
          startTime: selectedSlot.startTime,
          nome: guestInfo.nome,
          cognome: guestInfo.cognome,
          telefono: guestInfo.telefono,
        });
      }
      setStep("SUCCESS");
      toast.success("Prenotazione inviata con successo!");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Errore durante la prenotazione");
    }
  };

  // Render Helpers
  const renderHeader = () => (
    <div className="mb-8 text-center">
      <div className="flex justify-center mb-4">
        <div className="rounded-2xl bg-barber-500 p-3 text-white shadow-lg shadow-barber-500/20">
          <CalendarCheck className="h-8 w-8" />
        </div>
      </div>
      <h1 className="text-3xl font-heading font-bold tracking-tight">Prenota il tuo stile</h1>
      <p className="text-muted-foreground mt-2">Pochi passaggi per il tuo prossimo taglio</p>
    </div>
  );

  const renderSteps = () => (
    <div className="flex items-center justify-center space-x-4 mb-8">
      {[
        { id: "SERVICE", icon: Scissors },
        { id: "DATE", icon: Clock },
        { id: "DETAILS", icon: User },
      ].map((s, idx) => {
        const Icon = s.icon;
        const isActive = step === s.id;
        const isCompleted = ["DATE", "DETAILS", "SUCCESS"].includes(step) && idx === 0 || 
                           ["DETAILS", "SUCCESS"].includes(step) && idx === 1 ||
                           step === "SUCCESS" && idx === 2;
        
        return (
          <div key={s.id} className="flex items-center" aria-label={`Passaggio ${idx + 1}: ${s.id}${isActive ? ' (Corrente)' : isCompleted ? ' (Completato)' : ''}`}>
            <div className={cn(
              "flex h-10 w-10 items-center justify-center rounded-full border-2 transition-colors",
              isActive ? "border-barber-500 bg-barber-50 text-barber-500 dark:bg-barber-950/30" : 
              isCompleted ? "border-green-500 bg-green-50 text-green-500 dark:bg-green-950/30" : 
              "border-muted text-muted-foreground"
            )}>
              {isCompleted ? <CheckCircle2 className="h-5 w-5" aria-hidden="true" /> : <Icon className="h-5 w-5" aria-hidden="true" />}
            </div>
            {idx < 2 && (
              <div className={cn(
                "h-0.5 w-10 mx-2",
                isCompleted ? "bg-green-500" : "bg-muted"
              )} aria-hidden="true" />
            )}
          </div>
        );
      })}
    </div>
  );

  if (step === "SUCCESS") {
    return (
      <div className="container mx-auto max-w-xl py-12 px-4">
        <Card className="text-center p-8">
          <div className="flex justify-center mb-6">
            <div className="rounded-full bg-green-100 p-6 dark:bg-green-900/20">
              <CheckCircle2 className="h-16 w-16 text-green-600 dark:text-green-500" />
            </div>
          </div>
          <CardTitle className="text-3xl font-heading mb-4">Prenotazione Inviata!</CardTitle>
          <CardDescription className="text-lg mb-8">
            La tua richiesta è in fase di approvazione. Riceverai una notifica non appena il barbiere confermerà l'appuntamento.
          </CardDescription>
          <div className="bg-muted/50 rounded-xl p-6 mb-8 text-left space-y-3">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Servizio:</span>
              <span className="font-semibold">{selectedService?.nome}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Data:</span>
              <span className="font-semibold">
                {selectedSlot && format(new Date(selectedSlot.startTime), "EEEE d MMMM", { locale: it })}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Orario:</span>
              <span className="font-semibold">
                {selectedSlot && format(new Date(selectedSlot.startTime), "HH:mm")}
              </span>
            </div>
          </div>
          <div className="flex flex-col sm:flex-row gap-4">
            <Button className="flex-1 bg-barber-500" onClick={() => navigate(isAuthenticated() ? "/my" : "/")}>
              Vai alla tua area
            </Button>
            <Button variant="outline" className="flex-1" onClick={() => {
              setStep("SERVICE");
              setSelectedService(null);
              setSelectedSlot(null);
            }}>
              Prenota un altro
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-4xl py-12 px-4">
      {renderHeader()}
      {renderSteps()}

      <div className="grid gap-8">
        {/* STEP 1: SERVICE */}
        {step === "SERVICE" && (
          <div className="grid gap-4 sm:grid-cols-2">
            {isLoadingServices ? (
              <div className="col-span-full flex justify-center py-12"><Spinner size={40} /></div>
            ) : services?.length === 0 ? (
              <EmptyState 
                title="Nessun servizio disponibile" 
                description="Al momento non ci sono servizi prenotabili." 
                className="col-span-full"
              />
            ) : (
              services?.filter(s => s.attivo).map((service) => (
                <Card 
                  key={service.id} 
                  className={cn(
                    "cursor-pointer transition-all hover:ring-2 hover:ring-barber-500",
                    selectedService?.id === service.id && "ring-2 ring-barber-500"
                  )}
                  onClick={() => handleServiceSelect(service)}
                >
                  <CardHeader>
                    <div className="flex justify-between items-start">
                      <CardTitle className="text-xl">{service.nome}</CardTitle>
                      <span className="text-lg font-bold text-barber-500">€{service.prezzo}</span>
                    </div>
                    <CardDescription>{service.descrizione}</CardDescription>
                  </CardHeader>
                  <CardFooter className="text-sm text-muted-foreground">
                    <Clock className="mr-2 h-4 w-4" />
                    {service.durata} min
                  </CardFooter>
                </Card>
              ))
            )}
          </div>
        )}

        {/* STEP 2: DATE & TIME */}
        {step === "DATE" && (
          <div className="grid gap-8 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Seleziona Giorno</CardTitle>
              </CardHeader>
              <CardContent className="flex justify-center p-0 pb-4">
                <Calendar
                  mode="single"
                  selected={selectedDate}
                  onSelect={setSelectedDate}
                  locale={it}
                  disabled={(date) => date < new Date() || date.getDay() === 0} // Esempio: disabilita passato e domenica
                  className="rounded-md border-none"
                />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Seleziona Orario</CardTitle>
                <CardDescription>
                  {selectedDate && format(selectedDate, "EEEE d MMMM", { locale: it })}
                </CardDescription>
              </CardHeader>
              <CardContent>
                {isLoadingSlots ? (
                  <div className="flex justify-center py-8"><Spinner /></div>
                ) : !slots || slots.length === 0 ? (
                  <p className="text-center text-muted-foreground py-8">Nessun orario disponibile per questo giorno.</p>
                ) : (
                  <ScrollArea className="h-[300px] pr-4">
                    <div className="grid grid-cols-3 gap-2">
                      {slots.map((slot, idx) => (
                        <Button
                          key={idx}
                          variant={selectedSlot === slot ? "default" : "outline"}
                          className={cn(
                            "w-full",
                            selectedSlot === slot && "bg-barber-500"
                          )}
                          onClick={() => setSelectedSlot(slot)}
                        >
                          {format(new Date(slot.startTime), "HH:mm")}
                        </Button>
                      ))}
                    </div>
                  </ScrollArea>
                )}
              </CardContent>
              <CardFooter>
                <Button 
                  className="w-full bg-barber-500" 
                  disabled={!selectedSlot}
                  onClick={() => setStep("DETAILS")}
                >
                  Continua <ChevronRight className="ml-2 h-4 w-4" />
                </Button>
              </CardFooter>
            </Card>
            
            <Button variant="ghost" onClick={() => setStep("SERVICE")} className="lg:col-span-2">
              <ChevronLeft className="mr-2 h-4 w-4" /> Cambia servizio
            </Button>
          </div>
        )}

        {/* STEP 3: DETAILS & CONFIRM */}
        {step === "DETAILS" && (
          <div className="mx-auto w-full max-w-2xl">
            <Card>
              <CardHeader>
                <CardTitle>Riepilogo e Dati</CardTitle>
                <CardDescription>Conferma i dettagli della tua prenotazione</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="rounded-xl border p-4 space-y-3">
                  <div className="flex items-center space-x-3 text-barber-500">
                    <Scissors className="h-5 w-5" />
                    <span className="font-semibold text-foreground">{selectedService?.nome}</span>
                  </div>
                  <div className="flex items-center space-x-3 text-muted-foreground">
                    <CalendarIcon className="h-5 w-5" />
                    <span>{selectedDate && format(selectedDate, "EEEE d MMMM", { locale: it })} alle {selectedSlot && format(new Date(selectedSlot.startTime), "HH:mm")}</span>
                  </div>
                </div>

                {!isAuthenticated() ? (
                  <div className="space-y-4 border-t pt-6">
                    <h3 className="text-lg font-semibold">I tuoi dati</h3>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <div className="grid gap-2">
                        <Label htmlFor="nome">Nome</Label>
                        <Input 
                          id="nome" 
                          value={guestInfo.nome} 
                          onChange={(e) => setGuestInfo({...guestInfo, nome: e.target.value})} 
                        />
                      </div>
                      <div className="grid gap-2">
                        <Label htmlFor="cognome">Cognome</Label>
                        <Input 
                          id="cognome" 
                          value={guestInfo.cognome} 
                          onChange={(e) => setGuestInfo({...guestInfo, cognome: e.target.value})} 
                        />
                      </div>
                      <div className="grid gap-2 sm:col-span-2">
                        <Label htmlFor="telefono">Telefono</Label>
                        <Input 
                          id="telefono" 
                          value={guestInfo.telefono} 
                          onChange={(e) => setGuestInfo({...guestInfo, telefono: e.target.value})} 
                        />
                      </div>
                    </div>
                    <div className="text-center text-sm text-muted-foreground">
                      Vuoi accumulare punti? <Link to="/login" className="text-barber-500 underline">Accedi prima di prenotare</Link>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-2 border-t pt-6">
                    <h3 className="text-lg font-semibold">Cliente</h3>
                    <p className="text-muted-foreground">{user?.nome} {user?.cognome}</p>
                    <p className="text-sm text-muted-foreground">{user?.email}</p>
                  </div>
                )}
              </CardContent>
              <CardFooter className="flex flex-col gap-3">
                <Button 
                  className="w-full bg-barber-500 h-12 text-lg" 
                  disabled={createBooking.isPending || createGuestBooking.isPending || (!isAuthenticated() && (!guestInfo.nome || !guestInfo.telefono))}
                  onClick={handleConfirm}
                >
                  {createBooking.isPending || createGuestBooking.isPending ? (
                    <Spinner className="mr-2 h-5 w-5 text-white" />
                  ) : "Conferma Prenotazione"}
                </Button>
                <Button variant="ghost" onClick={() => setStep("DATE")} className="w-full">
                  Indietro
                </Button>
              </CardFooter>
            </Card>
          </div>
        )}
      </div>
    </div>
  );
}

