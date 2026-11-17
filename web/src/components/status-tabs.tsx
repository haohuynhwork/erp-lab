import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

/** Document status filter: all / draft / confirmed / done / cancelled (or custom). */
export function StatusTabs({
  value,
  onChange,
  statuses = ["DRAFT", "CONFIRMED", "DONE", "CANCELLED"],
  labels = {},
}: {
  value: string;
  onChange: (v: string) => void;
  statuses?: string[];
  labels?: Record<string, string>;
}) {
  return (
    <Tabs value={value} onValueChange={onChange}>
      <TabsList>
        <TabsTrigger value="all">All</TabsTrigger>
        {statuses.map((s) => (
          <TabsTrigger key={s} value={s}>
            {labels[s] ?? s.replace("_", " ").toLowerCase()}
          </TabsTrigger>
        ))}
      </TabsList>
    </Tabs>
  );
}
