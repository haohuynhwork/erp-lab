import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { StatusTabs } from "@/components/status-tabs";
import { Input } from "@/components/ui/input";
import { api, type Page, qs } from "@/lib/api";
import { fmtDateTime } from "@/lib/format";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/audit-log")({ component: AuditLog });

const contexts = ["auth", "masterdata", "inventory", "purchasing", "sales", "accounting"];

function AuditLog() {
  const [q, setQ] = useState("");
  const [username, setUsername] = useState("");
  const [context, setContext] = useState("all");
  const [page, setPage] = useState(0);
  const params = {
    q,
    username: username || undefined,
    context: context === "all" ? undefined : context,
    page,
    size: 50,
  };
  const list = useQuery({
    queryKey: ["audit-log", params],
    queryFn: () => api<Page<Schemas["AuditEntry"]>>(`/audit-log${qs(params)}`),
    placeholderData: keepPreviousData,
  });
  return (
    <div>
      <PageHeader
        title="Audit log"
        description="Every successful command: who, when, what and with which input"
      />
      <div className="mb-3 flex flex-wrap gap-2">
        <Input
          placeholder="Search action or details…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          className="max-w-xs"
        />
        <Input
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className="w-40"
        />
        <StatusTabs
          value={context}
          onChange={(c) => {
            setContext(c);
            setPage(0);
          }}
          statuses={contexts}
          labels={Object.fromEntries(contexts.map((c) => [c, c]))}
        />
      </div>
      <DataTable
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 50, total: list.data.total, onPage: setPage }}
        columns={[
          { accessorKey: "at", header: "When", cell: ({ getValue }) => fmtDateTime(getValue()) },
          { accessorKey: "username", header: "User" },
          { accessorKey: "context", header: "Area" },
          { accessorKey: "action", header: "Action" },
          { accessorKey: "targetId", header: "Record", meta: { align: "right" } },
          {
            accessorKey: "details",
            header: "Details",
            cell: ({ getValue }) => (
              <code
                className="block max-w-xl truncate text-xs text-muted-foreground"
                title={getValue()}
              >
                {getValue()}
              </code>
            ),
          },
        ]}
      />
    </div>
  );
}
