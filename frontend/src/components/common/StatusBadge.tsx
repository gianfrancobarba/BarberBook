import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

type BookingStatus = "IN_ATTESA" | "ACCETTATA" | "RIFIUTATA" | "ANNULLATA" | "PASSATA";

interface StatusBadgeProps {
  status: BookingStatus;
  className?: string;
}

const statusMap: Record<BookingStatus, { label: string; className: string }> = {
  IN_ATTESA: {
    label: "In Attesa",
    className: "bg-amber-100 text-amber-800 hover:bg-amber-100/80 border-amber-200",
  },
  ACCETTATA: {
    label: "Accettata",
    className: "bg-green-100 text-green-800 hover:bg-green-100/80 border-green-200",
  },
  RIFIUTATA: {
    label: "Rifiutata",
    className: "bg-red-100 text-red-800 hover:bg-red-100/80 border-red-200",
  },
  ANNULLATA: {
    label: "Annullata",
    className: "bg-gray-100 text-gray-500 hover:bg-gray-100/80 border-gray-200",
  },
  PASSATA: {
    label: "Passata",
    className: "bg-slate-100 text-slate-500 hover:bg-slate-100/80 border-slate-200",
  },
};

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const config = statusMap[status] || { label: status, className: "" };

  return (
    <Badge 
      variant="outline" 
      className={cn("font-medium", config.className, className)}
    >
      {config.label}
    </Badge>
  );
}
