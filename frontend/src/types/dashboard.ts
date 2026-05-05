import { type BookingResponseDto } from './booking';

export interface DailyDashboardDto {
  date: string;
  totalBookings: number;
  pendingBookings: number;
  confirmedBookings: number;
  expectedRevenue: number;
  bookingsByChair: Record<string, BookingResponseDto[]>;
}

export interface WeeklyDashboardDto {
  weekStart: string;
  weekEnd: string;
  days: {
    date: string;
    totalBookings: number;
    bookings: BookingResponseDto[];
  }[];
}
