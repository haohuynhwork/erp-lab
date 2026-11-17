import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus, Trash2, Undo2 } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { EntitySelect } from "@/components/entity-select";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
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
import { api, type Page, post, qs } from "@/lib/api";
import { fmtDate, fmtMoney, today } from "@/lib/format";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";

export const Route = createFileRoute("/_app/journal")({ component: Journal });

type Entry = {
  id: number;
  number: string;
  entryDate: string;
  description: string;
  sourceType: string;
  sourceNumber: string | null;
  amount: number;
  reversed: boolean;
  reversalOfId: number | null;
};
type Line = {
  accountCode: string;
  accountName: string;
  partnerName: string | null;
  debit: number;
  credit: number;
  memo: string | null;
};
type AccountOption = { id: number; code: string; name: string };

function Journal() {
  const can = useCan();
  const [q, setQ] = useState("");
  const [accountId, setAccountId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState<Entry | null>(null);
  const [creating, setCreating] = useState(false);
  const params = { q, accountId, page, size: 50 };
  const list = useQuery({
    queryKey: ["journal", params],
    queryFn: () => api<Page<Entry>>(`/accounting/journal-entries${qs(params)}`),
    placeholderData: keepPreviousData,
  });

  return (
    <div>
      <PageHeader
        title="Journal"
        description="All postings. Sales, purchasing and inventory post automatically; entries are corrected by reversal."
        actions={
          can("accounting:write") && (
            <Button onClick={() => setCreating(true)}>
              <Plus /> Manual entry
            </Button>
          )
        }
      />
      <div className="mb-3 flex flex-wrap gap-3">
        <Input
          placeholder="Search number, description, document…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          className="max-w-xs"
        />
        <EntitySelect<AccountOption>
          endpoint="/accounting/accounts"
          label={(a) => `${a.code} · ${a.name}`}
          value={accountId}
          onChange={(v) => {
            setAccountId(v);
            setPage(0);
          }}
          placeholder="All accounts"
          allowEmpty
          className="w-64"
        />
      </div>
      <DataTable
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 50, total: list.data.total, onPage: setPage }}
        onRowClick={setOpen}
        columns={[
          { accessorKey: "number", header: "Number" },
          { accessorKey: "entryDate", header: "Date", cell: ({ getValue }) => fmtDate(getValue()) },
          { accessorKey: "description", header: "Description" },
          {
            accessorKey: "sourceType",
            header: "Source",
            cell: ({ getValue }) => getValue().replace("_", " ").toLowerCase(),
          },
          { accessorKey: "sourceNumber", header: "Document" },
          {
            accessorKey: "amount",
            header: "Amount",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
          {
            id: "flags",
            header: "",
            cell: ({ row }) =>
              row.original.reversed ? (
                <Badge variant="neutral">reversed</Badge>
              ) : row.original.reversalOfId ? (
                <Badge variant="secondary">reversal</Badge>
              ) : null,
          },
        ]}
      />
      {open && <EntryDialog entry={open} onClose={() => setOpen(null)} />}
      {creating && <ManualEntryDialog onClose={() => setCreating(false)} />}
    </div>
  );
}

function EntryDialog({ entry, onClose }: { entry: Entry; onClose: () => void }) {
  const can = useCan();
  const { data } = useQuery({
    queryKey: ["journal", "entry", entry.id],
    queryFn: () => api<{ entry: Entry; lines: Line[] }>(`/accounting/journal-entries/${entry.id}`),
  });
  const reverse = useAction(() => post(`/accounting/journal-entries/${entry.id}/reverse`), {
    success: "Reversal posted",
    invalidate: [["journal"], ["trial-balance"], ["balances"]],
    onSuccess: onClose,
  });
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {entry.number} · {fmtDate(entry.entryDate)}
          </DialogTitle>
          <DialogDescription>{entry.description}</DialogDescription>
        </DialogHeader>
        <DataTable
          data={data?.lines}
          columns={[
            {
              id: "account",
              header: "Account",
              cell: ({ row }) => `${row.original.accountCode} ${row.original.accountName}`,
            },
            { accessorKey: "partnerName", header: "Partner" },
            {
              accessorKey: "debit",
              header: "Debit",
              meta: { align: "right" },
              cell: ({ getValue }) => (getValue() ? fmtMoney(getValue()) : ""),
            },
            {
              accessorKey: "credit",
              header: "Credit",
              meta: { align: "right" },
              cell: ({ getValue }) => (getValue() ? fmtMoney(getValue()) : ""),
            },
          ]}
        />
        {can("accounting:write") && !entry.reversed && !entry.reversalOfId && (
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => window.confirm("Post a reversing entry?") && reverse.mutate(undefined)}
            >
              <Undo2 /> Reverse
            </Button>
          </DialogFooter>
        )}
      </DialogContent>
    </Dialog>
  );
}

type Draft = { accountId: number | null; debit: number; credit: number; memo: string };

function ManualEntryDialog({ onClose }: { onClose: () => void }) {
  const [date, setDate] = useState(today());
  const [description, setDescription] = useState("");
  const [lines, setLines] = useState<Draft[]>([
    { accountId: null, debit: 0, credit: 0, memo: "" },
    { accountId: null, debit: 0, credit: 0, memo: "" },
  ]);
  const setLine = (i: number, patch: Partial<Draft>) =>
    setLines(lines.map((l, j) => (j === i ? { ...l, ...patch } : l)));
  const debit = lines.reduce((s, l) => s + (l.debit || 0), 0);
  const credit = lines.reduce((s, l) => s + (l.credit || 0), 0);
  const ok =
    description && debit > 0 && Math.abs(debit - credit) < 0.005 && lines.every((l) => l.accountId);
  const save = useAction(() => post("/accounting/journal-entries", { date, description, lines }), {
    success: "Entry posted",
    invalidate: [["journal"], ["trial-balance"]],
    onSuccess: onClose,
  });
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>Manual journal entry</DialogTitle>
          <DialogDescription>
            Debits must equal credits. Each line is either a debit or a credit.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-3 sm:grid-cols-[10rem_1fr]">
          <div>
            <Label className="mb-1.5">Date</Label>
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div>
            <Label className="mb-1.5">Description</Label>
            <Input value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
        </div>
        <div className="space-y-2">
          {lines.map((l, i) => (
            // biome-ignore lint/suspicious/noArrayIndexKey: draft lines have no id
            <div key={i} className="grid grid-cols-[1fr_7rem_7rem_2.5rem] gap-2">
              <EntitySelect<AccountOption>
                endpoint="/accounting/accounts"
                label={(a) => `${a.code} · ${a.name}`}
                value={l.accountId}
                onChange={(v) => setLine(i, { accountId: v })}
              />
              <Input
                type="number"
                step="any"
                min={0}
                placeholder="Debit"
                className="text-right"
                value={l.debit || ""}
                onChange={(e) => setLine(i, { debit: Number(e.target.value), credit: 0 })}
              />
              <Input
                type="number"
                step="any"
                min={0}
                placeholder="Credit"
                className="text-right"
                value={l.credit || ""}
                onChange={(e) => setLine(i, { credit: Number(e.target.value), debit: 0 })}
              />
              <Button
                variant="ghost"
                size="icon"
                aria-label="Remove line"
                onClick={() => setLines(lines.filter((_, j) => j !== i))}
              >
                <Trash2 />
              </Button>
            </div>
          ))}
          <div className="flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setLines([...lines, { accountId: null, debit: 0, credit: 0, memo: "" }])
              }
            >
              <Plus /> Add line
            </Button>
            <span className="text-sm tabular-nums">
              {fmtMoney(debit)} / {fmtMoney(credit)}
            </span>
          </div>
        </div>
        <DialogFooter>
          <Button disabled={!ok || save.isPending} onClick={() => save.mutate(undefined)}>
            Post entry
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
