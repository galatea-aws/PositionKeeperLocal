import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;


public class ReportGenerator {
	public static Logger logger = LogManager.getLogger(ReportGenerator.class.getName());
	private ArrayList<ClientTask> clientTaskList = new ArrayList<ClientTask>();
	private int serverInstanceCount;
	private Properties benchmarkProp = new Properties();
	private GitHelper gitHelper;
	//Folder for pushing reports to github
	private String targetReportPath;
	private String instanceType;
	private String gitRevision;
	private String uuid;
	public ReportGenerator(ArrayList<ClientTask> clientTaskList, GitHelper gitHelper, Properties benchmarkProp,int serverInstanceCount, String gitRevision, String uuid){
		this.clientTaskList = clientTaskList;
		this.serverInstanceCount = serverInstanceCount;
		this.benchmarkProp = benchmarkProp;	
		this.gitHelper = gitHelper;
		this.targetReportPath = benchmarkProp.getProperty("gitfolder")+"report/tmp/";
		this.instanceType = benchmarkProp.getProperty("instancetype");
		this.gitRevision = gitRevision;
		this.uuid = uuid;
	}
	
	public void GenerateReport(String queryName){
		//Collect Data	
		if(queryName.equals("TestDataSimulator")){
			TradeSimulatorCollector tc = new TradeSimulatorCollector(clientTaskList,serverInstanceCount, benchmarkProp,queryName,targetReportPath,gitRevision, uuid);
			tc.run();
		}
		else{
			QueryDataCollector qc = new QueryDataCollector(clientTaskList,serverInstanceCount, benchmarkProp,queryName,targetReportPath,gitRevision, uuid);
			qc.run();
		}
	}
	
	public void LoadWorkSpace(){
		try {
			File f = new File(targetReportPath);
			if(!f.exists())
				FileUtils.forceMkdir(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.fillInStackTrace());
		}
	}
	
	public void ArchiveReport() throws IOException{
/*		//Delete local report folder	
		try {
			gitHelper.addCommitAndPushReport();
		} catch (IOException e) {
			logger.error("Unable to commit reports to github", e.fillInStackTrace());
			throw e;
		}	*/
	}
	
	public static void SummarizeReport(String sourceXLS, String targetXLS) throws FileNotFoundException, IOException{
		File targetFile = new File(targetXLS);
		File sourceFile = new File(sourceXLS);
		if(!targetFile.exists()){
			try {
				FileUtils.copyFile(sourceFile,targetFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		HSSFWorkbook sourceWorkbook = new HSSFWorkbook(new FileInputStream(sourceFile));
		HSSFWorkbook targetWorkbook = new HSSFWorkbook(new FileInputStream(targetFile));
		HSSFSheet sourceSheet = sourceWorkbook.getSheetAt(0);
		HSSFSheet targetSheet = targetWorkbook.getSheetAt(0);
		
		for(int i=7;i<=sourceSheet.getLastRowNum();i++){
			Row sourceRow = sourceSheet.getRow(i);
			Row targetRow = targetSheet.createRow(targetSheet.getLastRowNum()+1);
			for(int j=0; j<sourceRow.getLastCellNum();j++){
				Cell targetcell = targetRow.createCell(j);
				Cell sourceCell = sourceRow.getCell(j);
				switch(sourceCell.getCellType()){
				case Cell.CELL_TYPE_NUMERIC:
					targetcell.setCellValue(sourceCell.getNumericCellValue());
					break;
				case Cell.CELL_TYPE_STRING:
					targetcell.setCellValue(sourceCell.getStringCellValue());
					break;
				case Cell.CELL_TYPE_BOOLEAN:
					targetcell.setCellValue(sourceCell.getBooleanCellValue());
					break;
				default:
					targetcell.setCellValue(sourceCell.getStringCellValue());
					break;
				}
				CellStyle newStyle = targetWorkbook.createCellStyle();
				newStyle.cloneStyleFrom(sourceCell.getCellStyle());
				targetcell.setCellStyle(newStyle);
			}
		}
		
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(targetFile);
			targetWorkbook.write(out);
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
}
