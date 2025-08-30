export type Ticket = {
  itemId: string;
  itemName: string;
  totalAmount: number;
};

export type TExchange = {
  quantity: number;
  amount: number;
};

export type THistory = {
  confirmedAt: string;
  user_ticket: number;
  amount: number;
};
