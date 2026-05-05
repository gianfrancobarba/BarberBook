import { render, screen } from "@testing-library/react";
import { StatusBadge } from "@/components/common/StatusBadge";
import { describe, it, expect } from "vitest";

describe("StatusBadge", () => {
  it("renders correctly with ACCETTATA status", () => {
    render(<StatusBadge status="ACCETTATA" />);
    const badge = screen.getByText(/Accettata/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-green-100");
  });

  it("renders correctly with IN_ATTESA status", () => {
    render(<StatusBadge status="IN_ATTESA" />);
    const badge = screen.getByText(/In attesa/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-amber-100");
  });

  it("renders correctly with RIFIUTATA status", () => {
    render(<StatusBadge status="RIFIUTATA" />);
    const badge = screen.getByText(/Rifiutata/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-red-100");
  });
});
