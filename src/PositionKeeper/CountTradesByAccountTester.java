package PositionKeeper;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
        connect();
        
    	long queryStartTS = System.currentTimeMillis();

    	String accountId = procedureProp.getProperty("CountTradesByAccount.accountid","account2");
    	
    	System.out.println("Call Procedure: CountTradesByAccount");
    	System.out.println("accountId = " + accountId);
    	
    	VoltTable result = client.callProcedure("CountTradesByAccount",
    			accountId).getResults()[0];
    	
    	long queryDuration = 0;
        while(result.advanceRow()) {
        	queryDuration = System.currentTimeMillis()-queryStartTS;
            System.out.println("Result: " + result.getLong(0));
            System.out.println((double)(queryDuration/1000f) + "s");
        }

        // block until all outstanding txns return
        client.drain();
        // close down the client connections
        client.close();
        
        BufferedWriter bw = new BufferedWriter(new FileWriter ("querytester"));
        bw.write(String.valueOf(queryDuration));
        bw.flush();
        bw.close();
    }
	
    
    public static void main(String[] args) throws Exception {
    	
        CountTradesByAccountTester tester = new CountTradesByAccountTester();
        tester.run();
    }

}
