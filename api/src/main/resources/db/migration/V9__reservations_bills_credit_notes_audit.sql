-- Stock reservations for confirmed sales orders (may exceed quantity: a shortage)
ALTER TABLE stock_item ADD COLUMN reserved NUMERIC(18, 4) NOT NULL DEFAULT 0 CHECK (reserved >= 0);

-- Draft sales orders are quotations
ALTER TABLE sales_order ADD COLUMN valid_until DATE;

-- Supplier bills replace free-standing supplier payments
ALTER TABLE purchase_order_line ADD COLUMN billed_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0;
ALTER TABLE purchase_order_line ADD CHECK (billed_quantity <= received_quantity);

CREATE SEQUENCE supplier_bill_number_seq;

CREATE TABLE supplier_bill (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version            BIGINT NOT NULL DEFAULT 0,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by         VARCHAR(100),
  updated_at         TIMESTAMPTZ,
  updated_by         VARCHAR(100),
  deleted_at         TIMESTAMPTZ,
  number             VARCHAR(30) NOT NULL UNIQUE,
  supplier_reference VARCHAR(100),
  purchase_order_id  BIGINT NOT NULL REFERENCES purchase_order (id),
  supplier_id        BIGINT NOT NULL REFERENCES partner (id),
  bill_date          DATE NOT NULL,
  due_date           DATE NOT NULL,
  total              NUMERIC(18, 2) NOT NULL,
  paid_amount        NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (paid_amount <= total),
  status             VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'PARTIALLY_PAID', 'PAID'))
);
CREATE INDEX supplier_bill_supplier_idx ON supplier_bill (supplier_id, status);

CREATE TABLE supplier_bill_line (
  bill_id    BIGINT NOT NULL REFERENCES supplier_bill (id),
  position   INT NOT NULL,
  product_id BIGINT NOT NULL REFERENCES product (id),
  quantity   NUMERIC(18, 4) NOT NULL,
  unit_price NUMERIC(18, 2) NOT NULL,
  PRIMARY KEY (bill_id, position)
);

CREATE TABLE bill_payment (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  bill_id      BIGINT NOT NULL REFERENCES supplier_bill (id),
  payment_date DATE NOT NULL,
  amount       NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
  method       VARCHAR(30) NOT NULL,
  reference    VARCHAR(100)
);

DROP TABLE supplier_payment;
DROP SEQUENCE supplier_payment_number_seq;

-- Credit notes
ALTER TABLE invoice ADD COLUMN credited_amount NUMERIC(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE invoice DROP CONSTRAINT invoice_check;
ALTER TABLE invoice ADD CONSTRAINT invoice_settled_check CHECK (paid_amount + credited_amount <= total);
ALTER TABLE invoice DROP CONSTRAINT invoice_status_check;
ALTER TABLE invoice ADD CONSTRAINT invoice_status_check CHECK (status IN ('ISSUED', 'PARTIALLY_PAID', 'PAID', 'CREDITED'));
ALTER TABLE invoice_line ADD COLUMN credited_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0;

CREATE SEQUENCE credit_note_number_seq;

CREATE TABLE credit_note (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version        BIGINT NOT NULL DEFAULT 0,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ,
  updated_by     VARCHAR(100),
  deleted_at     TIMESTAMPTZ,
  number         VARCHAR(30) NOT NULL UNIQUE,
  invoice_id     BIGINT NOT NULL REFERENCES invoice (id),
  invoice_number VARCHAR(30) NOT NULL,
  customer_id    BIGINT NOT NULL REFERENCES partner (id),
  issue_date     DATE NOT NULL,
  reason         VARCHAR(500) NOT NULL,
  tax_rate       NUMERIC(5, 2) NOT NULL,
  subtotal       NUMERIC(18, 2) NOT NULL,
  tax_amount     NUMERIC(18, 2) NOT NULL,
  total          NUMERIC(18, 2) NOT NULL
);

CREATE TABLE credit_note_line (
  credit_note_id    BIGINT NOT NULL REFERENCES credit_note (id),
  position          INT NOT NULL,
  product_id        BIGINT NOT NULL REFERENCES product (id),
  quantity          NUMERIC(18, 4) NOT NULL,
  unit_price        NUMERIC(18, 2) NOT NULL,
  credited_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0,
  PRIMARY KEY (credit_note_id, position)
);

-- Who did what (written by CommandAudit for every successful command)
CREATE TABLE audit_log (
  id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  username  VARCHAR(100) NOT NULL,
  context   VARCHAR(30) NOT NULL,
  action    VARCHAR(60) NOT NULL,
  target_id BIGINT,
  details   JSONB
);
CREATE INDEX audit_log_user_idx ON audit_log (username, at DESC);
CREATE INDEX audit_log_context_idx ON audit_log (context, at DESC);
