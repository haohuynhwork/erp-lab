import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { CrudPage } from "@/components/crud-page";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { api } from "@/lib/api";
import { fmtMoney, fmtQty } from "@/lib/format";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/products")({ component: Products });

type Product = Schemas["ProductView"];

const types = [
  { value: "RAW", label: "Raw material" },
  { value: "SEMI_FINISHED", label: "Semi-finished" },
  { value: "FINISHED", label: "Finished good" },
  { value: "GOODS", label: "Trading goods" },
  { value: "SERVICE", label: "Service" },
];

function Products() {
  const [type, setType] = useState("all");
  return (
    <CrudPage<Product>
      title="Products"
      description="Items you buy, make and sell. Finished and semi-finished products have a bill of materials."
      endpoint="/products"
      writePermission="masterdata:write"
      extraParams={{ type: type === "all" ? undefined : type }}
      filters={
        <Select value={type} onValueChange={setType}>
          <SelectTrigger className="w-44">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All types</SelectItem>
            {types.map((t) => (
              <SelectItem key={t.value} value={t.value}>
                {t.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      }
      columns={[
        { accessorKey: "sku", header: "SKU" },
        { accessorKey: "name", header: "Name" },
        {
          accessorKey: "type",
          header: "Type",
          cell: ({ getValue }) => types.find((t) => t.value === getValue())?.label,
        },
        { accessorKey: "categoryName", header: "Category" },
        { accessorKey: "unitCode", header: "Unit" },
        {
          accessorKey: "purchasePrice",
          header: "Cost",
          meta: { align: "right" },
          cell: ({ getValue }) => fmtMoney(getValue()),
        },
        {
          accessorKey: "salePrice",
          header: "Price",
          meta: { align: "right" },
          cell: ({ getValue }) => fmtMoney(getValue()),
        },
        {
          accessorKey: "minStock",
          header: "Min stock",
          meta: { align: "right" },
          cell: ({ getValue }) => fmtQty(getValue()),
        },
        {
          accessorKey: "active",
          header: "",
          cell: ({ getValue }) => !getValue() && <Badge variant="neutral">inactive</Badge>,
        },
      ]}
      fields={[
        { name: "sku", label: "SKU", required: true, createOnly: true },
        { name: "name", label: "Name", required: true },
        {
          name: "type",
          label: "Type",
          type: "select",
          options: types,
          required: true,
          createOnly: true,
        },
        {
          name: "unitId",
          label: "Unit",
          type: "entity",
          endpoint: "/units",
          optionLabel: (u) => `${u.code} · ${u.name}`,
          required: true,
        },
        {
          name: "categoryId",
          label: "Category",
          type: "entity",
          endpoint: "/categories",
          allowEmpty: true,
        },
        { name: "minStock", label: "Minimum stock", type: "number" },
        { name: "purchasePrice", label: "Purchase price", type: "number" },
        { name: "salePrice", label: "Sale price", type: "number" },
        { name: "active", label: "Active", type: "checkbox" },
        { name: "bom", label: "Bill of materials (per 1 unit)", type: "bom" },
      ]}
      newValues={{
        sku: "",
        name: "",
        type: "RAW",
        unitId: null,
        categoryId: null,
        minStock: 0,
        purchasePrice: 0,
        salePrice: 0,
        active: true,
        bom: [],
      }}
      loadValues={async (p) => {
        const d = await api<Schemas["ProductDetail"]>(`/products/${p.id}`);
        return {
          ...d.product,
          bom: d.bom.map((l) => ({ componentId: l.componentId, quantity: l.quantity })),
        };
      }}
      toBody={(v) => ({
        ...v,
        bom: (v.bom ?? []).filter((l: { componentId: number | null }) => l.componentId != null),
      })}
    />
  );
}
