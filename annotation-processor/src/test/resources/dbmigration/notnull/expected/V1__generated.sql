CREATE TABLE "prefab_outbox" (
  "sequence_num" BIGSERIAL NOT NULL,
  "id" VARCHAR (36) NOT NULL,
  "aggregate_type" VARCHAR (255) NOT NULL,
  "aggregate_id" VARCHAR (255) NOT NULL,
  "event_type" VARCHAR (255) NOT NULL,
  "payload" TEXT NOT NULL,
  "created_at" TIMESTAMP NOT NULL,
  "published_at" TIMESTAMP,
  PRIMARY KEY("id")
);

CREATE TABLE "product" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "name" VARCHAR (255) NOT NULL,
  "description" VARCHAR (255),
  PRIMARY KEY("id")
);

