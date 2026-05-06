import { Bell, CheckCheck } from "lucide-react";
import { useNotifications, useMarkAllAsRead, useMarkAsRead } from "@/hooks/useNotifications";
import { useNotificationStore } from "@/stores/notificationStore";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import { formatDistanceToNow } from "date-fns";
import { it } from "date-fns/locale";

export function NotificationBell() {
  const { data: notifications, isLoading } = useNotifications();
  const unreadCount = useNotificationStore((state) => state.unreadCount);
  const { mutate: markAllRead } = useMarkAllAsRead();
  const { mutate: markRead } = useMarkAsRead();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger>
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-barber-500 text-[10px] font-bold text-white">
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80 p-0">
        <div className="flex items-center justify-between p-4 pb-2">
          <h4 className="font-heading font-semibold text-sm">Notifiche</h4>
          {unreadCount > 0 && (
            <Button 
              variant="ghost" 
              size="sm" 
              className="h-8 px-2 text-xs text-barber-500"
              onClick={() => markAllRead()}
            >
              <CheckCheck className="mr-1 h-3 w-3" />
              Segna tutto come letto
            </Button>
          )}
        </div>
        <Separator />
        <ScrollArea className="h-80">
          {isLoading ? (
            <div className="flex h-20 items-center justify-center text-xs text-muted-foreground">
              Caricamento...
            </div>
          ) : notifications?.length === 0 ? (
            <div className="flex h-20 items-center justify-center text-xs text-muted-foreground">
              Nessuna notifica
            </div>
          ) : (
            notifications?.map((notif) => (
              <div
                key={notif.id}
                className={cn(
                  "flex flex-col gap-1 p-4 text-sm transition-colors cursor-pointer hover:bg-muted",
                  !notif.letta && "bg-barber-50/50 dark:bg-barber-950/20 border-l-2 border-barber-500"
                )}
                onClick={() => !notif.letta && markRead(notif.id)}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-semibold line-clamp-1">{notif.titolo}</span>
                  <span className="text-[10px] text-muted-foreground whitespace-nowrap">
                    {formatDistanceToNow(new Date(notif.createdAt), { addSuffix: true, locale: it })}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground line-clamp-2">
                  {notif.messaggio}
                </p>
              </div>
            ))
          )}
        </ScrollArea>
        <Separator />
        <div className="p-2">
          <Button variant="ghost" className="w-full text-xs" size="sm">
            Vedi tutte le notifiche
          </Button>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
