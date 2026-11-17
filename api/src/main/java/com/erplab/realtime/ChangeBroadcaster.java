package com.erplab.realtime;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.erplab.inventory.StockValueChanged;
import com.erplab.purchasing.BillPaymentMade;
import com.erplab.purchasing.GoodsReceived;
import com.erplab.sales.CreditNoteIssued;
import com.erplab.sales.DeliveryShipped;
import com.erplab.sales.InvoiceIssued;
import com.erplab.sales.InvoicePaymentReceived;
import com.erplab.sales.SalesOrderCancelled;
import com.erplab.sales.SalesOrderConfirmed;

/**
 * Tells connected browsers which lists to refetch. Listens to the domain events after the publishing
 * transaction commits, so a client that refetches immediately sees the new data.
 */
@Component
@ConditionalOnWebApplication
class ChangeBroadcaster {

	/** Everything the ledger feeds. */
	private static final List<String> LEDGER = List.of("journal", "trial-balance", "profit-and-loss", "balance-sheet",
			"balances", "dashboard");

	private final SimpMessagingTemplate messaging;

	ChangeBroadcaster(SimpMessagingTemplate messaging) {
		this.messaging = messaging;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(SalesOrderConfirmed e) {
		send("sales-orders", "stock", "dashboard");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(SalesOrderCancelled e) {
		send("sales-orders", "stock", "dashboard");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(DeliveryShipped e) {
		send("sales-orders", "stock", "movements");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(InvoiceIssued e) {
		send("invoices", "sales-orders");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(CreditNoteIssued e) {
		send("invoices", "sales-orders");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(InvoicePaymentReceived e) {
		send("invoices");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(GoodsReceived e) {
		send("purchase-orders", "stock", "movements");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(BillPaymentMade e) {
		send("bills", "purchase-orders");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(StockValueChanged e) {
		send("stock", "movements");
	}

	private void send(String... resources) {
		var all = new java.util.LinkedHashSet<>(List.of(resources));
		all.addAll(LEDGER);
		messaging.convertAndSend(RealtimeConfig.TOPIC, new ChangeNotice(List.copyOf(all)));
	}

}
