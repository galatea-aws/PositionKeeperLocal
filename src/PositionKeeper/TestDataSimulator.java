/* This file is part of VoltDB.
 * Copyright (C) 2008-2013 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * This samples uses the native asynchronous request processing protocol
 * to post requests to the VoltDB server, thus leveraging to the maximum
 * VoltDB's ability to run requests in parallel on multiple database
 * partitions, and multiple servers.
 *
 * While asynchronous processing is (marginally) more convoluted to work
 * with and not adapted to all workloads, it is the preferred interaction
 * model to VoltDB as it allows a single client with a small amount of
 * threads to flood VoltDB with requests, guaranteeing blazing throughput
 * performance.
 *
 * Note that this benchmark focuses on throughput performance and
 * not low latency performance.  This benchmark will likely 'firehose'
 * the database cluster (if the cluster is too slow or has too few CPUs)
 * and as a result, queue a significant amount of requests on the server
 * to maximize throughput measurement. To test VoltDB latency, run the
 * SyncBenchmark client, also found in the voter sample directory.
 */

package PositionKeeper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.voltdb.CLIConfig;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ClientStats;
import org.voltdb.client.ClientStatsContext;
import org.voltdb.client.ClientStatusListenerExt;
import org.voltdb.client.NullCallback;
import org.voltdb.client.ProcedureCallback;

import PositionKeeper.procedures.DoTrade;;

public class TestDataSimulator {

    // handy, rather than typing this out several times
    static final String HORIZONTAL_RULE =
            "----------" + "----------" + "----------" + "----------" +
            "----------" + "----------" + "----------" + "----------" + "\n";

    // Reference to the database connection we will use
    final Client client;
    // Trade generator
    TradeGenerator switchboard;
    // Timer for periodic stats printing
    Timer timer;
    // Benchmark start time
    long simulatorStartTS;
    // Statistics manager objects from the client
    final ClientStatsContext periodicStatsContext;
    final ClientStatsContext fullStatsContext;
    public Properties serverProp;
    public Properties tradesimulatorProp;
    
    AtomicLong acceptedTrades = new AtomicLong(0);

    /**
     * Uses included {@link CLIConfig} class to
     * declaratively state command line options with defaults
     * and validation.
     */
    static class TradeConfig extends CLIConfig {
    }

    /**
     * Provides a callback to be notified on node failure.
     * This example only logs the event.
     */
    class StatusListener extends ClientStatusListenerExt {
        @Override
        public void connectionLost(String hostname, int port, int connectionsLeft, DisconnectCause cause) {
            // if the simulator is still active
/*            if ((System.currentTimeMillis() - simulatorStartTS) < (config.duration * 1000)) {
                System.err.printf("Connection to %s:%d was lost.\n", hostname, port);
            }*/
        }
    }

    /**
     * Constructor for simulator instance.
     * Configures VoltDB client and prints configuration.
     *
     * @param config Parsed & validated CLI options.
     */
    public TestDataSimulator() {
    	tradesimulatorProp = new Properties();
		serverProp = new Properties();
    	try {
            //load a properties file
    		tradesimulatorProp.load(new FileInputStream("tradesimulatorconfig.properties"));
    		serverProp.load(new FileInputStream("serverconfig.properties"));
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
    	
    	String user = serverProp.getProperty("user");
    	String password = serverProp.getProperty("password");
        ClientConfig clientConfig = new ClientConfig(user, password, new StatusListener());
//      clientConfig.setMaxTransactionsPerSecond(config.ratelimit);

        client = ClientFactory.createClient(clientConfig);

        periodicStatsContext = client.createStatsContext();
        fullStatsContext = client.createStatsContext();

        Integer accounts = Integer.valueOf(tradesimulatorProp.getProperty("accounts"));
        Integer products = Integer.valueOf(tradesimulatorProp.getProperty("products"));
        switchboard = new TradeGenerator(accounts, products);

        System.out.print(HORIZONTAL_RULE);
        System.out.println(" Command Line Configuration");
        System.out.println(HORIZONTAL_RULE);
        System.out.println(tradesimulatorProp.toString());
/*        if(config.latencyreport) {
            System.out.println("NOTICE: Option latencyreport is ON for async run, please set a reasonable ratelimit.\n");
        }*/
    }

    /**
     * Connect to a single server with retry. Limited exponential backoff.
     * No timeout. This will run until the process is killed if it's not
     * able to connect.
     *
     * @param server hostname:port or just hostname (hostname can be ip).
     */
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
     * Connect to a set of servers in parallel. Each will retry until
     * connection. This call will block until all have connected.
     *
     * @param servers A comma separated list of servers using the hostname:port
     * syntax (where :port is optional).
     * @throws InterruptedException if anything bad happens with the threads.
     */
    void connect() throws InterruptedException {
        System.out.println("Connecting to VoltDB...");

		String servers = serverProp.getProperty("servers");
        String[] serverArray = servers.split(",");
        final CountDownLatch connections = new CountDownLatch(serverArray.length);
		final String clientport = serverProp.getProperty("clientport");
		
        // use a new thread to connect to each server
        for (final String server : serverArray) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectToOneServerWithRetry(server + ":" +clientport);
                    connections.countDown();
                }
            }).start();
        }
        // block until all have connected
        connections.await();
    }

    /**
     * Create a Timer task to display performance data on the Vote procedure
     * It calls printStatistics() every displayInterval seconds
     */
    public void schedulePeriodicStats() {
        timer = new Timer();
        TimerTask statsPrinting = new TimerTask() {
            @Override
            public void run() { printStatistics(); }
        };
        
        Long displayinterval = Long.valueOf(tradesimulatorProp.getProperty("displayinterval"));
        timer.scheduleAtFixedRate(statsPrinting,
                                  displayinterval * 1000,
                                  displayinterval * 1000);
    }

    /**
     * Prints a one line update on performance that can be printed
     * periodically during a simulator.
     */
    public synchronized void printStatistics() {
        ClientStats stats = periodicStatsContext.fetchAndResetBaseline().getStats();
        long time = Math.round((stats.getEndTimestamp() - simulatorStartTS) / 1000.0);

        System.out.printf("%02d:%02d:%02d ", time / 3600, (time / 60) % 60, time % 60);
        System.out.printf("Throughput %d/s, ", stats.getTxnThroughput());
        System.out.printf("Aborts/Failures %d/%d",
                stats.getInvocationAborts(), stats.getInvocationErrors());
/*        if(this.config.latencyreport) {
            System.out.printf(", Avg/95%% Latency %.2f/%dms", stats.getAverageLatency(),
                stats.kPercentileLatency(0.95));
        }*/
        System.out.printf("\n");
    }

    /**
     * Prints the results of the voting simulation and statistics
     * about performance.
     *
     * @throws Exception if anything unexpected happens.
     */
    public synchronized void printResults() throws Exception {
        ClientStats stats = fullStatsContext.fetch().getStats();

        String display = "\n" +
                         HORIZONTAL_RULE +
                         " Generating Trades Results\n" +
                         HORIZONTAL_RULE +
                         "\nA total of %d trades were received...\n" +
                         " - %d Accepted\n";
        System.out.printf(display, stats.getInvocationsCompleted(),
                acceptedTrades.get());

        System.out.print(HORIZONTAL_RULE);
        System.out.println(" Client Workload Statistics");
        System.out.println(HORIZONTAL_RULE);

        System.out.printf("Average throughput:%d txns/sec\n", stats.getTxnThroughput());
        System.out.printf("%d", (stats.getEndTimestamp() - simulatorStartTS));
/*        if(this.config.latencyreport) {
            System.out.printf("Average latency:               %,9.2f ms\n", stats.getAverageLatency());
            System.out.printf("10th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.1));
            System.out.printf("25th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.25));
            System.out.printf("50th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.5));
            System.out.printf("75th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.75));
            System.out.printf("90th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.9));
            System.out.printf("95th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.95));
            System.out.printf("99th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.99));
            System.out.printf("99.5th percentile latency:     %,9d ms\n", stats.kPercentileLatency(.995));
            System.out.printf("99.9th percentile latency:     %,9d ms\n", stats.kPercentileLatency(.999));

            System.out.print("\n" + HORIZONTAL_RULE);
            System.out.println(" System Server Statistics");
            System.out.println(HORIZONTAL_RULE);
            System.out.printf("Reported Internal Avg Latency: %,9.2f ms\n", stats.getAverageInternalLatency());

            System.out.print("\n" + HORIZONTAL_RULE);
            System.out.println(" Latency Histogram");
            System.out.println(HORIZONTAL_RULE);
            System.out.println(stats.latencyHistoReport());
        }*/
        String statsfile = tradesimulatorProp.getProperty("statsfile");
        client.writeSummaryCSV(stats, statsfile);
    }

    /**
     * Callback to handle the response to a stored procedure call.
     * Tracks response types.
     *
     */
    class TradeCallback implements ProcedureCallback {
        @Override
        public void clientCallback(ClientResponse response) throws Exception {
        	 acceptedTrades.incrementAndGet();
        }
    }

    /**
     * Core simulator code.
     * Connect. Initialize. Run the loop. Cleanup. Print Results.
     *
     * @throws Exception if anything unexpected happens.
     */
    public void runSimulator() throws Exception {
        System.out.print(HORIZONTAL_RULE);
        System.out.println(" Setup & Initialization");
        System.out.println(HORIZONTAL_RULE);

        // connect to one or more servers, loop until success
        connect();

        // initialize using synchronous call
        System.out.println("\nPopulating Static Tables\n");
        
        Integer accounts = Integer.valueOf(tradesimulatorProp.getProperty("accounts"));
        Integer products = Integer.valueOf(tradesimulatorProp.getProperty("products"));
        
        client.callProcedure("Initialize", accounts, products);

        System.out.print(HORIZONTAL_RULE);
        System.out.println(" Starting Simulator");
        System.out.println(HORIZONTAL_RULE);

        // print periodic statistics to the console
        schedulePeriodicStats();
        
        System.out.println("\nRunning Simulator...");
        
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        
        //Set the current knowledge date and effective date
        Integer tradedays = Integer.valueOf(tradesimulatorProp.getProperty("tradedays"));
        calendar.add(Calendar.DAY_OF_YEAR,-1*tradedays);
        Date currentDate = calendar.getTime();
        
        Integer tradevolume = Integer.valueOf(tradesimulatorProp.getProperty("tradevolume"));
        simulatorStartTS = System.currentTimeMillis();
        long tradeId = 1;
        while (endDate.before(currentDate)) {
        	for(int i=0;i<tradevolume;i++){
                // Get the next phone call
                TradeGenerator.Trade trade = switchboard.CreateTrade(tradeId++,currentDate, currentDate);
                // asynchronously call the "Vote" procedure
                client.callProcedure(new TradeCallback(),
                                     "DoTrade",
                                     trade.accountId,
                                     trade.tradeId,
                                     trade.productcusip,
                                     trade.knowledgeDate,
                                     trade.effectiveDate,
                                     trade.positionDelta
                					);
        	}
        	calendar.add(Calendar.DAY_OF_YEAR,1);
			currentDate = calendar.getTime();
        }

        // cancel periodic stats printing
        timer.cancel();

        // block until all outstanding txns return
        client.drain();

        // print the summary results
        printResults();

        // close down the client connections
        client.close();
    }

    /**
     * Main routine creates a Simulator instance and kicks off the run method.
     *
     * @param args Command line arguments.
     * @throws Exception if anything goes wrong.
     * @see {@link TradeConfig}
     */
    public static void main(String[] args) throws Exception {
        TestDataSimulator simulator = new TestDataSimulator();
        simulator.runSimulator();
    }
}
