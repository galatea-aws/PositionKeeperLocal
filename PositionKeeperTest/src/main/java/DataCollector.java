import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;


public class DataCollector {
	public static Logger logger = LogManager.getLogger(DataCollector.class.getName());
	public ArrayList<ClientTask> clientTaskList = new ArrayList<ClientTask>();
	public int serverInstanceCount;
	public int clientInstanceCount;
	public String queryName;
	public Properties benchmarkProp = new Properties();
	public String reportPath;
	public HSSFSheet sheet;
	public HSSFWorkbook workbook;
	public String tablePartition = "";
	public String tableIndex = "";
	public String procedurePartition = "";
	public String gitRevision;
	public String uuid;
	public ArrayList<Object> resultInfo = new ArrayList<Object>();
	public DataCollector(ArrayList<ClientTask> clientTaskList, int serverInstanceCount, Properties benchmarkProp, String queryName, String reportPath, String gitRevision, String uuid){
		this.clientTaskList = clientTaskList;
		this.serverInstanceCount = serverInstanceCount;
		this.clientInstanceCount = clientTaskList.size();
		this.queryName = queryName;
		this.benchmarkProp = benchmarkProp;	
		this.reportPath = reportPath;
		this.gitRevision = gitRevision;
		this.uuid = uuid;
	}
	
	/**
	 * Generate or update report file
	 */
	public void run(){
		File file = new File(reportPath + "/" + uuid + "_" + queryName + ".xls");
	    workbook = new HSSFWorkbook();
	    Boolean creatSheet = !file.exists();
		if(creatSheet)
			workbook.createSheet();
		else
			try {
				workbook = new HSSFWorkbook(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				logger.error(e.fillInStackTrace());
			} catch (IOException e) {
				logger.error(e.fillInStackTrace());
			}
		
		sheet = workbook.getSheetAt(0);
		
		if(creatSheet)
			addHead();
		writeQueryResult();
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
		    workbook.write(out);
		} catch (FileNotFoundException e) {
			logger.error(e.fillInStackTrace());
		} catch (IOException e) {
			logger.error(e.fillInStackTrace());
		}
		finally{
			if(out!=null)
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.error(e.fillInStackTrace());
				}
		}
	}
	
	public void addHead(){
		
	}
	
	public void writeQueryResult(){
		
	}
	
	public void writeVoltDbInfo(){
		resultInfo.add(Integer.valueOf(benchmarkProp.getProperty("sitesperhost")));
		resultInfo.add(Integer.valueOf(benchmarkProp.getProperty("kfactor")));
		resultInfo.add(Integer.valueOf(benchmarkProp.getProperty("temptablesize")));
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String now = sdf.format(new Date());
		resultInfo.add(now);
		
		resultInfo.add(benchmarkProp.getProperty("tradetableparition"));
		resultInfo.add(benchmarkProp.getProperty("tradetableindex"));
		resultInfo.add(gitRevision);
	}
	
	/**
	 * Output result info to a row
	 */
	public void writeRow(){
		int cellnum = 0;
		HSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);
		
		HSSFCellStyle style = workbook.createCellStyle();
		style.setWrapText(true);
		
		for(Object obj: resultInfo){
			Cell cell = row.createCell(cellnum);
		 if(obj instanceof Date) 
	            cell.setCellValue((Date)obj);
	        else if(obj instanceof Boolean)
	            cell.setCellValue((Boolean)obj);
	        else if(obj instanceof String)
	            cell.setCellValue((String)obj);
	        else if(obj instanceof Double)
	            cell.setCellValue((Double)obj);
	        else if(obj instanceof Integer)
	            cell.setCellValue((Integer)obj);
	        else if(obj instanceof Long)
	        	cell.setCellValue((Long)obj);
		 
			if(cellnum==resultInfo.size()-1||
				cellnum==resultInfo.size()-2||
				cellnum==resultInfo.size()-3){
				cell.setCellStyle(style);
			}
				
			cellnum++;
		}
	}
}
