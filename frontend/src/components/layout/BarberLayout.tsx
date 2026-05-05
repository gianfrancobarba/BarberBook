import { Outlet, Link, useLocation } from "react-router-dom";
import { 
  LayoutDashboard, 
  Calendar, 
  Scissors, 
  Armchair, 
  Clock, 
  LogOut,
  Menu
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { NotificationBell } from "./NotificationBell";
import { useLogout } from "@/hooks/useAuth";
import { cn } from "@/lib/utils";
import { useState } from "react";

const navItems = [
  { icon: LayoutDashboard, label: "Agenda oggi", href: "/barber" },
  { icon: Calendar, label: "Settimana", href: "/barber/weekly" },
  { icon: Scissors, label: "Servizi", href: "/barber/services" },
  { icon: Armchair, label: "Poltrone", href: "/barber/chairs" },
  { icon: Clock, label: "Orari", href: "/barber/schedules" },
];

export function BarberLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const location = useLocation();
  const { mutate: logout } = useLogout();

  return (
    <div className="flex min-h-screen bg-background">
      {/* Sidebar Mobile Overlay */}
      {isSidebarOpen && (
        <div 
          className="fixed inset-0 z-40 bg-black/50 lg:hidden" 
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={cn(
        "fixed inset-y-0 left-0 z-50 w-64 border-r bg-card transition-transform lg:static lg:translate-x-0",
        !isSidebarOpen && "-translate-x-full"
      )}>
        <div className="flex h-full flex-col">
          <div className="flex h-16 items-center border-b px-6">
            <Link to="/barber" className="flex items-center space-x-2">
              <div className="rounded-lg bg-barber-500 p-1 text-white">
                <Scissors className="h-4 w-4" />
              </div>
              <span className="font-heading font-bold">BarberBook</span>
            </Link>
          </div>

          <nav className="flex-1 space-y-1 p-4">
            {navItems.map((item) => (
              <Link
                key={item.href}
                to={item.href}
                className={cn(
                  "flex items-center space-x-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  location.pathname === item.href 
                    ? "bg-barber-500 text-white" 
                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                )}
                onClick={() => setIsSidebarOpen(false)}
              >
                <item.icon className="h-4 w-4" />
                <span>{item.label}</span>
              </Link>
            ))}
          </nav>

          <div className="border-t p-4">
            <Button 
              variant="ghost" 
              className="w-full justify-start text-muted-foreground hover:text-destructive"
              onClick={() => logout()}
            >
              <LogOut className="mr-3 h-4 w-4" />
              Esci
            </Button>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b bg-background px-4 lg:px-8">
          <Button 
            variant="ghost" 
            size="icon" 
            className="lg:hidden"
            onClick={() => setIsSidebarOpen(true)}
          >
            <Menu className="h-6 w-6" />
          </Button>

          <div className="ml-auto flex items-center space-x-4">
            <NotificationBell />
            <div className="h-8 w-px bg-border" />
            <div className="flex items-center space-x-3">
              <div className="h-8 w-8 rounded-full bg-barber-100 flex items-center justify-center text-barber-700 font-bold text-xs dark:bg-barber-900/30 dark:text-barber-500">
                BA
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 p-4 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
