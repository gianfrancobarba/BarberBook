import { renderHook, waitFor } from "@testing-library/react";
import { useLogin } from "@/hooks/useAuth";
import { describe, it, expect } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe("useLogin hook", () => {
  it("should login successfully and return user data", async () => {
    const { result } = renderHook(() => useLogin(), { wrapper });

    result.current.mutate({ email: "test@example.com", password: "password" });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.user.nome).toBe("Mario");
    expect(result.current.data?.accessToken).toBe("fake-jwt-token");
  });
});
