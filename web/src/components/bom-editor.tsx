import { Plus, Trash2 } from "lucide-react";
import { EntitySelect } from "@/components/entity-select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export type BomLine = { componentId: number | null; quantity: number | null };

/** Components and quantities per 1 unit of the product. */
export function BomEditor({
  value,
  onChange,
}: {
  value: BomLine[];
  onChange: (v: BomLine[]) => void;
}) {
  const update = (i: number, patch: Partial<BomLine>) =>
    onChange(value.map((l, j) => (j === i ? { ...l, ...patch } : l)));
  return (
    <div className="space-y-2 rounded-md border p-3">
      {value.length === 0 && <p className="text-sm text-muted-foreground">No components</p>}
      {value.map((line, i) => (
        // biome-ignore lint/suspicious/noArrayIndexKey: rows have no id until saved
        <div key={i} className="flex gap-2">
          <EntitySelect<{ id: number; sku: string; name: string }>
            endpoint="/products?active=true"
            label={(p) => `${p.sku} · ${p.name}`}
            value={line.componentId}
            onChange={(id) => update(i, { componentId: id })}
            className="flex-1"
          />
          <Input
            type="number"
            step="any"
            min="0"
            className="w-28"
            placeholder="Qty"
            value={line.quantity ?? ""}
            onChange={(e) =>
              update(i, { quantity: e.target.value === "" ? null : Number(e.target.value) })
            }
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label="Remove component"
            onClick={() => onChange(value.filter((_, j) => j !== i))}
          >
            <Trash2 />
          </Button>
        </div>
      ))}
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => onChange([...value, { componentId: null, quantity: 1 }])}
      >
        <Plus /> Add component
      </Button>
    </div>
  );
}
