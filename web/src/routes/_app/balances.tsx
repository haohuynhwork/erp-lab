import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { api } from "@/lib/api";
import { fmtMoney } from "@/lib/format";

export const Route = createFileRoute("/_app/balances")({ component: Balances });

type Row = { partnerId: number; code: string; name: string; balance: number };

function Balances() {
  const [kind, setKind] = useState("receivable");
  const { data, isLoading } = useQuery({
    queryKey: ["balances", kind],
    queryFn: () => api<Row[]>(`/accounting/balances/${kind}`),
  });
  const total = data?.reduce((s, r) => s + r.balance, 0) ?? 0;
  return (
    <div>
      <PageHeader
        title="Receivables & payables"
        description="Open balances per partner, from the ledger"
        actions={<div className="text-lg font-semibold tabular-nums">{fmtMoney(total)}</div>}
      />
      <Tabs value={kind} onValueChange={setKind} className="mb-3">
        <TabsList>
          <TabsTrigger value="receivable">Customers owe us</TabsTrigger>
          <TabsTrigger value="payable">We owe suppliers</TabsTrigger>
        </TabsList>
      </Tabs>
      <DataTable
        data={data}
        loading={isLoading}
        empty="All settled"
        columns={[
          { accessorKey: "code", header: "Code" },
          { accessorKey: "name", header: "Partner" },
          {
            accessorKey: "balance",
            header: "Balance",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
        ]}
      />
    </div>
  );
}
