import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Download } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, download, qs } from "@/lib/api";
import { fmtMoney } from "@/lib/format";
import { useCan } from "@/lib/session";

export const Route = createFileRoute("/_app/trial-balance")({ component: TrialBalance });

type Row = {
  accountId: number;
  code: string;
  name: string;
  type: string;
  debit: number;
  credit: number;
  balance: number;
};
type Result = { rows: Row[]; totalDebit: number; totalCredit: number };

function TrialBalance() {
  const can = useCan();
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const { data, isLoading } = useQuery({
    queryKey: ["trial-balance", from, to],
    queryFn: () => api<Result>(`/accounting/trial-balance${qs({ from, to })}`),
  });
  const balanced = data && data.totalDebit === data.totalCredit;
  return (
    <div>
      <PageHeader
        title="Trial balance"
        description="Debits and credits per account; balances shown on each account's normal side"
        actions={
          can("reports:read") && (
            <Button variant="outline" onClick={() => download("/reports/exports/trial-balance")}>
              <Download /> Excel
            </Button>
          )
        }
      />
      <div className="mb-3 flex flex-wrap items-end gap-3">
        <div>
          <Label className="mb-1.5">From</Label>
          <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div>
          <Label className="mb-1.5">To</Label>
          <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        {data && (
          <div className="ml-auto flex items-center gap-3 text-sm">
            <span>
              Debit <span className="font-medium tabular-nums">{fmtMoney(data.totalDebit)}</span>
            </span>
            <span>
              Credit <span className="font-medium tabular-nums">{fmtMoney(data.totalCredit)}</span>
            </span>
            {balanced ? (
              <Badge variant="success">balanced</Badge>
            ) : (
              <Badge variant="destructive">unbalanced</Badge>
            )}
          </div>
        )}
      </div>
      <DataTable
        data={data?.rows}
        loading={isLoading}
        columns={[
          { accessorKey: "code", header: "Code" },
          { accessorKey: "name", header: "Account" },
          { accessorKey: "type", header: "Type", cell: ({ getValue }) => getValue().toLowerCase() },
          {
            accessorKey: "debit",
            header: "Debit",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
          {
            accessorKey: "credit",
            header: "Credit",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
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
