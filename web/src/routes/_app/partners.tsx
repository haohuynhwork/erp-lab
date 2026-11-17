import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { CrudPage } from "@/components/crud-page";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

export const Route = createFileRoute("/_app/partners")({ component: Partners });

type Partner = {
  id: number;
  code: string;
  name: string;
  customer: boolean;
  supplier: boolean;
  taxCode: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  paymentTermDays: number;
  active: boolean;
};

function Partners() {
  const [role, setRole] = useState("all");
  return (
    <CrudPage<Partner>
      title="Customers & suppliers"
      description="A partner can be a customer, a supplier or both"
      endpoint="/partners"
      writePermission="masterdata:write"
      extraParams={{ role: role === "all" ? undefined : role }}
      filters={
        <Tabs value={role} onValueChange={setRole}>
          <TabsList>
            <TabsTrigger value="all">All</TabsTrigger>
            <TabsTrigger value="customer">Customers</TabsTrigger>
            <TabsTrigger value="supplier">Suppliers</TabsTrigger>
          </TabsList>
        </Tabs>
      }
      columns={[
        { accessorKey: "code", header: "Code" },
        { accessorKey: "name", header: "Name" },
        {
          id: "role",
          header: "Role",
          cell: ({ row }) => (
            <div className="flex gap-1">
              {row.original.customer && <Badge variant="info">customer</Badge>}
              {row.original.supplier && <Badge variant="warning">supplier</Badge>}
            </div>
          ),
        },
        { accessorKey: "email", header: "Email" },
        { accessorKey: "phone", header: "Phone" },
        { accessorKey: "paymentTermDays", header: "Terms (days)", meta: { align: "right" } },
      ]}
      fields={[
        { name: "code", label: "Code", required: true, createOnly: true },
        { name: "name", label: "Name", required: true },
        { name: "customer", label: "Customer", type: "checkbox" },
        { name: "supplier", label: "Supplier", type: "checkbox" },
        { name: "taxCode", label: "Tax code" },
        { name: "paymentTermDays", label: "Payment terms (days)", type: "number" },
        { name: "email", label: "Email" },
        { name: "phone", label: "Phone" },
        { name: "address", label: "Address", type: "textarea" },
        { name: "active", label: "Active", type: "checkbox" },
      ]}
      newValues={{
        code: "",
        name: "",
        customer: role !== "supplier",
        supplier: role === "supplier",
        taxCode: "",
        paymentTermDays: 30,
        email: "",
        phone: "",
        address: "",
        active: true,
      }}
      toBody={(v) => ({ ...v, email: v.email || null })}
    />
  );
}
