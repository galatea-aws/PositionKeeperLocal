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
		String servers = serverProp.getProperty("clienthost");
        connect(servers);
        
    	long queryStartTS = System.currentTimeMillis();

    	String accountId = procedureProp.getProperty("SumPositionByAccountAndProduct.accountid","account1");
    	String productCusip = procedureProp.getProperty("SumPositionByAccountAndProduct.productcusip","cusip1");
    	
    	System.out.println("Call Procedure: SumPositionByAccountAndProduct");
    	System.out.println("accountId = " + accountId);
    	System.out.println("productCusip = " + productCusip);
    	
    	VoltTable result = client.callProcedure("SumPositionByAccountAndProduct",
    			accountId,
    			productCusip).getResults()[0];
    	  
        while(result.advanceRow()) {
            System.out.println("Result: " + result.getLong(0));
            System.out.println((double)(System.currentTimeMillis()-queryStartTS)/1000f + "s");
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
