import { Badge } from "@/components/ui/badge";

const variants: Record<
  string,
  "neutral" | "info" | "success" | "warning" | "destructive" | "secondary"
> = {
  DRAFT: "secondary",
  CONFIRMED: "info",
  DONE: "success",
  CANCELLED: "neutral",
  ISSUED: "info",
  PARTIALLY_PAID: "warning",
  PAID: "success",
  OVERDUE: "destructive",
  OPEN: "info",
  CREDITED: "neutral",
  QUOTATION: "secondary",
};

/** Document status; sales drafts are shown as quotations. */
export function StatusBadge({ status, sales }: { status: string; sales?: boolean }) {
  const shown = sales && status === "DRAFT" ? "QUOTATION" : status;
  return (
    <Badge variant={variants[shown] ?? "secondary"}>{shown.replace("_", " ").toLowerCase()}</Badge>
  );
}
