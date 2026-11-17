export type SalesOrder = {
  id: number;
  number: string;
  customerId: number;
  customerName: string;
  warehouseId: number;
  warehouseCode: string;
  orderDate: string;
  deliveryDate: string | null;
  status: string;
  taxRate: number;
  subtotal: number;
  taxAmount: number;
  total: number;
  note: string | null;
};

export type SalesOrderLine = {
  id: number;
  productId: number;
  sku: string;
  productName: string;
  unitCode: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  deliveredQuantity: number;
  invoicedQuantity: number;
};

export type InvoiceRef = {
  id: number;
  number: string;
  issueDate: string;
  total: number;
  status: string;
};
