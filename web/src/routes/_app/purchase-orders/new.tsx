import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { emptyOrder, OrderForm, orderBody } from "@/components/order-form";
import { PageHeader } from "@/components/page-header";
import { post } from "@/lib/api";
import { useAction } from "@/lib/use-action";

export const Route = createFileRoute("/_app/purchase-orders/new")({ component: NewPurchaseOrder });

function NewPurchaseOrder() {
  const navigate = useNavigate();
  const create = useAction((body: unknown) => post<{ id: number }>("/purchase-orders", body), {
    success: "Purchase order created",
    invalidate: [["purchase-orders"]],
    onSuccess: ({ id }) => navigate({ to: "/purchase-orders/$id", params: { id: String(id) } }),
  });
  return (
    <div>
      <PageHeader
        title="New purchase order"
        description="Prices default to each product's purchase price"
      />
      <OrderForm
        kind="purchase"
        initial={emptyOrder()}
        saving={create.isPending}
        onSave={(o) => create.mutate(orderBody("purchase", o))}
      />
    </div>
  );
}
