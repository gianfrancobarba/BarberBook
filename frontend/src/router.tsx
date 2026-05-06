import { createBrowserRouter, Navigate } from "react-router-dom";
import { PublicLayout } from "@/components/layout/PublicLayout";
import { ClientLayout } from "@/components/layout/ClientLayout";
import { BarberLayout } from "@/components/layout/BarberLayout";
import { ProtectedRoute } from "@/components/layout/ProtectedRoute";

import LandingPage from "@/pages/LandingPage";
import BookingFlowPage from "@/pages/BookingFlowPage";
import LoginPage from "@/pages/LoginPage";
import RegisterPage from "@/pages/RegisterPage";
import ForgotPasswordPage from "@/pages/ForgotPasswordPage";
import ResetPasswordPage from "@/pages/ResetPasswordPage";
import ClientHomepage from "@/pages/ClientHomepage";
import ClientBookingsPage from "@/pages/ClientBookingsPage";
import ClientProfilePage from "@/pages/ClientProfilePage";
import DailyDashboardPage from "@/pages/DailyDashboardPage";
import WeeklyDashboardPage from "@/pages/WeeklyDashboardPage";
import ManageServicesPage from "@/pages/ManageServicesPage";
import ManageChairsPage from "@/pages/ManageChairsPage";
import ManageSchedulesPage from "@/pages/ManageSchedulesPage";
import NotFoundPage from "@/pages/NotFoundPage";
import RouteErrorPage from "@/pages/RouteErrorPage";

export const router = createBrowserRouter([
  // Rotte Pubbliche Auth (senza layout)
  { path: "/login", element: <LoginPage />, errorElement: <RouteErrorPage /> },
  { path: "/register", element: <RegisterPage />, errorElement: <RouteErrorPage /> },
  { path: "/forgot-password", element: <ForgotPasswordPage />, errorElement: <RouteErrorPage /> },
  { path: "/reset-password", element: <ResetPasswordPage />, errorElement: <RouteErrorPage /> },

  // Rotte Pubbliche con PublicLayout (Vetrina e Prenotazione)
  {
    path: "/",
    element: <PublicLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <LandingPage /> },
      { path: "book", element: <BookingFlowPage /> },
    ],
  },

  // Rotte Protette CLIENT
  {
    path: "/my",
    element: <ProtectedRoute requiredRole="CLIENT" />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <ClientLayout />,
        children: [
          { index: true, element: <ClientHomepage /> },
          { path: "bookings", element: <ClientBookingsPage /> },
          { path: "profile", element: <ClientProfilePage /> },
        ],
      },
    ],
  },

  // Rotte Protette BARBER
  {
    path: "/barber",
    element: <ProtectedRoute requiredRole="BARBER" />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <BarberLayout />,
        children: [
          { index: true, element: <DailyDashboardPage /> },
          { path: "weekly", element: <WeeklyDashboardPage /> },
          { path: "services", element: <ManageServicesPage /> },
          { path: "chairs", element: <ManageChairsPage /> },
          { path: "schedules", element: <ManageSchedulesPage /> },
        ],
      },
    ],
  },

  // 404
  { path: "/404", element: <NotFoundPage />, errorElement: <RouteErrorPage /> },
  { path: "*", element: <Navigate to="/404" replace /> },
]);
