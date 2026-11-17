import type { ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { fmtMoney } from "@/lib/format";

export function Statement({ children }: { children: ReactNode }) {
  return (
    <Card className="max-w-2xl">
      <CardContent className="space-y-6">{children}</CardContent>
    </Card>
  );
}

/** A financial statement section: account lines and a total. */
export function StatementSection({
  title,
  lines = [],
  total,
  strong,
}: {
  title: string;
  lines?: { accountId: number; code: string; name: string; amount: number }[];
  total: number;
  strong?: boolean;
}) {
  return (
    <div className="text-sm">
      {lines.length > 0 && <div className="mb-2 font-medium">{title}</div>}
      {lines.map((l) => (
        <div key={`${l.accountId}-${l.name}`} className="flex justify-between py-0.5 pl-4">
          <span>
            {l.code && <span className="mr-2 text-muted-foreground">{l.code}</span>}
            {l.name}
          </span>
          <span className="tabular-nums">{fmtMoney(l.amount)}</span>
        </div>
      ))}
      <div
        className={`mt-1 flex justify-between border-t pt-1 ${strong ? "text-base font-semibold" : "font-medium"}`}
      >
        <span>{lines.length > 0 ? `Total ${title.toLowerCase()}` : title}</span>
        <span className="tabular-nums">{fmtMoney(total)}</span>
      </div>
    </div>
  );
}
