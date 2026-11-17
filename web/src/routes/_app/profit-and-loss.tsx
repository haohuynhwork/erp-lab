import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { PageHeader } from "@/components/page-header";
import { Statement, StatementSection } from "@/components/statement";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, qs } from "@/lib/api";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/profit-and-loss")({ component: ProfitAndLoss });

const firstOfYear = () => `${new Date().getFullYear()}-01-01`;

function ProfitAndLoss() {
  const [from, setFrom] = useState(firstOfYear());
  const [to, setTo] = useState("");
  const { data } = useQuery({
    queryKey: ["profit-and-loss", from, to],
    queryFn: () => api<Schemas["ProfitAndLoss"]>(`/accounting/profit-and-loss${qs({ from, to })}`),
  });
  return (
    <div>
      <PageHeader title="Profit and loss" description="Revenue and expenses posted in the period" />
      <div className="mb-4 flex gap-3">
        <div>
          <Label className="mb-1.5">From</Label>
          <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div>
          <Label className="mb-1.5">To</Label>
          <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>
      {data && (
        <Statement>
          <StatementSection title="Revenue" lines={data.revenue} total={data.totalRevenue} />
          <StatementSection title="Expenses" lines={data.expenses} total={data.totalExpenses} />
          <StatementSection title="Net profit" total={data.netProfit} strong />
        </Statement>
      )}
    </div>
  );
}
