export type PurchaseOrder = {
  id: number;
  number: string;
  supplierId: number;
  supplierName: string;
  warehouseId: number;
  warehouseCode: string;
  orderDate: string;
  expectedDate: string | null;
  status: string;
  total: number;
  note: string | null;
};

export type PurchaseOrderLine = {
  id: number;
  productId: number;
  sku: string;
  productName: string;
  unitCode: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  receivedQuantity: number;
};
