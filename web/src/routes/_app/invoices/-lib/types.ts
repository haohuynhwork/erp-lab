export type Invoice = {
  id: number;
  number: string;
  salesOrderId: number;
  salesOrderNumber: string;
  customerId: number;
  customerName: string;
  issueDate: string;
  dueDate: string;
  taxRate: number;
  subtotal: number;
  taxAmount: number;
  total: number;
  paidAmount: number;
  balance: number;
  status: string;
  overdue: boolean;
};
