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
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <Badge variant={variants[status] ?? "secondary"}>
      {status.replace("_", " ").toLowerCase()}
    </Badge>
  );
}
