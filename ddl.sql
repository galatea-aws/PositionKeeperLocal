CREATE TABLE trades
(
	trade_id        BIGINT       NOT NULL,
	account_id      varchar(9)   NOT NULL,
	product_cusip   char(9)      NOT NULL,
	knowledge_date  TIMESTAMP    NOT NULL,
	effective_date  TIMESTAMP    NOT NULL,
	position_delta	BIGINT		 NOT NULL,
);

PARTITION TABLE trades ON COLUMN account_id;

CREATE TABLE accounts
(
	account_id		varchar(50) NOT NULL,
	name			varchar(50) NOT NULL,
	address			varchar(50) NOT NULL,
	tin             varchar(50) NOT NULL,
	PRIMARY KEY(account_id)
);

PARTITION TABLE accounts ON COLUMN account_id;

CREATE TABLE products
(
	product_cusip	char(9)     NOT NULL,
	product_name	varchar(50) NOT NULL,
	product_isin    char(12)    NOT NULL,
	product_ric     varchar(9)  NOT NULL,
	product_ticker  varchar(6)  NOT NULL,
	product_ccy     char(3)     NOT NULL,
	product_coi     char(3)     NOT NULL,
	PRIMARY KEY(product_cusip)
);

PARTITION TABLE products ON COLUMN product_cusip;

-- stored procedures
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.Initialize;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.DoTrade;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.CountTradesByAccount;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.SumPositionByAccount;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.SumPositionByAccountAndProduct;