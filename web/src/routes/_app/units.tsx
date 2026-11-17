import { createFileRoute } from "@tanstack/react-router";
import { CrudPage } from "@/components/crud-page";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/units")({ component: Units });

type Unit = Schemas["UnitView"];

function Units() {
  return (
    <CrudPage<Unit>
      title="Units"
      description="Units of measure for products"
      endpoint="/units"
      writePermission="masterdata:write"
      columns={[
        { accessorKey: "code", header: "Code" },
        { accessorKey: "name", header: "Name" },
      ]}
      fields={[
        { name: "code", label: "Code", required: true },
        { name: "name", label: "Name", required: true },
      ]}
      newValues={{ code: "", name: "" }}
    />
  );
}
