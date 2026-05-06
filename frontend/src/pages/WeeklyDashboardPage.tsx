import { useState } from "react";
import { useWeeklyDashboard } from "@/hooks/useDashboard";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/common/Spinner";
import {
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
} from "lucide-react";
import { format, startOfWeek, addDays, subWeeks, addWeeks } from "date-fns";
import { it } from "date-fns/locale";
import { cn } from "@/lib/utils";

export default function WeeklyDashboardPage() {
  const [currentWeekStart, setCurrentWeekStart] = useState(
    startOfWeek(new Date(), { weekStartsOn: 1 })
  );

  const weekStr = format(currentWeekStart, "yyyy-MM-dd");
  const { data: dashboard, isLoading } = useWeeklyDashboard(weekStr);

  const handlePrevWeek = () => setCurrentWeekStart(subWeeks(currentWeekStart, 1));
  const handleNextWeek = () => setCurrentWeekStart(addWeeks(currentWeekStart, 1));
  const handleToday = () => setCurrentWeekStart(startOfWeek(new Date(), { weekStartsOn: 1 }));

  return (
    <div className="space-y-8">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold">Agenda Settimanale</h1>
          <p className="text-muted-foreground">
            {format(currentWeekStart, "d MMMM", { locale: it })} – {format(addDays(currentWeekStart, 6), "d MMMM yyyy", { locale: it })}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon" onClick={handlePrevWeek}><ChevronLeft className="h-4 w-4" /></Button>
          <Button variant="outline" onClick={handleToday}>Oggi</Button>
          <Button variant="outline" size="icon" onClick={handleNextWeek}><ChevronRight className="h-4 w-4" /></Button>
        </div>
      </header>

      {isLoading ? (
        <div className="flex justify-center py-20"><Spinner size={50} /></div>
      ) : (
        <div className="grid gap-4 lg:grid-cols-7">
          {dashboard?.days.map((day) => {
            const isToday = format(new Date(), "yyyy-MM-dd") === day.date;
            const dateObj = new Date(day.date + "T00:00:00");
            const allBookings = day.chairs.flatMap(c => c.bookings);
            const totalFreeSlots = day.chairs.reduce((sum, c) => sum + c.freeSlots.length, 0);

            return (
              <Card
                key={day.date}
                className={cn(
                  "flex flex-col h-full min-h-[400px] transition-all hover:ring-2 hover:ring-barber-500/50",
                  isToday && "ring-2 ring-barber-500 bg-barber-500/[0.02]"
                )}
              >
                <CardHeader className="p-4 border-b text-center">
                  <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    {format(dateObj, "EEE", { locale: it })}
                  </p>
                  <CardTitle className={cn("text-xl font-heading", isToday && "text-barber-500")}>
                    {format(dateObj, "d")}
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-3 flex-1 overflow-hidden">
                  <div className="mb-3 flex items-center justify-between text-[10px] text-muted-foreground font-bold">
                    <span>{allBookings.length} prenotazioni</span>
                    {totalFreeSlots > 0 && (
                      <span className="text-green-600">{totalFreeSlots} slot liberi</span>
                    )}
                  </div>

                  <div className="space-y-2">
                    {allBookings.slice(0, 5).map((b) => (
                      <div
                        key={b.id}
                        className="text-[10px] p-2 rounded bg-muted/50 border-l-2 border-barber-500 cursor-default"
                        title={`${b.servizio.nome} – ${b.client?.nome || b.guestNome}`}
                      >
                        <div className="font-bold flex justify-between">
                          <span>{format(new Date(b.startTime), "HH:mm")}</span>
                          <span className="text-barber-600 truncate ml-2">{b.client?.nome || b.guestNome}</span>
                        </div>
                        <div className="text-muted-foreground truncate">{b.servizio.nome}</div>
                      </div>
                    ))}
                    {allBookings.length > 5 && (
                      <div className="text-[10px] text-center text-muted-foreground pt-1">
                        + {allBookings.length - 5} altre
                      </div>
                    )}
                    {allBookings.length === 0 && (
                      <div className="h-full flex items-center justify-center pt-20">
                        <CalendarIcon className="h-8 w-8 text-muted/20" />
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      <footer className="flex items-center gap-6 text-sm text-muted-foreground bg-muted/30 p-4 rounded-xl">
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded-full bg-barber-500" />
          <span>Prenotazioni Confermate</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded-full bg-green-500" />
          <span>Slot Liberi</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded-full border-2 border-barber-500 bg-barber-500/5" />
          <span>Giorno Corrente</span>
        </div>
      </footer>
    </div>
  );
}
