import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { PageHeader } from "@/components/page-header";
import { Statement, StatementSection } from "@/components/statement";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, qs } from "@/lib/api";
import { today } from "@/lib/format";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/balance-sheet")({ component: BalanceSheet });

function BalanceSheet() {
  const [asOf, setAsOf] = useState(today());
  const { data } = useQuery({
    queryKey: ["balance-sheet", asOf],
    queryFn: () => api<Schemas["BalanceSheet"]>(`/accounting/balance-sheet${qs({ asOf })}`),
  });
  return (
    <div>
      <PageHeader
        title="Balance sheet"
        description="Assets = liabilities + equity; profit not yet closed into equity shows as current earnings"
        actions={
          data &&
          (data.balanced ? (
            <Badge variant="success">balanced</Badge>
          ) : (
            <Badge variant="destructive">unbalanced</Badge>
          ))
        }
      />
      <div className="mb-4">
        <Label className="mb-1.5">As of</Label>
        <Input
          type="date"
          value={asOf}
          onChange={(e) => setAsOf(e.target.value)}
          className="w-44"
        />
      </div>
      {data && (
        <div className="grid gap-4 lg:grid-cols-2">
          <Statement>
            <StatementSection title="Assets" lines={data.assets} total={data.totalAssets} strong />
          </Statement>
          <Statement>
            <StatementSection
              title="Liabilities"
              lines={data.liabilities}
              total={data.totalLiabilities}
            />
            <StatementSection
              title="Equity"
              lines={[
                ...data.equity,
                { accountId: 0, code: "", name: "Current earnings", amount: data.currentEarnings },
              ]}
              total={data.totalEquity}
            />
            <StatementSection
              title="Liabilities + equity"
              total={data.totalLiabilities + data.totalEquity}
              strong
            />
          </Statement>
        </div>
      )}
    </div>
  );
}
