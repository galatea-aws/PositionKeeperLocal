package PositionKeeper;

import java.util.Date;
import java.util.Random;

public class TradeGenerator {
	
	private int maxAccounts = 0;
	private int maxProducts = 0;
	private Random random = new Random();
    public static class Trade {
        public String productcusip;
        public String productName;
        public Date knowledgeDate;
        public Date effectiveDate;
        public long positionDelta;
        public String accountId;
        public long tradeId;
        protected Trade(String accountId, long tradeId, String productcusip, String productName, Date knowledgeDate, Date effectiveDate, long positionDelta) {
        	this.accountId = accountId;
            this.tradeId = tradeId;
            this.productcusip = productcusip;
            this.productName = productName;
            this.knowledgeDate = knowledgeDate;
            this.effectiveDate = effectiveDate;
            this.positionDelta = positionDelta;
        }
    }
    
    public TradeGenerator(int maxAccounts, int maxProducts){
    	this.maxAccounts = maxAccounts;
    	this.maxProducts = maxProducts;
    }
    
    public Trade CreateTrade(long tradeId, Date knowledgeDate, Date effectiveDate){
    	int productId = random.nextInt(maxProducts)+1;
    	String productcusip = "cusip" + productId;
        String productName = "product" + productId;
        long positionDelta = random.nextInt(1000)*(random.nextBoolean()?1:-1);
        String accountId = "account" + (random.nextInt(maxAccounts)+1);
    	return new Trade(accountId,tradeId,productcusip, productName, knowledgeDate, effectiveDate, positionDelta);
    }
}
