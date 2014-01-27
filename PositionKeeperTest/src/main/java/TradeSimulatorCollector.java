import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class TradeSimulatorCollector extends DataCollector {

	public static Logger logger = LogManager.getLogger(TradeSimulatorCollector.class.getName());
	public TradeSimulatorCollector(ArrayList<ClientTask> clientTaskList,
			int serverInstanceCount, int clientInstanceCount, Properties benchmarkProp, String queryName, String reportPath) {
		super(clientTaskList, serverInstanceCount, clientInstanceCount,benchmarkProp,queryName, reportPath);
	}
	
	@Override
	public void run(){
		long totalDuration = 1;
		for(ClientTask clientTask: clientTaskList){
			try {
				BufferedReader br = new BufferedReader(new FileReader((queryName) + "_" + clientTask.getInstance().getInstanceId()));
				String line,last = null;
				while((line = br.readLine())!=null){
					last = line;
				}
				totalDuration += Long.valueOf(last);
			}catch (FileNotFoundException e) {
				logger.error("File not exists: " + (queryName) + "_" + clientTask.getInstance().getInstanceId(), e.fillInStackTrace());
			}catch (Exception e) {
				logger.error("Unable to read file: " + (queryName) + "_" + clientTask.getInstance().getInstanceId(), e.fillInStackTrace());
			}
		}
		int accounts = Integer.valueOf(benchmarkProp.getProperty("accounts"));
		int products = Integer.valueOf(benchmarkProp.getProperty("products"));
		int tradedays = Integer.valueOf(benchmarkProp.getProperty("tradedays"));
		long totalVolume = Long.valueOf(benchmarkProp.getProperty("tradevolume"))*clientInstanceCount*tradedays;
		int avgThroughput = (int) (totalVolume/(totalDuration/1000.0));
		System.out.print(totalVolume);
		System.out.print(totalDuration/1000.0);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String now = sdf.format(new Date());
		BufferedWriter bw = null;
		try {
			String filePath = reportPath + "/" + queryName + ".csv";
			Boolean addHead = (new File(filePath)).exists();
			bw = new BufferedWriter(new FileWriter(filePath,true));
			if(!addHead){
				String head = "Server Count,Client Count,Avg Through Put(txn/s),Trade Volume,Account Count,Product Count,Trade Days,Sitesperhost,Kfactor,Temtablesize,Date";
				bw.write(head);
			}
			String result = serverInstanceCount + ","
						+ clientInstanceCount + ","
						+ avgThroughput + ","
						+ totalVolume + ","
						+ accounts + ","
						+ products + ","
						+ tradedays + ","
						+ benchmarkProp.getProperty("sitesperhost") + ","
						+ benchmarkProp.getProperty("kfactor") + ","
						+ benchmarkProp.getProperty("temptablesize") + ","
						+ now;
			bw.newLine();
			bw.write(result);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			logger.error("Unable to generate report for query: " + queryName, e.fillInStackTrace());
		}
		finally{
			if(bw!=null){
				try {
					bw.close();
				} catch (IOException e) {
					logger.error("Unable to close report file for query: " + queryName, e.fillInStackTrace());
				}
			}
		}
	}

}
