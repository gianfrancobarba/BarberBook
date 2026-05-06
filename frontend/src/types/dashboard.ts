import { type BookingResponseDto } from './booking';

export interface FreeSlotDto {
  start: string; // "HH:mm"
  end: string;   // "HH:mm"
}

export interface ChairDayScheduleDto {
  chairId: number;
  chairName: string;
  date: string;
  bookings: BookingResponseDto[];
  freeSlots: FreeSlotDto[];
}

export interface DailyDashboardDto {
  date: string;
  chairs: ChairDayScheduleDto[];
}

export interface DayScheduleDto {
  date: string;
  dayName: string;
  chairs: ChairDayScheduleDto[];
}

export interface WeeklyDashboardDto {
  weekStart: string;
  weekEnd: string;
  days: DayScheduleDto[];
}
