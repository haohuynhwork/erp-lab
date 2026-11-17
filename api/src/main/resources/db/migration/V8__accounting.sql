CREATE SEQUENCE journal_entry_number_seq;
CREATE SEQUENCE supplier_payment_number_seq;

CREATE TABLE account (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version    BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(100),
  updated_at TIMESTAMPTZ,
  updated_by VARCHAR(100),
  deleted_at TIMESTAMPTZ,
  code       VARCHAR(10) NOT NULL UNIQUE,
  name       VARCHAR(200) NOT NULL,
  type       VARCHAR(20) NOT NULL CHECK (type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
  active     BOOLEAN NOT NULL DEFAULT true
);

-- Accounts used by automatic postings (codes referenced in Account.java)
INSERT INTO account (code, name, type, created_by) VALUES
  ('1110', 'Cash and bank', 'ASSET', 'system'),
  ('1310', 'Accounts receivable', 'ASSET', 'system'),
  ('1410', 'Inventory', 'ASSET', 'system'),
  ('2110', 'Accounts payable', 'LIABILITY', 'system'),
  ('2210', 'VAT payable (output)', 'LIABILITY', 'system'),
  ('3110', 'Opening balance equity', 'EQUITY', 'system'),
  ('4110', 'Sales revenue', 'REVENUE', 'system'),
  ('5110', 'Cost of goods sold', 'EXPENSE', 'system'),
  ('5210', 'Inventory adjustments', 'EXPENSE', 'system'),
  ('6110', 'Operating expenses', 'EXPENSE', 'system');

CREATE TABLE journal_entry (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version        BIGINT NOT NULL DEFAULT 0,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ,
  updated_by     VARCHAR(100),
  deleted_at     TIMESTAMPTZ,
  number         VARCHAR(30) NOT NULL UNIQUE,
  entry_date     DATE NOT NULL,
  description    VARCHAR(500) NOT NULL,
  source_type    VARCHAR(30) NOT NULL,
  source_id      BIGINT,
  source_number  VARCHAR(30),
  reversal_of_id BIGINT REFERENCES journal_entry (id),
  reversed       BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX journal_entry_date_idx ON journal_entry (entry_date);
CREATE INDEX journal_entry_source_idx ON journal_entry (source_type, source_id);

CREATE TABLE journal_line (
  journal_entry_id BIGINT NOT NULL REFERENCES journal_entry (id),
  position         INT NOT NULL,
  account_id       BIGINT NOT NULL REFERENCES account (id),
  partner_id       BIGINT REFERENCES partner (id),
  debit            NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (debit >= 0),
  credit           NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (credit >= 0),
  memo             VARCHAR(500),
  PRIMARY KEY (journal_entry_id, position)
);
CREATE INDEX journal_line_account_idx ON journal_line (account_id);
CREATE INDEX journal_line_partner_idx ON journal_line (partner_id) WHERE partner_id IS NOT NULL;

CREATE TABLE supplier_payment (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version      BIGINT NOT NULL DEFAULT 0,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by   VARCHAR(100),
  updated_at   TIMESTAMPTZ,
  updated_by   VARCHAR(100),
  deleted_at   TIMESTAMPTZ,
  number       VARCHAR(30) NOT NULL UNIQUE,
  supplier_id  BIGINT NOT NULL REFERENCES partner (id),
  payment_date DATE NOT NULL,
  amount       NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
  method       VARCHAR(30) NOT NULL,
  reference    VARCHAR(100)
);
