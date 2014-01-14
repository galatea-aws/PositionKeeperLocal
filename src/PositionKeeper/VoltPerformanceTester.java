package PositionKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientStatusListenerExt;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

import PositionKeeper.AsyncBenchmark.TradeConfig;

public class VoltPerformanceTester {
	
    // validated command line configuration
    final TradeConfig config;
    // Reference to the database connection we will use
    final Client client;
    
    public VoltPerformanceTester(TradeConfig tradeconfig){
    	 config = tradeconfig;
         ClientConfig clientConfig = new ClientConfig(config.user, config.password, new ClientStatusListenerExt());
         clientConfig.setMaxTransactionsPerSecond(config.ratelimit);

         client = ClientFactory.createClient(clientConfig);
    }
    
    void connect(String servers) throws InterruptedException {
        System.out.println("Connecting to VoltDB...");

        String[] serverArray = servers.split(",");
        final CountDownLatch connections = new CountDownLatch(serverArray.length);

        // use a new thread to connect to each server
        for (final String server : serverArray) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectToOneServerWithRetry(server);
                    connections.countDown();
                }
            }).start();
        }
        // block until all have connected
        connections.await();
    }
    
    void connectToOneServerWithRetry(String server) {
        int sleep = 1000;
        while (true) {
            try {
                client.createConnection(server);
                break;
            }
            catch (Exception e) {
                System.err.printf("Connection failed - retrying in %d second(s).\n", sleep / 1000);
                try { Thread.sleep(sleep); } catch (Exception interruted) {}
                if (sleep < 8000) sleep += sleep;
            }
        }
        System.out.printf("Connected to VoltDB node at: %s.\n", server);
    }
    
    public void run() throws NoConnectionsException, IOException, ProcCallException, InterruptedException{
        connect(config.servers);
        
    	long benchmarkStartTS = System.currentTimeMillis();
        
    	VoltTable result = client.callProcedure("CountTradesByAccount",
    			"account2").getResults()[0];
    	  
        while(result.advanceRow()) {
            System.out.println("Count: " + result.getLong(0));
            System.out.println((double)(System.currentTimeMillis()-benchmarkStartTS)/1000f + "s");
        }  

        // block until all outstanding txns return
        client.drain();


        // close down the client connections
        client.close();
        

    }
    public static void main(String[] args) throws Exception {
        // create a configuration from the arguments
        TradeConfig config = new TradeConfig();
        config.parse(AsyncBenchmark.class.getName(), args);
        
        VoltPerformanceTester tester = new VoltPerformanceTester(config);
        tester.run();
    }
}
