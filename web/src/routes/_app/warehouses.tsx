import { createFileRoute } from "@tanstack/react-router";
import { CrudPage } from "@/components/crud-page";
import { Badge } from "@/components/ui/badge";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/warehouses")({ component: Warehouses });

type Warehouse = Schemas["WarehouseView"];

function Warehouses() {
  return (
    <CrudPage<Warehouse>
      title="Warehouses"
      endpoint="/warehouses"
      writePermission="masterdata:write"
      columns={[
        { accessorKey: "code", header: "Code" },
        { accessorKey: "name", header: "Name" },
        { accessorKey: "address", header: "Address" },
        {
          accessorKey: "active",
          header: "Status",
          cell: ({ row }) =>
            row.original.active ? (
              <Badge variant="success">active</Badge>
            ) : (
              <Badge variant="neutral">inactive</Badge>
            ),
        },
      ]}
      fields={[
        { name: "code", label: "Code", required: true, createOnly: true },
        { name: "name", label: "Name", required: true },
        { name: "address", label: "Address", type: "textarea" },
        { name: "active", label: "Active", type: "checkbox" },
      ]}
      newValues={{ code: "", name: "", address: "", active: true }}
    />
  );
}
