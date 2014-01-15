package PositionKeeper;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientStatusListenerExt;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

import PositionKeeper.TestDataSimulator.TradeConfig;

public class VoltPerformanceTester {
    // Reference to the database connection we will use
    final Client client;
    public Properties procedureProp;
    public Properties serverProp;
    
    public VoltPerformanceTester(){
		procedureProp = new Properties();
		serverProp = new Properties();
    	try {
            //load a properties file
    		procedureProp.load(new FileInputStream("procedureconfig.properties"));
    		serverProp.load(new FileInputStream("serverconfig.properties"));
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
    	
    	String user = serverProp.getProperty("user");
    	String password = serverProp.getProperty("password");
		ClientConfig clientConfig = new ClientConfig(user, password, new ClientStatusListenerExt());
		/*         clientConfig.setMaxTransactionsPerSecond(config.ratelimit);*/
		client = ClientFactory.createClient(clientConfig);
    }
    
    
    //Connect to Servers
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
    
    /**
     * Core simulator code.
     * Connect. Run. Print Results.
     *
     * @throws Exception if anything unexpected happens.
     */
    public void run() throws NoConnectionsException, IOException, ProcCallException, InterruptedException{
    }
}
