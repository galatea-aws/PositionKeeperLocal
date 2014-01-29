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
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Cell;


public class TradeSimulatorCollector extends DataCollector {

	public static Logger logger = LogManager.getLogger(TradeSimulatorCollector.class.getName());
	public TradeSimulatorCollector(ArrayList<ClientTask> clientTaskList,
			int serverInstanceCount, Properties benchmarkProp, String queryName, String reportPath) {
		super(clientTaskList, serverInstanceCount,benchmarkProp,queryName, reportPath);
	}
	
/*	@Override
	public void run(){
		long totalDuration = 1;
		for(ClientTask clientTask: clientTaskList){
			String clientResultFilePath = (queryName) + "_"+ clientTask.getInstance().getInstanceId();
			try {
				BufferedReader br = new BufferedReader(new FileReader(clientResultFilePath));
				String line,last = null;
				while((line = br.readLine())!=null){
					last = line;
				}
				totalDuration += Long.valueOf(last);
			}catch (FileNotFoundException e) {
				logger.error("File not exists: " + clientResultFilePath, e.fillInStackTrace());
			}catch (Exception e) {
				logger.error("Unable to read file: " + clientResultFilePath, e.fillInStackTrace());
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
			Boolean addHead = (new File(getReportFilePath())).exists();
			bw = new BufferedWriter(new FileWriter(getReportFilePath(),true));
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
	}*/
	
	
	@Override
	public void addHead(){
		if(clientTaskList.size()>0){
			BufferedReader br = null;
			String line = null;
			String queryStatement = "";
			String queryProcedureName = "";
			ClientTask clientTask = clientTaskList.get(0);
			String clientResultFilePath = (queryName) + "_"+ clientTask.getInstance().getInstanceId();
			HSSFCellStyle style = workbook.createCellStyle();
			style.setWrapText(true);
			
			//Open result file that is from client instance
			try {
				ArrayList<String> resultBuffer = new ArrayList<String>();
				br = new BufferedReader(new FileReader(clientResultFilePath));
				while((line = br.readLine())!=null){
					resultBuffer.add(line);
				}
				
				int size = resultBuffer.size();
				if(resultBuffer.size()<3){
					queryStatement = resultBuffer.get(size-3);
					queryProcedureName = resultBuffer.get(size-2);
				}
				
				HSSFRow row = sheet.createRow(0);
				Cell cell = row.createCell(0);
				cell.setCellValue("QueryName: ");
				cell = row.createCell(1);
				cell.setCellValue(queryProcedureName);
				
				row = sheet.createRow(1);
				cell = row.createCell(0);
				cell.setCellValue("QueryStatement: ");
				
				cell = row.createCell(1);
				cell.setCellValue(queryStatement);
				cell.setCellStyle(style);
				
				row = sheet.createRow(2);
				cell = row.createCell(0);
				cell.setCellValue("Instance Type: ");
				
				cell = row.createCell(1);
				cell.setCellValue(benchmarkProp.getProperty("instanceType"));
				cell.setCellStyle(style);
				
				sheet.createRow(4);
				sheet.createRow(5);
				int cellnum = 0;
				row = sheet.createRow(6);
				String[] columnName = new String[]{"Server Count","Client Count","Avg Through Put(txn/s)","Trade Volume","Account Count","Product Count","Trade Days",
						"Sitesperhost","Kfactor","Temtablesize","Date", "Table Parition", "Table Index", "Procedure Partition"};
				
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
	public void writeResult(){
		int tradedays = Integer.valueOf(benchmarkProp.getProperty("tradedays"));
		long totalVolume = Long.valueOf(benchmarkProp.getProperty("tradevolume")) * clientInstanceCount * tradedays;

		long totalDuration = 1;
		for(ClientTask clientTask: clientTaskList){
			String clientResultFilePath = (queryName) + "_"+ clientTask.getInstance().getInstanceId();
			try {
				BufferedReader br = new BufferedReader(new FileReader(clientResultFilePath));
				String line,last = null;
				while((line = br.readLine())!=null){
					last = line;
				}
				totalDuration += Long.valueOf(last);
			}catch (FileNotFoundException e) {
				logger.error("File not exists: " + clientResultFilePath, e.fillInStackTrace());
			}catch (Exception e) {
				logger.error("Unable to read file: " + clientResultFilePath, e.fillInStackTrace());
			}
		}
		
		
		int avgThroughput = (int) (totalVolume/(totalDuration/1000.0));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String now = sdf.format(new Date());
		
		ArrayList<String> resultInfo = new ArrayList<String>();
		resultInfo.add(String.valueOf(serverInstanceCount));
		resultInfo.add(String.valueOf(clientInstanceCount));
		resultInfo.add(String.valueOf(avgThroughput));
		resultInfo.add(String.valueOf(totalVolume));
		resultInfo.add(benchmarkProp.getProperty("accounts"));
		resultInfo.add(benchmarkProp.getProperty("products"));
		resultInfo.add(benchmarkProp.getProperty("tradedays"));
		resultInfo.add(benchmarkProp.getProperty("sitesperhost"));
		resultInfo.add(benchmarkProp.getProperty("kfactor"));
		resultInfo.add(benchmarkProp.getProperty("temptablesize"));
		resultInfo.add(now);
		resultInfo.add(tablePartition);
		resultInfo.add(tableIndex);
		resultInfo.add(procedurePartition);
		
		int cellnum = 0;
		HSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);
		Cell cell = row.createCell(0);
		
		HSSFCellStyle style = workbook.createCellStyle();
		style.setWrapText(true);
		
		for(String s: resultInfo){
			cell = row.createCell(cellnum);
			cell.setCellValue(s);
			if(cellnum==resultInfo.size()-1||
				cellnum==resultInfo.size()-2||
				cellnum==resultInfo.size()-3){
				cell.setCellStyle(style);
			}
				
			cellnum++;
		}
		
		for(int i=0;i<=15;i++){
			sheet.autoSizeColumn(i);
		}
	}
	

}
