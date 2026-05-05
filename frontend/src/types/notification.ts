export interface NotificationResponseDto {
  id: number;
  titolo: string;
  messaggio: string;
  tipo: string;
  letta: boolean;
  createdAt: string;
}

export interface NotificationPushDto {
  id: number;
  titolo: string;
  messaggio: string;
}
