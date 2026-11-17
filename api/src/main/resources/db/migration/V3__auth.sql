-- Every aggregate table has the BaseEntity columns:
--   id, version, created_at, created_by, updated_at, updated_by, deleted_at (soft delete)

CREATE TABLE app_user (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version       BIGINT NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    VARCHAR(100),
  updated_at    TIMESTAMPTZ,
  updated_by    VARCHAR(100),
  deleted_at    TIMESTAMPTZ,
  username      VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(200) NOT NULL,
  email         VARCHAR(200),
  enabled       BOOLEAN NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX app_user_username_uq ON app_user (username) WHERE deleted_at IS NULL;

CREATE TABLE app_user_role (
  user_id BIGINT NOT NULL REFERENCES app_user (id),
  role    VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'SALES', 'PURCHASING', 'WAREHOUSE', 'ACCOUNTANT')),
  PRIMARY KEY (user_id, role)
);
