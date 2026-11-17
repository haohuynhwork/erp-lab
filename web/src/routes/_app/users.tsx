import { createFileRoute } from "@tanstack/react-router";
import { CrudPage } from "@/components/crud-page";
import { Badge } from "@/components/ui/badge";
import { post, put } from "@/lib/api";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/users")({ component: Users });

type User = Schemas["UserView"];

const roles = ["ADMIN", "MANAGER", "SALES", "PURCHASING", "WAREHOUSE", "ACCOUNTANT"].map((r) => ({
  value: r,
  label: r.toLowerCase(),
}));

function Users() {
  return (
    <CrudPage<User>
      title="Users"
      description="Roles decide what each user can see and do. Leave the password empty to keep it."
      endpoint="/users"
      writePermission="users:manage"
      columns={[
        { accessorKey: "username", header: "Username" },
        { accessorKey: "fullName", header: "Name" },
        { accessorKey: "email", header: "Email" },
        {
          accessorKey: "roles",
          header: "Roles",
          cell: ({ row }) => (
            <div className="flex flex-wrap gap-1">
              {row.original.roles.map((r: string) => (
                <Badge key={r} variant="secondary">
                  {r.toLowerCase()}
                </Badge>
              ))}
            </div>
          ),
        },
        {
          accessorKey: "enabled",
          header: "",
          cell: ({ getValue }) => !getValue() && <Badge variant="neutral">disabled</Badge>,
        },
      ]}
      fields={[
        { name: "username", label: "Username", required: true, createOnly: true },
        { name: "fullName", label: "Full name", required: true },
        { name: "email", label: "Email" },
        { name: "password", label: "Password (min. 8 characters)" },
        { name: "roles", label: "Roles", type: "multi", options: roles },
        { name: "enabled", label: "Enabled", type: "checkbox" },
      ]}
      newValues={{ username: "", fullName: "", email: "", password: "", roles: [], enabled: true }}
      toValues={(u) => ({ ...u, email: u.email ?? "", password: "" })}
      toBody={(v) => ({ ...v, email: v.email || null })}
      save={async (id, body) => {
        if (!id) return post("/users", body);
        await put(`/users/${id}`, body);
        if (body.password) await put(`/users/${id}/password`, { password: body.password });
      }}
    />
  );
}
