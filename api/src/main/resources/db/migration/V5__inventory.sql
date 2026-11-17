CREATE TABLE stock_item (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version      BIGINT NOT NULL DEFAULT 0,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by   VARCHAR(100),
  updated_at   TIMESTAMPTZ,
  updated_by   VARCHAR(100),
  deleted_at   TIMESTAMPTZ,
  product_id   BIGINT NOT NULL REFERENCES product (id),
  warehouse_id BIGINT NOT NULL REFERENCES warehouse (id),
  quantity     NUMERIC(18, 4) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  average_cost NUMERIC(18, 4) NOT NULL DEFAULT 0,
  UNIQUE (product_id, warehouse_id)
);

CREATE TABLE stock_movement (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  product_id   BIGINT NOT NULL REFERENCES product (id),
  warehouse_id BIGINT NOT NULL REFERENCES warehouse (id),
  type         VARCHAR(20) NOT NULL CHECK (type IN ('RECEIPT', 'ISSUE', 'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT')),
  quantity     NUMERIC(18, 4) NOT NULL,
  value        NUMERIC(18, 2) NOT NULL,
  ref_type     VARCHAR(30),
  ref_id       BIGINT,
  ref_number   VARCHAR(30),
  note         VARCHAR(500),
  moved_at     TIMESTAMPTZ NOT NULL,
  moved_by     VARCHAR(100)
);
CREATE INDEX stock_movement_product_idx ON stock_movement (product_id, warehouse_id);
CREATE INDEX stock_movement_ref_idx ON stock_movement (ref_type, ref_id);
