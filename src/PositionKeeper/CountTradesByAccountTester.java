package PositionKeeper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.voltdb.VoltTable;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

import PositionKeeper.TestDataSimulator.TradeConfig;
import PositionKeeper.procedures.CountTradesByAccount;
import PositionKeeper.procedures.SumPositionByAccountAndProduct;

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
    	
    	VoltTable result = client.callProcedure("CountTradesByAccount",
    			accountId).getResults()[0];
    	
    	String queryDuration = String.valueOf((double)(System.currentTimeMillis()-queryStartTS)/1000f);
        while(result.advanceRow()) {
            String output = "SumPositionByAccountAndProduct," + queryDuration + "," + result.getRowCount() + "," + CountTradesByAccount.resultStmt.getText();
            System.out.println(output);
        }

        // block until all outstanding txns return
        client.drain();
        // close down the client connections
        client.close();
    }
	
    
    public static void main(String[] args) throws Exception {
    	
        CountTradesByAccountTester tester = new CountTradesByAccountTester();
        tester.run();
    }

}
