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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class QueryDataCollector extends DataCollector {
	
	public static Logger logger = LogManager.getLogger(QueryDataCollector.class.getName());
	public QueryDataCollector(ArrayList<ClientTask> clientTaskList,
			int serverInstanceCount, int clientInstanceCount,
			Properties benchmarkProp, String queryName, String reportPath) {
		super(clientTaskList, serverInstanceCount, clientInstanceCount,
				benchmarkProp, queryName,reportPath);
	}

	@Override
	public void run() {
		int tradedays = Integer.valueOf(benchmarkProp.getProperty("tradedays"));
		long totalVolume = Long.valueOf(benchmarkProp.getProperty("tradevolume")) * clientInstanceCount * tradedays;
		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(queryName+ ".csv", true));
			for (ClientTask clientTask : clientTaskList) {
				String line, last = "";
				//Open result file that is from client instance
				try {
					br = new BufferedReader(new FileReader((queryName) + "_"+ clientTask.getInstance().getInstanceId()));
					while ((line = br.readLine()) != null) {
						last = line;
					}
				} catch (FileNotFoundException e) {
					logger.error("File not exists: " + (queryName) + "_" + clientTask.getInstance().getInstanceId(), e.fillInStackTrace());
					continue;
				} catch (IOException e) {
					logger.error("Unable to read file: " + (queryName) + "_" + clientTask.getInstance().getInstanceId(), e.fillInStackTrace());
					continue;
				}
				
				//Write result into report
				String filePath = reportPath + "/" + queryName + ".csv";
				Boolean addHead = (new File(filePath)).exists();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				String now = sdf.format(new Date());

				if (!addHead) {
					String head = "Server Count,Client Count,Trade Volume,Query Name,Query Duration(s),Record Count,Query Statement,Sitesperhost,Kfactor,Temtablesize,Date";
					bw.write(head);
				}
				
				String result = serverInstanceCount + "," + clientInstanceCount
						+ "," + totalVolume + "," + last + ","
						+ benchmarkProp.getProperty("sitesperhost") + ","
						+ benchmarkProp.getProperty("kfactor") + ","
						+ benchmarkProp.getProperty("temptablesize") + ","
						+ now;
				bw.newLine();
				bw.write(result);
				bw.flush();
			}
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
