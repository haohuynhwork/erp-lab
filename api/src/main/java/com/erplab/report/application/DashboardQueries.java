package com.erplab.report.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Read-only numbers across contexts for the dashboard (SQL over their tables; reports never write). */
@Service
public class DashboardQueries {

	public record Kpis(BigDecimal salesThisMonth, BigDecimal salesLastMonth, long openSalesOrders,
			long openPurchaseOrders, BigDecimal receivable, BigDecimal overdueReceivable, BigDecimal payable,
			BigDecimal inventoryValue, long lowStockItems) {
	}

	public record MonthAmount(String month, BigDecimal sales, BigDecimal purchases, BigDecimal grossProfit) {
	}

	public record TopProduct(Long productId, String sku, String name, BigDecimal quantity, BigDecimal revenue) {
	}

	public record Dashboard(Kpis kpis, List<MonthAmount> months, List<TopProduct> topProducts) {
	}

	private final JdbcClient jdbc;

	DashboardQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Dashboard dashboard() {
		var kpis = jdbc.sql("""
				SELECT
				  (SELECT coalesce(sum(subtotal), 0) FROM invoice
				    WHERE issue_date >= date_trunc('month', current_date)) AS sales_this_month,
				  (SELECT coalesce(sum(subtotal), 0) FROM invoice
				    WHERE issue_date >= date_trunc('month', current_date) - interval '1 month'
				      AND issue_date < date_trunc('month', current_date)) AS sales_last_month,
				  (SELECT count(*) FROM sales_order WHERE status IN ('DRAFT', 'CONFIRMED') AND deleted_at IS NULL)
				    AS open_sales_orders,
				  (SELECT count(*) FROM purchase_order WHERE status IN ('DRAFT', 'CONFIRMED') AND deleted_at IS NULL)
				    AS open_purchase_orders,
				  (SELECT coalesce(sum(total - paid_amount), 0) FROM invoice) AS receivable,
				  (SELECT coalesce(sum(total - paid_amount), 0) FROM invoice
				    WHERE status <> 'PAID' AND due_date < current_date) AS overdue_receivable,
				  (SELECT coalesce(sum(l.credit - l.debit), 0) FROM journal_line l JOIN account a ON a.id = l.account_id
				    WHERE a.code = '2110') AS payable,
				  (SELECT coalesce(round(sum(quantity * average_cost), 2), 0) FROM stock_item) AS inventory_value,
				  (SELECT count(*) FROM stock_item s JOIN product p ON p.id = s.product_id WHERE s.quantity < p.min_stock)
				    AS low_stock_items""")
			.query(Kpis.class)
			.single();
		var months = jdbc.sql("""
				WITH m AS (
				  SELECT generate_series(date_trunc('month', current_date) - interval '11 months',
				                         date_trunc('month', current_date), interval '1 month')::date AS month)
				SELECT to_char(m.month, 'YYYY-MM') AS month,
				  (SELECT coalesce(sum(subtotal), 0) FROM invoice
				    WHERE date_trunc('month', issue_date) = m.month) AS sales,
				  (SELECT coalesce(sum(l.debit), 0) FROM journal_line l JOIN journal_entry e ON e.id = l.journal_entry_id
				    JOIN account a ON a.id = l.account_id
				    WHERE a.code = '1410' AND e.source_type = 'PURCHASE_ORDER' AND date_trunc('month', e.entry_date) = m.month)
				    AS purchases,
				  (SELECT coalesce(sum(l.credit - l.debit), 0) FROM journal_line l JOIN journal_entry e ON e.id = l.journal_entry_id
				    JOIN account a ON a.id = l.account_id
				    WHERE a.code IN ('4110', '5110') AND date_trunc('month', e.entry_date) = m.month) AS gross_profit
				FROM m ORDER BY m.month""")
			.query(MonthAmount.class)
			.list();
		var top = jdbc.sql("""
				SELECT p.id AS product_id, p.sku, p.name, sum(l.quantity) AS quantity,
				       round(sum(l.quantity * l.unit_price), 2) AS revenue
				FROM invoice_line l JOIN invoice i ON i.id = l.invoice_id JOIN product p ON p.id = l.product_id
				WHERE i.issue_date >= current_date - interval '90 days'
				GROUP BY p.id ORDER BY revenue DESC LIMIT 5""")
			.query(TopProduct.class)
			.list();
		return new Dashboard(kpis, months, top);
	}

}
