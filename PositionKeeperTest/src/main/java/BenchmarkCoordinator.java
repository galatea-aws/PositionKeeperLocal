import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class BenchmarkCoordinator implements Runnable{
	public static Logger logger = LogManager.getLogger(BenchmarkCoordinator.class.getName());
	
	public boolean waitForBenmarkStart;
	public boolean keepRunning = true;
	public PositionKeeperBenchmark currentBenchmark;
	public Queue<PositionKeeperBenchmark> benchmarkQueue = new LinkedList<PositionKeeperBenchmark>();
	
	public synchronized void addBenchmark(PositionKeeperBenchmark pb){
		benchmarkQueue.add(pb);
	}
	
	@Override
	public void run() {
		while(keepRunning){
			if(currentBenchmark!=null){
				logger.info("no current benchmark");
				while(currentBenchmark.isStartingServer){
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				if(benchmarkQueue.size()>0){
					currentBenchmark = benchmarkQueue.remove();
					currentBenchmark.isStartingServer = true;
					currentBenchmark.waitingForOtherBenchmark = false;
				}
				else
					currentBenchmark = null;
			}
			else{
				if(benchmarkQueue.size()>0){
					currentBenchmark = benchmarkQueue.remove();
					currentBenchmark.isStartingServer = true;
					currentBenchmark.waitingForOtherBenchmark = false;
				}
			}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
