import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '@/api/dashboard';

export function useDailyDashboard(date?: string) {
  return useQuery({
    queryKey: ['dashboard', 'daily', date],
    queryFn: () => dashboardApi.getDaily(date),
    refetchInterval: 60 * 1000, // Refresh ogni minuto
  });
}

export function useWeeklyDashboard(weekStart?: string) {
  return useQuery({
    queryKey: ['dashboard', 'weekly', weekStart],
    queryFn: () => dashboardApi.getWeekly(weekStart),
  });
}
