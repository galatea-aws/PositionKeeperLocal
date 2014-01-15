package PositionKeeper;

import java.io.IOException;

import org.voltdb.VoltTable;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

import PositionKeeper.TestDataSimulator.TradeConfig;

public class CountTradesByAccountTester extends VoltPerformanceTester{

	public CountTradesByAccountTester() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	@Override
    public void run() throws NoConnectionsException, IOException, ProcCallException, InterruptedException{
		String servers = serverProp.getProperty("clienthost");
        connect(servers);
        
    	long queryStartTS = System.currentTimeMillis();

    	String accountId = procedureProp.getProperty("CountTradesByAccount.accountid","account2");
    	
    	System.out.println("Call Procedure: CountTradesByAccount");
    	System.out.println("accountId = " + accountId);
    	
    	VoltTable result = client.callProcedure("CountTradesByAccount",
    			accountId).getResults()[0];
    	  
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
        // create a configuration from the arguments
        TradeConfig config = new TradeConfig();
        config.parse(TestDataSimulator.TradeConfig.class.getName(), args);
        
        CountTradesByAccountTester tester = new CountTradesByAccountTester();
        tester.run();
    }

}
