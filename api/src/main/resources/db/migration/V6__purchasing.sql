CREATE SEQUENCE purchase_order_number_seq;

CREATE TABLE purchase_order (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version       BIGINT NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    VARCHAR(100),
  updated_at    TIMESTAMPTZ,
  updated_by    VARCHAR(100),
  deleted_at    TIMESTAMPTZ,
  number        VARCHAR(30) NOT NULL UNIQUE,
  supplier_id   BIGINT REFERENCES partner (id),
  warehouse_id  BIGINT REFERENCES warehouse (id),
  order_date    DATE,
  expected_date DATE,
  status        VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED', 'DONE', 'CANCELLED')),
  note          VARCHAR(1000),
  total         NUMERIC(18, 2) NOT NULL DEFAULT 0
);
CREATE INDEX purchase_order_status_idx ON purchase_order (status);

CREATE TABLE purchase_order_line (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  purchase_order_id BIGINT NOT NULL REFERENCES purchase_order (id),
  product_id        BIGINT NOT NULL REFERENCES product (id),
  quantity          NUMERIC(18, 4) NOT NULL CHECK (quantity > 0),
  unit_price        NUMERIC(18, 2) NOT NULL CHECK (unit_price >= 0),
  received_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0 CHECK (received_quantity >= 0 AND received_quantity <= quantity)
);
