import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createFileRoute,
  Link,
  Outlet,
  redirect,
  useLocation,
  useNavigate,
} from "@tanstack/react-router";
import {
  BookOpen,
  Boxes,
  ChartColumn,
  FileText,
  LayoutDashboard,
  LogOut,
  Package,
  ShoppingCart,
  Truck,
  Users,
} from "lucide-react";
import { type ReactNode, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { meQuery, useCan } from "@/lib/session";
import { clearTokens, getTokens } from "@/lib/tokens";

export const Route = createFileRoute("/_app")({
  beforeLoad: () => {
    if (!getTokens()) throw redirect({ to: "/login" });
  },
  loader: ({ context }) => context.queryClient.ensureQueryData(meQuery),
  component: AppLayout,
});

type Item = { to: string; label: string; perm?: string };
type Group = { label: string; icon: ReactNode; items: Item[] };

const menu: Group[] = [
  {
    label: "Overview",
    icon: <LayoutDashboard className="size-4" />,
    items: [{ to: "/", label: "Dashboard" }],
  },
  {
    label: "Sales",
    icon: <ShoppingCart className="size-4" />,
    items: [
      { to: "/sales-orders", label: "Sales orders", perm: "sales:read" },
      { to: "/invoices", label: "Invoices", perm: "sales:read" },
    ],
  },
  {
    label: "Purchasing",
    icon: <Truck className="size-4" />,
    items: [{ to: "/purchase-orders", label: "Purchase orders", perm: "purchasing:read" }],
  },
  {
    label: "Inventory",
    icon: <Boxes className="size-4" />,
    items: [
      { to: "/stock", label: "Stock", perm: "inventory:read" },
      { to: "/movements", label: "Movements", perm: "inventory:read" },
    ],
  },
  {
    label: "Accounting",
    icon: <BookOpen className="size-4" />,
    items: [
      { to: "/journal", label: "Journal", perm: "accounting:read" },
      { to: "/trial-balance", label: "Trial balance", perm: "accounting:read" },
      { to: "/balances", label: "Receivables & payables", perm: "accounting:read" },
      { to: "/supplier-payments", label: "Supplier payments", perm: "accounting:read" },
      { to: "/accounts", label: "Chart of accounts", perm: "accounting:read" },
    ],
  },
  {
    label: "Master data",
    icon: <Package className="size-4" />,
    items: [
      { to: "/products", label: "Products", perm: "masterdata:read" },
      { to: "/partners", label: "Customers & suppliers", perm: "masterdata:read" },
      { to: "/warehouses", label: "Warehouses", perm: "masterdata:read" },
      { to: "/categories", label: "Categories", perm: "masterdata:read" },
      { to: "/units", label: "Units", perm: "masterdata:read" },
    ],
  },
  {
    label: "Reports",
    icon: <ChartColumn className="size-4" />,
    items: [{ to: "/reports", label: "Exports", perm: "reports:read" }],
  },
  {
    label: "Admin",
    icon: <Users className="size-4" />,
    items: [{ to: "/users", label: "Users", perm: "users:manage" }],
  },
];

/** Permission needed for a path: the menu item it belongs to (detail pages inherit their list's). */
function requiredPermission(path: string) {
  const items = menu.flatMap((g) => g.items).filter((i) => i.to !== "/" && i.perm);
  return items.find((i) => path === i.to || path.startsWith(`${i.to}/`))?.perm;
}

function AppLayout() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const qc = useQueryClient();
  const can = useCan();
  const { data: me } = useQuery(meQuery);

  const signOut = () => {
    clearTokens();
    qc.clear();
    navigate({ to: "/login" });
  };

  useEffect(() => {
    const onSignedOut = () => navigate({ to: "/login" });
    window.addEventListener("erp:signed-out", onSignedOut);
    return () => window.removeEventListener("erp:signed-out", onSignedOut);
  }, [navigate]);

  return (
    <div className="flex min-h-screen">
      <aside className="flex w-60 shrink-0 flex-col border-r bg-secondary/40">
        <div className="flex items-center gap-2 px-5 py-4 text-lg font-semibold">
          <FileText className="size-5 text-primary" /> ERP Lab
        </div>
        <nav className="flex-1 space-y-4 overflow-y-auto px-3 pb-4">
          {menu.map((group) => {
            const items = group.items.filter((i) => !i.perm || can(i.perm));
            if (items.length === 0) return null;
            return (
              <div key={group.label}>
                <div className="mb-1 flex items-center gap-2 px-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  {group.icon}
                  {group.label}
                </div>
                {items.map((item) => (
                  <Link
                    key={item.to}
                    to={item.to}
                    className="block rounded-md px-3 py-1.5 text-sm hover:bg-accent"
                    activeProps={{ className: "bg-accent font-medium" }}
                    activeOptions={{ exact: item.to === "/" }}
                  >
                    {item.label}
                  </Link>
                ))}
              </div>
            );
          })}
        </nav>
        <div className="border-t p-3">
          <div className="truncate px-2 text-sm font-medium">{me?.fullName}</div>
          <div className="truncate px-2 text-xs text-muted-foreground">
            {me?.roles.join(", ").toLowerCase()}
          </div>
          <Button variant="ghost" size="sm" className="mt-2 w-full justify-start" onClick={signOut}>
            <LogOut /> Sign out
          </Button>
        </div>
      </aside>
      <main className="min-w-0 flex-1 p-6">
        {(() => {
          const perm = requiredPermission(pathname);
          return perm && !can(perm) ? (
            <div className="py-20 text-center">
              <h1 className="text-xl font-semibold">No access</h1>
              <p className="mt-2 text-sm text-muted-foreground">
                Your role does not include {perm}.
              </p>
            </div>
          ) : (
            <Outlet />
          );
        })()}
      </main>
    </div>
  );
}
