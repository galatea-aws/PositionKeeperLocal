import java.util.LinkedList;
import java.util.Queue;


public class BenchmarkCoordinator implements Runnable{
	
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
