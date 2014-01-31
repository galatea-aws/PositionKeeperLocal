CREATE TABLE trades
(
	trade_id       	      BIGINT       NOT NULL,
	account_id    		  varchar(9)   NOT NULL,
	product_cusip         varchar(9)   NOT NULL,
	exchange		      varchar(2)   NOT NULL,
	status                varchar(1)   NOT NULL,
	sourcesystem_id       varchar(50)  NOT NULL,
	knowledge_date        TIMESTAMP    NOT NULL,
	effective_date        TIMESTAMP    NOT NULL,
	settlement_date       TIMESTAMP    NOT NULL,
	position_delta	      BIGINT	   NOT NULL,
	create_user           varchar(50)  NOT NULL,
	create_timestamp      TIMESTAMP    NOT NULL,
	last_update_user      varchar(50)  NOT NULL,
	last_update_timestamp TIMESTAMP    NOT NULL,
);

CREATE TABLE accounts
(
	account_id	          varchar(9)  NOT NULL,
	account_name	      varchar(50)  NOT NULL,
	account_address	      varchar(50)  NOT NULL,
	account_tin		      varchar(20)  NOT NULL,
	create_user           varchar(50)  NOT NULL,
	create_timestamp      TIMESTAMP    NOT NULL,
	last_update_user      varchar(50)  NOT NULL,
	last_update_timestamp TIMESTAMP    NOT NULL,
	PRIMARY KEY(account_id)
);

CREATE TABLE products
(
	product_cusip	      varchar(9)  NOT NULL,
	product_name	      varchar(50)  NOT NULL,
	product_isin	      varchar(12)  NOT NULL,
	prodcut_ticker	      varchar(6)   NOT NULL,
	prodcut_ric	          varchar(9)   NOT NULL,
	prodcut_ccy	          varchar(3)   NOT NULL,
	prodcut_coi           varchar(3)   NOT NULL,
	create_user           varchar(50)  NOT NULL,
	create_timestamp      TIMESTAMP    NOT NULL,
	last_update_user      varchar(50)  NOT NULL,
	last_update_timestamp TIMESTAMP    NOT NULL,
	PRIMARY KEY(product_cusip)
);

-- stored procedures
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.Initialize;
--INSERT INTO trades "
--(trade_id, account_id, product_cusip, exchange, status, sourcesystem_id,
--knowledge_date, effective_date, settlement_date, position_delta,
--create_user, create_timestamp, last_update_user, last_update_timestamp)
--VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.DoTrade;
--SELECT product_cusip from products where product_isin = ?
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.GetProductCusipByIsin;
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.CountTradesByAccount;
--SELECT sum(position_delta) from trades where product_cusip = ? and account_id = ?
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.SumPositionByAccountAndProduct;
--SELECT product_cusip,sum(position_delta) FROM trades WHERE account_id = ? GROUP BY product_cusip
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.SumPositionForAccountGroupByProduct;
--SELECT account_id,sum(position_delta) FROM trades WHERE product_cusip = ? GROUP BY account_id
CREATE PROCEDURE FROM CLASS PositionKeeper.procedures.SumPositionForProductGroupByAccount;
