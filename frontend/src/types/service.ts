export interface ServiceResponseDto {
  id: number;
  nome: string;
  descrizione: string;
  durata: number;
  prezzo: number;
  attivo: boolean;
}

export interface CreateServiceRequestDto {
  nome: string;
  descrizione: string;
  durata: number;
  prezzo: number;
}

export interface UpdateServiceRequestDto extends Partial<CreateServiceRequestDto> {
  attivo?: boolean;
}
