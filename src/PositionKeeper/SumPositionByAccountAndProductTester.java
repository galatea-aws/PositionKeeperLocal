package PositionKeeper;

import java.io.IOException;

import org.voltdb.VoltTable;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

import PositionKeeper.TestDataSimulator.TradeConfig;

public class SumPositionByAccountAndProductTester  extends VoltPerformanceTester{

	public SumPositionByAccountAndProductTester() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	@Override
    public void run() throws NoConnectionsException, IOException, ProcCallException, InterruptedException{
        connect();
        
    	long queryStartTS = System.currentTimeMillis();

    	String accountId = procedureProp.getProperty("SumPositionByAccountAndProduct.accountid","account1");
    	String productCusip = procedureProp.getProperty("SumPositionByAccountAndProduct.productcusip","cusip1");
    	
    	VoltTable result = client.callProcedure("SumPositionByAccountAndProduct",
    			accountId,
    			productCusip).getResults()[0];
    	
    	String queryDuration = String.valueOf((double)(System.currentTimeMillis()-queryStartTS)/1000f);
        while(result.advanceRow()) {
            String output = "SumPositionByAccountAndProduct," + accountId + "," + productCusip + "," + result.getLong(0) + "," + queryDuration;
            System.out.println(output);
        }

        // block until all outstanding txns return
        client.drain();

        // close down the client connections
        client.close();
    }
	
    public static void main(String[] args) throws Exception {
        
        SumPositionByAccountAndProductTester tester = new SumPositionByAccountAndProductTester();
        tester.run();
    }
}
