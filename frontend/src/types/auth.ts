export type UserRole = "BARBER" | "CLIENT";

export interface UserResponseDto {
  id: number;
  email: string;
  nome: string;
  cognome: string;
  telefono?: string;
  ruolo: UserRole;
}

export interface AuthResponseDto {
  accessToken: string;
  user: UserResponseDto;
}
