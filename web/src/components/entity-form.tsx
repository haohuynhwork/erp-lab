import { useForm } from "@tanstack/react-form";
import type { ReactNode } from "react";
import { BomEditor } from "@/components/bom-editor";
import { EntitySelect } from "@/components/entity-select";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

export type FieldDef = {
  name: string;
  label: string;
  type?:
    | "text"
    | "number"
    | "date"
    | "textarea"
    | "checkbox"
    | "select"
    | "entity"
    | "multi"
    | "bom";
  required?: boolean;
  /** Read-only when editing an existing record (codes, SKUs). */
  createOnly?: boolean;
  options?: { value: string; label: string }[];
  /** For type "entity": list endpoint and option label. */
  endpoint?: string;
  // biome-ignore lint/suspicious/noExplicitAny: rows of any list endpoint
  optionLabel?: (row: any) => string;
  allowEmpty?: boolean;
  placeholder?: string;
  /** Spans both columns. */
  wide?: boolean;
};

// biome-ignore lint/suspicious/noExplicitAny: form values are dynamic per field config
type Values = Record<string, any>;

/** Config-driven form (TanStack Form) for master data dialogs. */
export function EntityForm({
  fields,
  defaultValues,
  editing,
  onSubmit,
  submitting,
  footer,
}: {
  fields: FieldDef[];
  defaultValues: Values;
  editing: boolean;
  onSubmit: (values: Values) => void;
  submitting?: boolean;
  footer?: ReactNode;
}) {
  const form = useForm({
    defaultValues,
    onSubmit: ({ value }) => onSubmit(value),
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        form.handleSubmit();
      }}
      className="space-y-4"
    >
      <div className="grid gap-4 sm:grid-cols-2">
        {fields.map((f) => (
          <form.Field
            key={f.name}
            name={f.name}
            validators={{
              onSubmit: ({ value }) =>
                f.required && (value === "" || value == null) ? "Required" : undefined,
            }}
          >
            {(field) => {
              const id = `f-${f.name}`;
              const disabled = editing && f.createOnly;
              const value = field.state.value;
              const set = (v: unknown) => field.handleChange(v as never);
              let input: ReactNode;
              switch (f.type ?? "text") {
                case "checkbox":
                  return (
                    <div className="flex items-center gap-2 self-end pb-2">
                      <Checkbox
                        id={id}
                        checked={!!value}
                        onCheckedChange={(c) => set(c === true)}
                      />
                      <Label htmlFor={id}>{f.label}</Label>
                    </div>
                  );
                case "textarea":
                  input = (
                    <Textarea id={id} value={value ?? ""} onChange={(e) => set(e.target.value)} />
                  );
                  break;
                case "number":
                  input = (
                    <Input
                      id={id}
                      type="number"
                      step="any"
                      value={value ?? ""}
                      disabled={disabled}
                      onChange={(e) => set(e.target.value === "" ? null : Number(e.target.value))}
                    />
                  );
                  break;
                case "select":
                  input = (
                    <Select value={value ?? ""} onValueChange={set} disabled={disabled}>
                      <SelectTrigger id={id} className="w-full">
                        <SelectValue placeholder={f.placeholder ?? "Select…"} />
                      </SelectTrigger>
                      <SelectContent>
                        {f.options?.map((o) => (
                          <SelectItem key={o.value} value={o.value}>
                            {o.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  );
                  break;
                case "multi":
                  input = (
                    <div className="flex flex-wrap gap-3 pt-1">
                      {f.options?.map((o) => {
                        const list: string[] = value ?? [];
                        return (
                          <div key={o.value} className="flex items-center gap-1.5 text-sm">
                            <Checkbox
                              id={`${id}-${o.value}`}
                              checked={list.includes(o.value)}
                              onCheckedChange={(c) =>
                                set(c ? [...list, o.value] : list.filter((x) => x !== o.value))
                              }
                            />
                            <Label htmlFor={`${id}-${o.value}`} className="font-normal">
                              {o.label}
                            </Label>
                          </div>
                        );
                      })}
                    </div>
                  );
                  break;
                case "bom":
                  input = <BomEditor value={value ?? []} onChange={set} />;
                  break;
                case "entity":
                  input = (
                    <EntitySelect
                      id={id}
                      endpoint={f.endpoint ?? ""}
                      label={f.optionLabel ?? ((r) => r.name)}
                      value={value}
                      onChange={set}
                      allowEmpty={f.allowEmpty}
                    />
                  );
                  break;
                default:
                  input = (
                    <Input
                      id={id}
                      type={f.type === "date" ? "date" : "text"}
                      value={value ?? ""}
                      disabled={disabled}
                      placeholder={f.placeholder}
                      onChange={(e) => set(e.target.value)}
                    />
                  );
              }
              return (
                <div
                  className={
                    f.wide || f.type === "textarea" || f.type === "multi" || f.type === "bom"
                      ? "sm:col-span-2"
                      : ""
                  }
                >
                  <Label htmlFor={id} className="mb-1.5">
                    {f.label}
                    {f.required && <span className="text-destructive"> *</span>}
                  </Label>
                  {input}
                  {field.state.meta.errors.length > 0 && (
                    <p className="mt-1 text-xs text-destructive">
                      {field.state.meta.errors.join(", ")}
                    </p>
                  )}
                </div>
              );
            }}
          </form.Field>
        ))}
      </div>
      <div className="flex items-center justify-between gap-2 pt-2">
        <div>{footer}</div>
        <Button type="submit" disabled={submitting}>
          {submitting ? "Saving…" : "Save"}
        </Button>
      </div>
    </form>
  );
}
