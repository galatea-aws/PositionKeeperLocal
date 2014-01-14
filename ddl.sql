CREATE TABLE trades
(
	trade_id        BIGINT       NOT NULL,
	account_id      varchar(50)  NOT NULL,
	product_cusip   varchar(50)  NOT NULL,
	knowledge_date  TIMESTAMP    NOT NULL,
	effective_date  TIMESTAMP    NOT NULL,
	position_delta	BIGINT		 NOT NULL,
);

PARTITION TABLE trades ON COLUMN account_id;

CREATE TABLE accounts
(
	account_id		varchar(50) NOT NULL,
	PRIMARY KEY(account_id)
);

PARTITION TABLE accounts ON COLUMN account_id;

CREATE TABLE products
(
	product_cusip	varchar(50) NOT NULL,
	product_name	varchar(50) NOT NULL,
	PRIMARY KEY(product_cusip)
);

PARTITION TABLE products ON COLUMN product_cusip;

-- stored procedures
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.Initialize;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.DoTrade;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.CountTradesByAccount;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.SumPositionByAccount;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.SumPositionByAccountAndProduct;