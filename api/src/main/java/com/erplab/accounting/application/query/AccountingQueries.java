package com.erplab.accounting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.erplab.common.domain.NotFoundException;
import com.erplab.common.http.PageResult;
import com.erplab.common.infrastructure.Params;
import com.erplab.common.infrastructure.SqlPage;

@Service
public class AccountingQueries {

	public record AccountView(Long id, String code, String name, String type, boolean active) {
	}

	/** Balance is shown on the account's normal side (positive = normal). */
	public record TrialBalanceRow(Long accountId, String code, String name, String type, BigDecimal debit,
			BigDecimal credit, BigDecimal balance) {
	}

	public record TrialBalance(LocalDate from, LocalDate to, List<TrialBalanceRow> rows, BigDecimal totalDebit,
			BigDecimal totalCredit) {
	}

	public record JournalEntryView(Long id, String number, LocalDate entryDate, String description, String sourceType,
			Long sourceId, String sourceNumber, BigDecimal amount, boolean reversed, Long reversalOfId) {
	}

	public record JournalLineView(Long accountId, String accountCode, String accountName, Long partnerId, String partnerName,
			BigDecimal debit, BigDecimal credit, String memo) {
	}

	public record JournalEntryDetail(JournalEntryView entry, List<JournalLineView> lines) {
	}

	/** Open receivable (customers) or payable (suppliers) per partner. */
	public record PartnerBalance(Long partnerId, String code, String name, BigDecimal balance) {
	}

	/** One account's amount in a statement, positive on its normal side. */
	public record StatementLine(Long accountId, String code, String name, BigDecimal amount) {
	}

	public record ProfitAndLoss(LocalDate from, LocalDate to, List<StatementLine> revenue, BigDecimal totalRevenue,
			List<StatementLine> expenses, BigDecimal totalExpenses, BigDecimal netProfit) {
	}

	public record BalanceSheet(LocalDate asOf, List<StatementLine> assets, BigDecimal totalAssets,
			List<StatementLine> liabilities, BigDecimal totalLiabilities, List<StatementLine> equity,
			BigDecimal currentEarnings, BigDecimal totalEquity, boolean balanced) {
	}

	private static final String ENTRY_COLUMNS = """
			e.id, e.number, e.entry_date, e.description, e.source_type, e.source_id, e.source_number,
			(SELECT sum(debit) FROM journal_line WHERE journal_entry_id = e.id) AS amount, e.reversed, e.reversal_of_id""";

	private final JdbcClient jdbc;

	private final SqlPage sql;

	AccountingQueries(JdbcClient jdbc, SqlPage sql) {
		this.jdbc = jdbc;
		this.sql = sql;
	}

	public PageResult<AccountView> accounts(String q, int page, int size) {
		return sql.query("id, code, name, type, active",
				"account WHERE deleted_at IS NULL AND (code ILIKE :q OR name ILIKE :q)",
				Params.of("q", SqlPage.like(q)), "code", page, size, AccountView.class);
	}

	public TrialBalance trialBalance(LocalDate from, LocalDate to) {
		var rows = jdbc.sql("""
				SELECT a.id AS account_id, a.code, a.name, a.type,
				       coalesce(sum(l.debit), 0) AS debit, coalesce(sum(l.credit), 0) AS credit,
				       CASE WHEN a.type IN ('ASSET', 'EXPENSE') THEN coalesce(sum(l.debit - l.credit), 0)
				            ELSE coalesce(sum(l.credit - l.debit), 0) END AS balance
				FROM account a
				LEFT JOIN journal_line l ON l.account_id = a.id
				 AND l.journal_entry_id IN (SELECT id FROM journal_entry
				                            WHERE (CAST(:from AS date) IS NULL OR entry_date >= :from)
				                              AND (CAST(:to AS date) IS NULL OR entry_date <= :to))
				WHERE a.deleted_at IS NULL
				GROUP BY a.id ORDER BY a.code""")
			.params(Params.of("from", from, "to", to))
			.query(TrialBalanceRow.class)
			.list();
		var debit = rows.stream().map(TrialBalanceRow::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
		var credit = rows.stream().map(TrialBalanceRow::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new TrialBalance(from, to, rows, debit, credit);
	}

	public PageResult<JournalEntryView> entries(String q, String sourceType, Long accountId, LocalDate from, LocalDate to,
			int page, int size) {
		return sql.query(ENTRY_COLUMNS, """
				journal_entry e WHERE (e.number ILIKE :q OR e.description ILIKE :q OR coalesce(e.source_number, '') ILIKE :q)
				 AND (CAST(:sourceType AS text) IS NULL OR e.source_type = :sourceType)
				 AND (CAST(:accountId AS bigint) IS NULL
				      OR EXISTS (SELECT 1 FROM journal_line l WHERE l.journal_entry_id = e.id AND l.account_id = :accountId))
				 AND (CAST(:from AS date) IS NULL OR e.entry_date >= :from)
				 AND (CAST(:to AS date) IS NULL OR e.entry_date <= :to)""",
				Params.of("q", SqlPage.like(q), "sourceType", sourceType, "accountId", accountId, "from", from, "to", to),
				"e.id DESC", page, size, JournalEntryView.class);
	}

	public JournalEntryDetail entry(Long id) {
		var entry = jdbc.sql("SELECT " + ENTRY_COLUMNS + " FROM journal_entry e WHERE e.id = :id")
			.param("id", id)
			.query(JournalEntryView.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Journal entry", id));
		var lines = jdbc.sql("""
				SELECT l.account_id, a.code AS account_code, a.name AS account_name, l.partner_id, p.name AS partner_name,
				       l.debit, l.credit, l.memo
				FROM journal_line l JOIN account a ON a.id = l.account_id LEFT JOIN partner p ON p.id = l.partner_id
				WHERE l.journal_entry_id = :id ORDER BY l.position""")
			.param("id", id)
			.query(JournalLineView.class)
			.list();
		return new JournalEntryDetail(entry, lines);
	}

	/** @param kind "receivable" or "payable" */
	public List<PartnerBalance> partnerBalances(String kind) {
		boolean receivable = "receivable".equals(kind);
		return jdbc.sql("""
				SELECT p.id AS partner_id, p.code, p.name, sum(%s) AS balance
				FROM journal_line l JOIN account a ON a.id = l.account_id JOIN partner p ON p.id = l.partner_id
				WHERE a.code = :code
				GROUP BY p.id HAVING sum(%s) <> 0 ORDER BY balance DESC""".formatted(
				receivable ? "l.debit - l.credit" : "l.credit - l.debit",
				receivable ? "l.debit - l.credit" : "l.credit - l.debit"))
			.param("code", receivable ? "1310" : "2110")
			.query(PartnerBalance.class)
			.list();
	}

	/** Revenue and expenses posted between {@code from} and {@code to} (inclusive; null = open-ended). */
	public ProfitAndLoss profitAndLoss(LocalDate from, LocalDate to) {
		var revenue = statement("REVENUE", from, to);
		var expenses = statement("EXPENSE", from, to);
		var totalRevenue = sum(revenue);
		var totalExpenses = sum(expenses);
		return new ProfitAndLoss(from, to, revenue, totalRevenue, expenses, totalExpenses,
				totalRevenue.subtract(totalExpenses));
	}

	/** Balances at {@code asOf}; profit not yet closed to equity shows as current earnings. */
	public BalanceSheet balanceSheet(LocalDate asOf) {
		var assets = statement("ASSET", null, asOf);
		var liabilities = statement("LIABILITY", null, asOf);
		var equity = statement("EQUITY", null, asOf);
		var earnings = sum(statement("REVENUE", null, asOf)).subtract(sum(statement("EXPENSE", null, asOf)));
		var totalAssets = sum(assets);
		var totalLiabilities = sum(liabilities);
		var totalEquity = sum(equity).add(earnings);
		return new BalanceSheet(asOf, assets, totalAssets, liabilities, totalLiabilities, equity, earnings, totalEquity,
				totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0);
	}

	private List<StatementLine> statement(String type, LocalDate from, LocalDate to) {
		return jdbc.sql("""
				SELECT a.id AS account_id, a.code, a.name,
				       coalesce(sum(CASE WHEN a.type IN ('ASSET', 'EXPENSE') THEN l.debit - l.credit
				                         ELSE l.credit - l.debit END), 0) AS amount
				FROM account a
				JOIN journal_line l ON l.account_id = a.id
				JOIN journal_entry e ON e.id = l.journal_entry_id
				WHERE a.type = :type
				  AND (CAST(:from AS date) IS NULL OR e.entry_date >= :from)
				  AND (CAST(:to AS date) IS NULL OR e.entry_date <= :to)
				GROUP BY a.id HAVING sum(l.debit - l.credit) <> 0 ORDER BY a.code""")
			.params(Params.of("type", type, "from", from, "to", to))
			.query(StatementLine.class)
			.list();
	}

	private static BigDecimal sum(List<StatementLine> lines) {
		return lines.stream().map(StatementLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}
