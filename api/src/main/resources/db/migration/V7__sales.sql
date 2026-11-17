CREATE SEQUENCE sales_order_number_seq;
CREATE SEQUENCE invoice_number_seq;

CREATE TABLE sales_order (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version       BIGINT NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    VARCHAR(100),
  updated_at    TIMESTAMPTZ,
  updated_by    VARCHAR(100),
  deleted_at    TIMESTAMPTZ,
  number        VARCHAR(30) NOT NULL UNIQUE,
  customer_id   BIGINT REFERENCES partner (id),
  warehouse_id  BIGINT REFERENCES warehouse (id),
  order_date    DATE,
  delivery_date DATE,
  status        VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED', 'DONE', 'CANCELLED')),
  note          VARCHAR(1000),
  tax_rate      NUMERIC(5, 2) NOT NULL DEFAULT 0,
  subtotal      NUMERIC(18, 2) NOT NULL DEFAULT 0,
  tax_amount    NUMERIC(18, 2) NOT NULL DEFAULT 0,
  total         NUMERIC(18, 2) NOT NULL DEFAULT 0
);
CREATE INDEX sales_order_status_idx ON sales_order (status);

CREATE TABLE sales_order_line (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sales_order_id     BIGINT NOT NULL REFERENCES sales_order (id),
  product_id         BIGINT NOT NULL REFERENCES product (id),
  quantity           NUMERIC(18, 4) NOT NULL CHECK (quantity > 0),
  unit_price         NUMERIC(18, 2) NOT NULL CHECK (unit_price >= 0),
  delivered_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0 CHECK (delivered_quantity <= quantity),
  invoiced_quantity  NUMERIC(18, 4) NOT NULL DEFAULT 0 CHECK (invoiced_quantity <= delivered_quantity)
);

CREATE TABLE invoice (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version        BIGINT NOT NULL DEFAULT 0,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ,
  updated_by     VARCHAR(100),
  deleted_at     TIMESTAMPTZ,
  number         VARCHAR(30) NOT NULL UNIQUE,
  sales_order_id BIGINT NOT NULL REFERENCES sales_order (id),
  customer_id    BIGINT NOT NULL REFERENCES partner (id),
  issue_date     DATE NOT NULL,
  due_date       DATE NOT NULL,
  tax_rate       NUMERIC(5, 2) NOT NULL,
  subtotal       NUMERIC(18, 2) NOT NULL,
  tax_amount     NUMERIC(18, 2) NOT NULL,
  total          NUMERIC(18, 2) NOT NULL,
  paid_amount    NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (paid_amount <= total),
  status         VARCHAR(20) NOT NULL CHECK (status IN ('ISSUED', 'PARTIALLY_PAID', 'PAID'))
);
CREATE INDEX invoice_customer_idx ON invoice (customer_id, status);

CREATE TABLE invoice_line (
  invoice_id BIGINT NOT NULL REFERENCES invoice (id),
  position   INT NOT NULL,
  product_id BIGINT NOT NULL REFERENCES product (id),
  quantity   NUMERIC(18, 4) NOT NULL,
  unit_price NUMERIC(18, 2) NOT NULL,
  PRIMARY KEY (invoice_id, position)
);

CREATE TABLE invoice_payment (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  invoice_id   BIGINT NOT NULL REFERENCES invoice (id),
  payment_date DATE NOT NULL,
  amount       NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
  method       VARCHAR(30) NOT NULL,
  reference    VARCHAR(100)
);
