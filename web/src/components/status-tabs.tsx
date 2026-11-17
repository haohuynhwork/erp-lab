import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

/** Document status filter: all / draft / confirmed / done / cancelled (or custom). */
export function StatusTabs({
  value,
  onChange,
  statuses = ["DRAFT", "CONFIRMED", "DONE", "CANCELLED"],
}: {
  value: string;
  onChange: (v: string) => void;
  statuses?: string[];
}) {
  return (
    <Tabs value={value} onValueChange={onChange}>
      <TabsList>
        <TabsTrigger value="all">All</TabsTrigger>
        {statuses.map((s) => (
          <TabsTrigger key={s} value={s}>
            {s.replace("_", " ").toLowerCase()}
          </TabsTrigger>
        ))}
      </TabsList>
    </Tabs>
  );
}
