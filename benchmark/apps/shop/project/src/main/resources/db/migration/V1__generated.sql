CREATE TABLE "category" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "name" VARCHAR (100) NOT NULL,
  PRIMARY KEY("id")
);

CREATE TABLE "customer" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "first_name" VARCHAR (100) NOT NULL,
  "last_name" VARCHAR (100) NOT NULL,
  "email" VARCHAR (255) NOT NULL,
  "active" BOOLEAN NOT NULL,
  PRIMARY KEY("id")
);

CREATE TABLE "customer_order_history" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "customer" VARCHAR (255) NOT NULL,
  PRIMARY KEY("id")
);

CREATE TABLE "customer_order_history_entry" (
  "customer_order_history" VARCHAR (255) NOT NULL REFERENCES "customer_order_history"("id"),
  "order" VARCHAR (255) NOT NULL,
  "status" VARCHAR (255) NOT NULL,
  "customer_order_history_key" INTEGER NOT NULL,
  PRIMARY KEY("customer_order_history", "customer_order_history_key")
);

CREATE INDEX "customer_order_history_entry_customer_order_history_ix" ON "customer_order_history_entry" ("customer_order_history");

CREATE TABLE "invoice" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "order" VARCHAR (255) NOT NULL,
  "customer" VARCHAR (255) NOT NULL,
  "amount" DECIMAL (19, 4) NOT NULL,
  "status" VARCHAR (255) NOT NULL,
  PRIMARY KEY("id")
);

CREATE INDEX "invoice_order_ix" ON "invoice" ("order");

CREATE TABLE "order" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "customer" VARCHAR (255) NOT NULL,
  "product" VARCHAR (255) NOT NULL,
  "quantity" INTEGER NOT NULL,
  "total" DECIMAL (19, 4) NOT NULL,
  "status" VARCHAR (255) NOT NULL,
  PRIMARY KEY("id")
);

CREATE INDEX "order_customer_ix" ON "order" ("customer");

CREATE TABLE "product" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "name" VARCHAR (255) NOT NULL,
  "description" TEXT,
  "brand" VARCHAR (100) NOT NULL,
  "category" VARCHAR (255) NOT NULL,
  "price" DECIMAL (19, 4) NOT NULL,
  "image_name" VARCHAR (255),
  "image_content_type" VARCHAR (255),
  "image_data" BYTEA,
  "audit_created_at" TIMESTAMP NOT NULL,
  "audit_created_by" VARCHAR (255) NOT NULL,
  "audit_last_modified_at" TIMESTAMP NOT NULL,
  "audit_last_modified_by" VARCHAR (255) NOT NULL,
  PRIMARY KEY("id")
);

CREATE INDEX "product_brand_ix" ON "product" ("brand");

CREATE INDEX "product_category_ix" ON "product" ("category");

CREATE INDEX "product_price_ix" ON "product" ("price");

CREATE INDEX "product_name_description_fts" ON "product" USING GIN ((setweight(to_tsvector('simple', coalesce("name", '')), 'A') || setweight(to_tsvector('simple', coalesce("description", '')), 'D')));

CREATE INDEX "product_name_initials" ON "product" (regexp_replace(lower("name"), '[^[:alnum:]]*([[:alnum:]])[[:alnum:]]*', '\1', 'g') text_pattern_ops);

CREATE INDEX "product_description_initials" ON "product" (regexp_replace(lower("description"), '[^[:alnum:]]*([[:alnum:]])[[:alnum:]]*', '\1', 'g') text_pattern_ops);

CREATE TABLE "product_variant" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "product" VARCHAR (255) NOT NULL,
  "sku" VARCHAR (50) NOT NULL,
  "label" VARCHAR (100) NOT NULL,
  PRIMARY KEY("id")
);

CREATE TABLE "promotion" (
  "id" VARCHAR (255) NOT NULL,
  "type" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "code" VARCHAR (50) NOT NULL,
  "percentage" INTEGER,
  "amount" DECIMAL (19, 4),
  PRIMARY KEY("id")
);

CREATE TABLE "stock_level" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "product" VARCHAR (255) NOT NULL,
  "available" INTEGER NOT NULL,
  "reserved" INTEGER NOT NULL,
  PRIMARY KEY("id")
);

CREATE INDEX "stock_level_product_ix" ON "stock_level" ("product");

