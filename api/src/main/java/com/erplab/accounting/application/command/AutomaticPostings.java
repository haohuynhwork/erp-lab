package com.erplab.accounting.application.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.erplab.accounting.domain.Account;
import com.erplab.accounting.domain.JournalLine;
import com.erplab.inventory.StockValueChanged;
import com.erplab.purchasing.BillPaymentMade;
import com.erplab.purchasing.GoodsReceived;
import com.erplab.sales.CreditNoteIssued;
import com.erplab.sales.InvoiceIssued;
import com.erplab.sales.InvoicePaymentReceived;

/**
 * Books business events into the journal, synchronously in the business transaction (the ledger always matches the
 * documents):
 * <ul>
 * <li>goods received: Dr inventory / Cr payables (supplier)</li>
 * <li>invoice issued: Dr receivables (customer) / Cr revenue, Cr output VAT</li>
 * <li>credit note: Dr revenue, Dr output VAT / Cr receivables (customer)</li>
 * <li>customer payment: Dr cash / Cr receivables (customer)</li>
 * <li>supplier bill payment: Dr payables (supplier) / Cr cash</li>
 * <li>delivery (stock issued): Dr COGS / Cr inventory</li>
 * <li>manual stock receipt: Dr inventory / Cr opening equity; manual issue and count differences: inventory
 * adjustment</li>
 * </ul>
 */
@Component
class AutomaticPostings {

	private final AccountingCommandHandler ledger;

	AutomaticPostings(AccountingCommandHandler ledger) {
		this.ledger = ledger;
	}

	@EventListener
	void on(GoodsReceived e) {
		if (e.amount().signum() == 0) {
			return;
		}
		ledger.post(e.date(), "Goods received " + e.purchaseOrderNumber(), "PURCHASE_ORDER", e.purchaseOrderId(),
				e.purchaseOrderNumber(), List.of(JournalLine.debit(acc(Account.INVENTORY), null, e.amount()),
						JournalLine.credit(acc(Account.PAYABLE), e.supplierId(), e.amount())));
	}

	@EventListener
	void on(InvoiceIssued e) {
		var lines = new ArrayList<JournalLine>();
		lines.add(JournalLine.debit(acc(Account.RECEIVABLE), e.customerId(), e.total()));
		lines.add(JournalLine.credit(acc(Account.REVENUE), null, e.subtotal()));
		if (e.taxAmount().signum() > 0) {
			lines.add(JournalLine.credit(acc(Account.VAT_OUTPUT), null, e.taxAmount()));
		}
		ledger.post(e.date(), "Invoice " + e.invoiceNumber(), "INVOICE", e.invoiceId(), e.invoiceNumber(), lines);
	}

	@EventListener
	void on(CreditNoteIssued e) {
		var lines = new ArrayList<JournalLine>();
		lines.add(JournalLine.debit(acc(Account.REVENUE), null, e.subtotal()));
		if (e.taxAmount().signum() > 0) {
			lines.add(JournalLine.debit(acc(Account.VAT_OUTPUT), null, e.taxAmount()));
		}
		lines.add(JournalLine.credit(acc(Account.RECEIVABLE), e.customerId(), e.total()));
		ledger.post(e.date(), "Credit note " + e.creditNoteNumber() + " for " + e.invoiceNumber(), "CREDIT_NOTE",
				e.creditNoteId(), e.creditNoteNumber(), lines);
	}

	@EventListener
	void on(InvoicePaymentReceived e) {
		ledger.post(e.date(), "Payment for " + e.invoiceNumber() + " (" + e.method() + ")", "INVOICE", e.invoiceId(),
				e.invoiceNumber(), List.of(JournalLine.debit(acc(Account.CASH), null, e.amount()),
						JournalLine.credit(acc(Account.RECEIVABLE), e.customerId(), e.amount())));
	}

	@EventListener
	void on(BillPaymentMade e) {
		ledger.post(e.date(), "Payment of " + e.billNumber() + " (" + e.method() + ")", "SUPPLIER_BILL", e.billId(),
				e.billNumber(), List.of(JournalLine.debit(acc(Account.PAYABLE), e.supplierId(), e.amount()),
						JournalLine.credit(acc(Account.CASH), null, e.amount())));
	}

	@EventListener
	void on(StockValueChanged e) {
		var amount = e.amount().abs();
		if (amount.signum() == 0) {
			return;
		}
		String debit;
		String credit;
		switch (e.reason()) {
			case PURCHASE_RECEIPT -> {
				return; // booked from GoodsReceived
			}
			case SALES_ISSUE -> {
				debit = Account.COGS;
				credit = Account.INVENTORY;
			}
			case MANUAL_RECEIPT -> {
				debit = Account.INVENTORY;
				credit = Account.OPENING_EQUITY;
			}
			case MANUAL_ISSUE -> {
				debit = Account.INVENTORY_ADJUSTMENT;
				credit = Account.INVENTORY;
			}
			case ADJUSTMENT -> {
				boolean gain = e.amount().signum() > 0;
				debit = gain ? Account.INVENTORY : Account.INVENTORY_ADJUSTMENT;
				credit = gain ? Account.INVENTORY_ADJUSTMENT : Account.INVENTORY;
			}
			default -> throw new IllegalStateException("Unknown reason " + e.reason());
		}
		var description = switch (e.reason()) {
			case SALES_ISSUE -> "Cost of goods sold " + e.refNumber();
			case MANUAL_RECEIPT -> "Stock receipt (opening balance)";
			case MANUAL_ISSUE -> "Stock issue";
			default -> "Stock count adjustment";
		};
		ledger.post(e.date(), description, e.refType(), e.refId(), e.refNumber(),
				List.of(JournalLine.debit(acc(debit), null, amount), JournalLine.credit(acc(credit), null, amount)));
	}

	private Long acc(String code) {
		return ledger.account(code);
	}

}
