CREATE TABLE gift_voucher (
  id VARCHAR (255) NOT NULL,
  version BIGINT NOT NULL,
  code VARCHAR (255) NOT NULL,
  remaining_value DECIMAL (19, 4) NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE invoice (
  id VARCHAR (255) NOT NULL,
  version BIGINT NOT NULL,
  total DECIMAL (19, 4),
  PRIMARY KEY(id)
);

CREATE TABLE todo (
  id VARCHAR (255) NOT NULL,
  version BIGINT NOT NULL,
  description VARCHAR (255) NOT NULL,
  done BOOLEAN NOT NULL,
  created TIMESTAMP NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE customer (
  id VARCHAR (255) NOT NULL,
  version BIGINT NOT NULL,
  name_first_name VARCHAR (255) NOT NULL,
  name_last_name VARCHAR (255) NOT NULL,
  address_street VARCHAR (255) NOT NULL,
  address_number VARCHAR (255),
  address_postal_code VARCHAR (255) NOT NULL,
  address_city VARCHAR (255) NOT NULL,
  address_country VARCHAR (255) NOT NULL,
  email VARCHAR (255) NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE sale (
  id VARCHAR (255) NOT NULL,
  version BIGINT NOT NULL,
  start TIMESTAMP NOT NULL,
  returned DECIMAL (19, 4) NOT NULL,
  state VARCHAR (255) NOT NULL,
  customer VARCHAR (255) REFERENCES customer(id),
  type VARCHAR (255),
  PRIMARY KEY(id)
);

CREATE TABLE sale_item (
  sale VARCHAR (255) NOT NULL REFERENCES sale(id),
  sale_key INTEGER NOT NULL,
  description VARCHAR (255) NOT NULL,
  quantity DECIMAL (19, 4) NOT NULL,
  price DECIMAL (19, 4) NOT NULL,
  PRIMARY KEY(sale, sale_key)
);

CREATE TABLE payment (
  sale VARCHAR (255) NOT NULL REFERENCES sale(id),
  sale_key INTEGER NOT NULL,
  amount DECIMAL (19, 4) NOT NULL,
  method VARCHAR (255) NOT NULL,
  gift_voucher VARCHAR (255) REFERENCES gift_voucher(id),
  PRIMARY KEY(sale, sale_key)
);

