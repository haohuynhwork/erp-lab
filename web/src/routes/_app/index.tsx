import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { EChart } from "@/components/echart";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { api } from "@/lib/api";
import { fmtMoney, fmtQty } from "@/lib/format";
import type { Schemas } from "@/lib/schemas";
import { useCan } from "@/lib/session";

export const Route = createFileRoute("/_app/")({ component: Dashboard });

type Dashboard = Schemas["Dashboard"];

// Chart colors from the theme (docs/theme.css chart-1..3).
const css = (v: string) => getComputedStyle(document.documentElement).getPropertyValue(v).trim();

function Dashboard() {
  const can = useCan();
  const { data } = useQuery({
    queryKey: ["dashboard"],
    queryFn: () => api<Dashboard>("/reports/dashboard"),
    enabled: can("reports:read"),
  });

  if (!can("reports:read")) {
    return <PageHeader title="Welcome" description="Use the menu to get started." />;
  }
  const k = data?.kpis;
  const change =
    k && k.salesLastMonth > 0
      ? ((k.salesThisMonth - k.salesLastMonth) / k.salesLastMonth) * 100
      : null;

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Sales, cash and stock at a glance" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Kpi
          label="Sales this month"
          value={fmtMoney(k?.salesThisMonth)}
          hint={
            change == null
              ? "excl. VAT"
              : `${change >= 0 ? "+" : ""}${change.toFixed(0)}% vs last month`
          }
        />
        <Kpi
          label="Receivable"
          value={fmtMoney(k?.receivable)}
          hint={
            k && k.overdueReceivable > 0
              ? `${fmtMoney(k.overdueReceivable)} overdue`
              : "nothing overdue"
          }
          warn={!!k && k.overdueReceivable > 0}
          to="/invoices"
        />
        <Kpi label="Payable" value={fmtMoney(k?.payable)} hint="owed to suppliers" to="/balances" />
        <Kpi
          label="Inventory value"
          value={fmtMoney(k?.inventoryValue)}
          hint={k ? `${k.lowStockItems} item(s) below minimum` : ""}
          warn={!!k && k.lowStockItems > 0}
          to="/stock"
        />
        <Kpi label="Open sales orders" value={k?.openSalesOrders ?? ""} to="/sales-orders" />
        <Kpi
          label="Open purchase orders"
          value={k?.openPurchaseOrders ?? ""}
          to="/purchase-orders"
        />
      </div>
      <div className="grid gap-4 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader>
            <CardTitle>Last 12 months</CardTitle>
          </CardHeader>
          <CardContent>
            {data && (
              <EChart
                option={{
                  tooltip: { trigger: "axis" },
                  legend: { bottom: 0 },
                  grid: { left: 60, right: 20, top: 20, bottom: 50 },
                  xAxis: { type: "category", data: data.months.map((m) => m.month) },
                  yAxis: { type: "value" },
                  series: [
                    {
                      name: "Sales",
                      type: "bar",
                      data: data.months.map((m) => m.sales),
                      color: css("--chart-1"),
                    },
                    {
                      name: "Purchases",
                      type: "bar",
                      data: data.months.map((m) => m.purchases),
                      color: css("--chart-2"),
                    },
                    {
                      name: "Gross profit",
                      type: "line",
                      smooth: true,
                      data: data.months.map((m) => m.grossProfit),
                      color: css("--chart-3"),
                    },
                  ],
                }}
              />
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Top products (90 days)</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data?.topProducts.length === 0 && (
              <p className="text-sm text-muted-foreground">No sales yet</p>
            )}
            {data?.topProducts.map((p) => (
              <div key={p.productId} className="flex items-center justify-between gap-2 text-sm">
                <div className="min-w-0">
                  <div className="truncate font-medium">{p.name}</div>
                  <div className="text-xs text-muted-foreground">
                    {p.sku} · {fmtQty(p.quantity)} sold
                  </div>
                </div>
                <div className="tabular-nums">{fmtMoney(p.revenue)}</div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Kpi({
  label,
  value,
  hint,
  warn,
  to,
}: {
  label: string;
  value: string | number;
  hint?: string;
  warn?: boolean;
  to?: string;
}) {
  const body = (
    <Card className="h-full transition-colors hover:bg-accent/40">
      <CardContent>
        <div className="text-sm text-muted-foreground">{label}</div>
        <div className="mt-1 text-2xl font-semibold tabular-nums">{value}</div>
        {hint && (
          <div
            className={
              warn ? "mt-1 text-xs text-destructive" : "mt-1 text-xs text-muted-foreground"
            }
          >
            {hint}
          </div>
        )}
      </CardContent>
    </Card>
  );
  return to ? <Link to={to}>{body}</Link> : body;
}
