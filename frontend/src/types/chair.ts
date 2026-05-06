export interface ChairResponseDto {
  id: number;
  nome: string;
  attiva: boolean;
}

export interface CreateChairRequestDto {
  nome: string;
}

export interface UpdateChairRequestDto {
  nome: string;
  attiva: boolean;
}
