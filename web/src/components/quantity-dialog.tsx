import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { fmtQty, today } from "@/lib/format";

export type QtyLine = { id: number; sku: string; productName: string; remaining: number };

/** Enter quantities per order line (goods receipt, delivery); prefilled with what is left. */
export function QuantityDialog({
  title,
  description,
  lines,
  open,
  onOpenChange,
  busy,
  onSubmit,
}: {
  title: string;
  description: string;
  lines: QtyLine[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
  busy: boolean;
  onSubmit: (date: string, lines: { lineId: number; quantity: number }[]) => void;
}) {
  const [qty, setQty] = useState<Record<number, number>>({});
  const [date, setDate] = useState(today());
  const value = (l: QtyLine) => qty[l.id] ?? l.remaining;
  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        setQty({});
        onOpenChange(o);
      }}
    >
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div className="w-44">
            <Label className="mb-1.5">Date</Label>
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          {lines
            .filter((l) => l.remaining > 0)
            .map((l) => (
              <div key={l.id} className="flex items-center gap-3">
                <div className="min-w-0 flex-1 text-sm">
                  <div className="truncate font-medium">{l.productName}</div>
                  <div className="text-xs text-muted-foreground">
                    {l.sku} · {fmtQty(l.remaining)} left
                  </div>
                </div>
                <Input
                  type="number"
                  step="any"
                  min={0}
                  max={l.remaining}
                  className="w-28 text-right"
                  value={value(l)}
                  onChange={(e) => setQty({ ...qty, [l.id]: Number(e.target.value) })}
                />
              </div>
            ))}
        </div>
        <DialogFooter>
          <Button
            disabled={busy}
            onClick={() =>
              onSubmit(
                date,
                lines
                  .filter((l) => l.remaining > 0)
                  .map((l) => ({ lineId: l.id, quantity: value(l) })),
              )
            }
          >
            {busy ? "Saving…" : "Confirm"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
