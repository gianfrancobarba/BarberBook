import { type UserResponseDto } from './auth';
import { type ServiceResponseDto } from './service';
import { type ChairResponseDto } from './chair';

export type BookingStatus = "IN_ATTESA" | "ACCETTATA" | "RIFIUTATA" | "ANNULLATA" | "PASSATA";

export interface BookingResponseDto {
  id: number;
  client?: UserResponseDto; // opzionale se guest
  guestNome?: string;
  guestCognome?: string;
  guestTelefono?: string;
  servizio: ServiceResponseDto;
  poltrona: ChairResponseDto;
  startTime: string;
  endTime: string;
  stato: BookingStatus;
  commentoAnnullamento?: string;
  createdAt: string;
}

export interface CreateBookingRequestDto {
  serviceId: number;
  chairId: number;
  startTime: string; // ISO format
}

export interface GuestBookingRequestDto extends CreateBookingRequestDto {
  nome: string;
  cognome: string;
  telefono: string;
}

export interface DirectBookingRequestDto extends CreateBookingRequestDto {
  clientId?: number;
  guestNome?: string;
  guestCognome?: string;
  guestTelefono?: string;
}

export interface TimeSlotDto {
  startTime: string;
  endTime: string;
  available: boolean;
  chairId: number;
}
