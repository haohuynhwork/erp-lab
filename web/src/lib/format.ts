const money = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const qty = new Intl.NumberFormat(undefined, { maximumFractionDigits: 4 });

export const fmtMoney = (v: number | null | undefined) => (v == null ? "" : money.format(v));
export const fmtQty = (v: number | null | undefined) => (v == null ? "" : qty.format(v));
export const fmtDate = (v: string | null | undefined) => (v ? v.slice(0, 10) : "");
export const fmtDateTime = (v: string | null | undefined) =>
  v ? new Date(v).toLocaleString(undefined, { dateStyle: "short", timeStyle: "short" }) : "";
export const today = () => new Date().toISOString().slice(0, 10);
