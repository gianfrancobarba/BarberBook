export type BookingStatus = "IN_ATTESA" | "ACCETTATA" | "RIFIUTATA" | "ANNULLATA" | "PASSATA";

export interface BookingResponseDto {
  id: number;
  chairId: number;
  chairName: string;
  serviceId: number;
  serviceName: string;
  serviceDurationMinutes: number;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  customerName: string;
  isGuest: boolean;
  guestPhone?: string;
  cancellationReason?: string;
  createdAt: string;
}

export interface CreateBookingRequestDto {
  serviceId: number;
  chairId: number;
  date: string;      // "yyyy-MM-dd"
  startTime: string; // "HH:mm:ss"
}

export interface GuestBookingRequestDto extends CreateBookingRequestDto {
  guestNome: string;
  guestCognome: string;
  guestTelefono: string;
}

export interface DirectBookingRequestDto extends CreateBookingRequestDto {
  clientId?: number;
  guestNome?: string;
  guestCognome?: string;
  guestTelefono?: string;
}

/** Slot come restituito dal backend: solo ora, senza data */
export interface AvailabilitySlotDto {
  start: string; // "HH:mm"
  end: string;   // "HH:mm"
}

/** Risposta availability per una singola poltrona */
export interface AvailabilityResponseDto {
  chairId: number;
  chairName: string;
  availableSlots: AvailabilitySlotDto[];
}

/** Slot arricchito usato nel booking flow: data+ora completa + chairId */
export interface BookableSlot {
  chairId: number;
  chairName: string;
  startTime: string; // ISO: "yyyy-MM-ddTHH:mm:ss"
  endTime: string;   // ISO: "yyyy-MM-ddTHH:mm:ss"
}
