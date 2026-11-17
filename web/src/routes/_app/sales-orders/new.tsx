import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { emptyOrder, OrderForm, orderBody } from "@/components/order-form";
import { PageHeader } from "@/components/page-header";
import { post } from "@/lib/api";
import { useAction } from "@/lib/use-action";

export const Route = createFileRoute("/_app/sales-orders/new")({ component: NewSalesOrder });

function NewSalesOrder() {
  const navigate = useNavigate();
  const create = useAction((body: unknown) => post<{ id: number }>("/sales-orders", body), {
    success: "Sales order created",
    invalidate: [["sales-orders"]],
    onSuccess: ({ id }) => navigate({ to: "/sales-orders/$id", params: { id: String(id) } }),
  });
  return (
    <div>
      <PageHeader
        title="New sales order"
        description="Prices default to each product's sale price"
      />
      <OrderForm
        kind="sales"
        initial={emptyOrder()}
        saving={create.isPending}
        onSave={(o) => create.mutate(orderBody("sales", o))}
      />
    </div>
  );
}
