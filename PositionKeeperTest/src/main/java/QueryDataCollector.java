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
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.joda.time.DateTime;

public class QueryDataCollector extends DataCollector {
	
	public static Logger logger = LogManager.getLogger(QueryDataCollector.class.getName());
	public QueryDataCollector(ArrayList<ClientTask> clientTaskList,
			int serverInstanceCount,Properties benchmarkProp, String queryName, String reportPath, String gitRevision, String uuid) {
		super(clientTaskList, serverInstanceCount,
				benchmarkProp, queryName,reportPath, gitRevision, uuid);
	}

/*	public void run() {
		int tradedays = Integer.valueOf(benchmarkProp.getProperty("tradedays"));
		long totalVolume = Long.valueOf(benchmarkProp.getProperty("tradevolume")) * clientInstanceCount * tradedays;
		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			Boolean addHead = (new File(getReportFilePath())).exists();
			bw = new BufferedWriter(new FileWriter(getReportFilePath(), true));
			for (ClientTask clientTask : clientTaskList) {
				String line, last = "";
				String clientResultFilePath = (queryName) + "_"+ clientTask.getInstance().getInstanceId();
				//Open result file that is from client instance
				try {
					br = new BufferedReader(new FileReader(clientResultFilePath));
					while ((line = br.readLine()) != null) {
						last = line;
					}
				} catch (FileNotFoundException e) {
					logger.error("File not exists: " + clientResultFilePath, e.fillInStackTrace());
					continue;
				} catch (IOException e) {
					logger.error("Unable to read file: " + clientResultFilePath, e.fillInStackTrace());
					continue;
				}
				
				//Write result into report
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
			}
			bw.flush();
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
	}*/
	
	@Override
	public void addHead(){
		if(clientTaskList.size()>0){
			BufferedReader br = null;
			String line = null;
			String queryStatement = "";
			String queryProcedureName = "";
			ClientTask clientTask = clientTaskList.get(0);
			String clientResultFilePath = uuid + "_" + (queryName) + "_"+ clientTask.getInstance().getInstanceId();
			HSSFCellStyle style = workbook.createCellStyle();
			style.setWrapText(true);

			//Open result file that is from client instance
			try {
				br = new BufferedReader(new FileReader(clientResultFilePath));
				if((line = br.readLine())!=null)
					queryProcedureName = line;
				if((line = br.readLine())!=null)
					queryStatement = line;
				
				HSSFRow row = sheet.createRow(0);
				Cell cell = row.createCell(0);
				cell.setCellValue("QueryName: ");
				cell = row.createCell(1);
				cell.setCellValue(queryProcedureName);
				
				row = sheet.createRow(1);
				cell = row.createCell(0);
				cell.setCellValue("QueryStatement: ");
				
				sheet.addMergedRegion(new CellRangeAddress(1,1,1,12));
				cell = row.createCell(1);
				cell.setCellValue(queryStatement);
				cell.setCellStyle(style);
				
				row = sheet.createRow(2);
				cell = row.createCell(0);
				cell.setCellValue("Instance Type: ");
				
				cell = row.createCell(1);
				cell.setCellValue(benchmarkProp.getProperty("instancetype"));
				cell.setCellStyle(style);
				
				sheet.createRow(4);
				sheet.createRow(5);
				
				int cellnum = 0;
				row = sheet.createRow(6);
				String[] columnName = new String[]{"Server Count","Client Count","Trade Volume","Query Duration(s)",
						"Record Count","Sitesperhost","Kfactor","Temtablesize","Date", "Table Parition", "Table Index", "Git Revision"};
				
				for(String s: columnName){
					cell = row.createCell(cellnum++);
					cell.setCellValue(s);
				}
			} catch (FileNotFoundException e) {
				logger.error("File not exists: " + clientResultFilePath, e.fillInStackTrace());
			} catch (IOException e) {
				logger.error("Unable to read file: " + clientResultFilePath, e.fillInStackTrace());
			}
		}

	}
	
	@Override
	public void writeQueryResult(){
		int tradedays = Integer.valueOf(benchmarkProp.getProperty("tradedays"));
		long totalVolume = Long.valueOf(benchmarkProp.getProperty("tradevolume")) * clientInstanceCount * tradedays;
		for (ClientTask clientTask : clientTaskList) {
			BufferedReader br = null;
			String line = null;
			String clientResultFilePath = uuid + "_" + (queryName) + "_"+ clientTask.getInstance().getInstanceId();
			resultInfo.add(serverInstanceCount);
			resultInfo.add(clientTaskList.size());
			resultInfo.add(totalVolume);
			try {
				br = new BufferedReader(new FileReader(clientResultFilePath));
				int rowcount = 1;
				//
				while((line = br.readLine())!=null){
					if(rowcount==3){
						//queryDuration
						resultInfo.add(line);
					}
					else if(rowcount==4){
						//record count
						resultInfo.add(line);
					}
					rowcount++;
				}
				
			} catch (FileNotFoundException e) {
				logger.error("File not exists: " + clientResultFilePath, e.fillInStackTrace());
			} catch (IOException e) {
				logger.error("Unable to read file: " + clientResultFilePath, e.fillInStackTrace());
			}
			
			writeVoltDbInfo();
			
			writeRow();
		}
		
		for(int i=0;i<=15;i++){
			sheet.autoSizeColumn(i);
		}
	}
}
