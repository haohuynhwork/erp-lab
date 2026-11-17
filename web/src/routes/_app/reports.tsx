import { createFileRoute } from "@tanstack/react-router";
import { FileSpreadsheet } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { download } from "@/lib/api";

export const Route = createFileRoute("/_app/reports")({ component: Reports });

const exports = [
  { id: "stock", title: "Stock", description: "On hand, average cost and value per warehouse" },
  { id: "sales-orders", title: "Sales orders", description: "All orders with totals and status" },
  {
    id: "purchase-orders",
    title: "Purchase orders",
    description: "All orders with totals and status",
  },
  { id: "invoices", title: "Invoices", description: "Totals, payments and open balances" },
  { id: "trial-balance", title: "Trial balance", description: "Debits and credits per account" },
];

function Reports() {
  return (
    <div>
      <PageHeader title="Exports" description="Download current data as Excel" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {exports.map((e) => (
          <Card key={e.id}>
            <CardHeader>
              <CardTitle>{e.title}</CardTitle>
              <CardDescription>{e.description}</CardDescription>
            </CardHeader>
            <CardContent>
              <Button
                variant="outline"
                onClick={() =>
                  download(`/reports/exports/${e.id}`).catch(() => toast.error("Download failed"))
                }
              >
                <FileSpreadsheet /> Download .xlsx
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
