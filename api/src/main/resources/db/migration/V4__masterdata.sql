CREATE TABLE unit (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version    BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(100),
  updated_at TIMESTAMPTZ,
  updated_by VARCHAR(100),
  deleted_at TIMESTAMPTZ,
  code       VARCHAR(20) NOT NULL,
  name       VARCHAR(100) NOT NULL
);
CREATE UNIQUE INDEX unit_code_uq ON unit (lower(code)) WHERE deleted_at IS NULL;

CREATE TABLE product_category (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version     BIGINT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by  VARCHAR(100),
  updated_at  TIMESTAMPTZ,
  updated_by  VARCHAR(100),
  deleted_at  TIMESTAMPTZ,
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(500)
);
CREATE UNIQUE INDEX product_category_name_uq ON product_category (lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE product (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version        BIGINT NOT NULL DEFAULT 0,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ,
  updated_by     VARCHAR(100),
  deleted_at     TIMESTAMPTZ,
  sku            VARCHAR(50) NOT NULL,
  name           VARCHAR(200),
  type           VARCHAR(20) NOT NULL CHECK (type IN ('RAW', 'SEMI_FINISHED', 'FINISHED', 'GOODS', 'SERVICE')),
  unit_id        BIGINT REFERENCES unit (id),
  category_id    BIGINT REFERENCES product_category (id),
  purchase_price NUMERIC(18, 2) NOT NULL DEFAULT 0,
  sale_price     NUMERIC(18, 2) NOT NULL DEFAULT 0,
  min_stock      NUMERIC(18, 4) NOT NULL DEFAULT 0,
  active         BOOLEAN NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX product_sku_uq ON product (lower(sku)) WHERE deleted_at IS NULL;

CREATE TABLE product_bom_line (
  product_id   BIGINT NOT NULL REFERENCES product (id),
  position     INT NOT NULL,
  component_id BIGINT NOT NULL REFERENCES product (id),
  quantity     NUMERIC(18, 4) NOT NULL CHECK (quantity > 0),
  PRIMARY KEY (product_id, position)
);

CREATE TABLE warehouse (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version    BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(100),
  updated_at TIMESTAMPTZ,
  updated_by VARCHAR(100),
  deleted_at TIMESTAMPTZ,
  code       VARCHAR(20) NOT NULL,
  name       VARCHAR(200),
  address    VARCHAR(500),
  active     BOOLEAN NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX warehouse_code_uq ON warehouse (lower(code)) WHERE deleted_at IS NULL;

CREATE TABLE partner (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version           BIGINT NOT NULL DEFAULT 0,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by        VARCHAR(100),
  updated_at        TIMESTAMPTZ,
  updated_by        VARCHAR(100),
  deleted_at        TIMESTAMPTZ,
  code              VARCHAR(20) NOT NULL,
  name              VARCHAR(200),
  customer          BOOLEAN NOT NULL DEFAULT false,
  supplier          BOOLEAN NOT NULL DEFAULT false,
  tax_code          VARCHAR(50),
  email             VARCHAR(200),
  phone             VARCHAR(50),
  address           VARCHAR(500),
  payment_term_days INT NOT NULL DEFAULT 0,
  active            BOOLEAN NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX partner_code_uq ON partner (lower(code)) WHERE deleted_at IS NULL;
