CREATE TABLE "channel"
(
    "id"      VARCHAR(255) NOT NULL,
    "version" BIGINT       NOT NULL,
    "name"    VARCHAR(255) NOT NULL,
    PRIMARY KEY ("id")
);

CREATE TABLE "user"
(
    "id"                    VARCHAR(255)   NOT NULL,
    "version"               BIGINT         NOT NULL,
    "name"                  VARCHAR(255)   NOT NULL,
    "channel_subscriptions" VARCHAR(255)[] NOT NULL,
    PRIMARY KEY ("id")
);

CREATE TABLE "message"
(
    "id"        VARCHAR(255) NOT NULL,
    "version"   BIGINT       NOT NULL,
    "author"    VARCHAR(255) NOT NULL REFERENCES "user" ("id"),
    "channel"   VARCHAR(255) NOT NULL REFERENCES "channel" ("id"),
    "content"   VARCHAR(255),
    "timestamp" TIMESTAMP    NOT NULL,
    PRIMARY KEY ("id")
);

CREATE TABLE "unread_message"
(
    "user"     VARCHAR(255) NOT NULL REFERENCES "user" ("id"),
    "user_key" INTEGER      NOT NULL,
    "message"  VARCHAR(255) REFERENCES "message" ("id"),
    "channel"  VARCHAR(255) REFERENCES "channel" ("id"),
    PRIMARY KEY ("user", "user_key")
);

