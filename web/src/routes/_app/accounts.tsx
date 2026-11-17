import { createFileRoute } from "@tanstack/react-router";
import { CrudPage } from "@/components/crud-page";
import { Badge } from "@/components/ui/badge";
import { post, put } from "@/lib/api";

export const Route = createFileRoute("/_app/accounts")({ component: Accounts });

type Account = { id: number; code: string; name: string; type: string; active: boolean };

const types = ["ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"].map((t) => ({
  value: t,
  label: t.toLowerCase(),
}));

function Accounts() {
  return (
    <CrudPage<Account>
      title="Chart of accounts"
      description="Accounts 1110–5210 are used by automatic postings"
      endpoint="/accounting/accounts"
      writePermission="accounting:write"
      canDelete={false}
      columns={[
        { accessorKey: "code", header: "Code" },
        { accessorKey: "name", header: "Name" },
        { accessorKey: "type", header: "Type", cell: ({ getValue }) => getValue().toLowerCase() },
        {
          accessorKey: "active",
          header: "",
          cell: ({ getValue }) => !getValue() && <Badge variant="neutral">inactive</Badge>,
        },
      ]}
      fields={[
        { name: "code", label: "Code", required: true, createOnly: true },
        {
          name: "type",
          label: "Type",
          type: "select",
          options: types,
          required: true,
          createOnly: true,
        },
        { name: "name", label: "Name", required: true, wide: true },
        { name: "active", label: "Active", type: "checkbox" },
      ]}
      newValues={{ code: "", type: "EXPENSE", name: "", active: true }}
      save={(id, body) =>
        id
          ? put(`/accounting/accounts/${id}`, { name: body.name, active: body.active })
          : post("/accounting/accounts", body)
      }
    />
  );
}
