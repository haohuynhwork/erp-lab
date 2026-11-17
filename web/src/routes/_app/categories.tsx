import { createFileRoute } from "@tanstack/react-router";
import { CrudPage } from "@/components/crud-page";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/categories")({ component: Categories });

type Category = Schemas["CategoryView"];

function Categories() {
  return (
    <CrudPage<Category>
      title="Categories"
      description="Product categories"
      endpoint="/categories"
      writePermission="masterdata:write"
      columns={[
        { accessorKey: "name", header: "Name" },
        { accessorKey: "description", header: "Description" },
      ]}
      fields={[
        { name: "name", label: "Name", required: true, wide: true },
        { name: "description", label: "Description", type: "textarea" },
      ]}
      newValues={{ name: "", description: "" }}
    />
  );
}
