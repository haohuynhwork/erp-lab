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

	public record EntryView(Long id, String number, LocalDate entryDate, String description, String sourceType,
			Long sourceId, String sourceNumber, BigDecimal amount, boolean reversed, Long reversalOfId) {
	}

	public record LineView(Long accountId, String accountCode, String accountName, Long partnerId, String partnerName,
			BigDecimal debit, BigDecimal credit, String memo) {
	}

	public record EntryDetail(EntryView entry, List<LineView> lines) {
	}

	/** Open receivable (customers) or payable (suppliers) per partner. */
	public record PartnerBalance(Long partnerId, String code, String name, BigDecimal balance) {
	}

	public record PaymentView(Long id, String number, Long supplierId, String supplierName, LocalDate paymentDate,
			BigDecimal amount, String method, String reference) {
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

	public PageResult<EntryView> entries(String q, String sourceType, Long accountId, LocalDate from, LocalDate to,
			int page, int size) {
		return sql.query(ENTRY_COLUMNS, """
				journal_entry e WHERE (e.number ILIKE :q OR e.description ILIKE :q OR coalesce(e.source_number, '') ILIKE :q)
				 AND (CAST(:sourceType AS text) IS NULL OR e.source_type = :sourceType)
				 AND (CAST(:accountId AS bigint) IS NULL
				      OR EXISTS (SELECT 1 FROM journal_line l WHERE l.journal_entry_id = e.id AND l.account_id = :accountId))
				 AND (CAST(:from AS date) IS NULL OR e.entry_date >= :from)
				 AND (CAST(:to AS date) IS NULL OR e.entry_date <= :to)""",
				Params.of("q", SqlPage.like(q), "sourceType", sourceType, "accountId", accountId, "from", from, "to", to),
				"e.id DESC", page, size, EntryView.class);
	}

	public EntryDetail entry(Long id) {
		var entry = jdbc.sql("SELECT " + ENTRY_COLUMNS + " FROM journal_entry e WHERE e.id = :id")
			.param("id", id)
			.query(EntryView.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Journal entry", id));
		var lines = jdbc.sql("""
				SELECT l.account_id, a.code AS account_code, a.name AS account_name, l.partner_id, p.name AS partner_name,
				       l.debit, l.credit, l.memo
				FROM journal_line l JOIN account a ON a.id = l.account_id LEFT JOIN partner p ON p.id = l.partner_id
				WHERE l.journal_entry_id = :id ORDER BY l.position""")
			.param("id", id)
			.query(LineView.class)
			.list();
		return new EntryDetail(entry, lines);
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

	public PageResult<PaymentView> supplierPayments(Long supplierId, int page, int size) {
		return sql.query("""
				sp.id, sp.number, sp.supplier_id, p.name AS supplier_name, sp.payment_date, sp.amount, sp.method,
				sp.reference""", """
				supplier_payment sp JOIN partner p ON p.id = sp.supplier_id
				WHERE (CAST(:supplierId AS bigint) IS NULL OR sp.supplier_id = :supplierId)""",
				Params.of("supplierId", supplierId), "sp.id DESC", page, size, PaymentView.class);
	}

}
