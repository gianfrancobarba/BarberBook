import { cn } from "@/lib/utils";

interface LogoProps {
  /** Altezza Tailwind class, es. "h-8", "h-10", "h-16" */
  size?: string;
  /** Variante: "auto" si adatta al tema, "light" forza bianco (su sfondi scuri), "dark" forza nero */
  variant?: "auto" | "light" | "dark";
  className?: string;
}

/**
 * Logo Hair Man Tony.
 * - variant="auto"  → nero in light mode, bianco in dark mode
 * - variant="light" → sempre bianco (per hero scuri)
 * - variant="dark"  → sempre nero
 */
export function Logo({ size = "h-10", variant = "auto", className }: LogoProps) {
  return (
    <img
      src="/logo.png"
      alt="Hair Man Tony"
      className={cn(
        "w-auto object-contain",
        size,
        variant === "auto" && "dark:invert",
        variant === "light" && "invert",
        variant === "dark" && "",
        className
      )}
    />
  );
}
