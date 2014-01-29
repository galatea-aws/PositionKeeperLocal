import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;


public class DataCollector {
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
	public DataCollector(ArrayList<ClientTask> clientTaskList, int serverInstanceCount, Properties benchmarkProp, String queryName, String reportPath){
		this.clientTaskList = clientTaskList;
		this.serverInstanceCount = serverInstanceCount;
		this.clientInstanceCount = clientTaskList.size();
		this.queryName = queryName;
		this.benchmarkProp = benchmarkProp;	
		this.reportPath = reportPath;
		getTablePartition();
		getTableIndex();
		getProcedurePartition();
	}
	
	public void run(){
		File file = new File(reportPath + "/" + queryName + ".xls");
	    workbook = new HSSFWorkbook();
	    Boolean creatSheet = !file.exists();
		if(creatSheet)
			workbook.createSheet();
		else
			try {
				workbook = new HSSFWorkbook(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		sheet = workbook.getSheetAt(0);
		
		if(creatSheet)
			addHead();
		writeResult();
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
		    workbook.write(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			if(out!=null)
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	public String getReportFilePath(){
		return reportPath + "/" + queryName + ".csv";
	}
	
	public void addHead(){
		
	}
	
	public void writeResult(){
		
	}
	
	public void getTablePartition(){
		try {
			BufferedReader br = new BufferedReader(new FileReader("dbschema/table_partition.sql"));
			String line;
			//
			while((line = br.readLine())!=null){
				tablePartition+=line+"\n";
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void getTableIndex(){
		try {
			BufferedReader br = new BufferedReader(new FileReader("dbschema/table_index.sql"));
			String line;
			//
			while((line = br.readLine())!=null){
				tableIndex+=line+"\n";
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void getProcedurePartition(){
		try {
			BufferedReader br = new BufferedReader(new FileReader("dbschema/procedure_partition.sql"));
			String line;
			//
			while((line = br.readLine())!=null){
				procedurePartition+=line+"\n";
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
