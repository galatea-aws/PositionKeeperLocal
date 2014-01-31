PARTITION PROCEDURE DoTrade ON TABLE trades COLUMN product_cusip PARAMETER 2;
PARTITION PROCEDURE SumPositionByAccountAndProduct ON TABLE trades COLUMN product_cusip;
PARTITION PROCEDURE SumPositionForProductGroupByAccount ON TABLE trades COLUMN product_cusip;