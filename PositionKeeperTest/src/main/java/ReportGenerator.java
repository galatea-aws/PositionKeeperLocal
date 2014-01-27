import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class ReportGenerator {
	public static Logger logger = LogManager.getLogger(ReportGenerator.class.getName());
	private ArrayList<ClientTask> clientTaskList = new ArrayList<ClientTask>();
	private int serverInstanceCount;
	private int clientInstanceCount;
	private Properties benchmarkProp = new Properties();
	private GitHelper gitHelper;
	//Folder for pushing reports to github
	private String targetReportPath;
	//Temp folder for saving all reports locally
	private String tempReportPath = "report/";	
	//Folder for saving newly generated reports;
	private String newlyGeneratedReportPath;
	private String instanceType;
	public ReportGenerator(ArrayList<ClientTask> clientTaskList, GitHelper gitHelper, Properties benchmarkProp,int serverInstanceCount, int clientInstanceCount){
		this.clientTaskList = clientTaskList;
		this.serverInstanceCount = serverInstanceCount;
		this.clientInstanceCount = clientInstanceCount;
		this.benchmarkProp = benchmarkProp;	
		this.gitHelper = gitHelper;
		this.targetReportPath = benchmarkProp.getProperty("gitfolder")+"/report/";
		this.instanceType = benchmarkProp.getProperty("instanceType");
		this.newlyGeneratedReportPath = tempReportPath+"Archive/"+instanceType + "/";
	}
	
	public void GenerateReport(String queryName){
		//Collect Data	
		if(queryName.equals("TestDataSimulator")){
			TradeSimulatorCollector tc = new TradeSimulatorCollector(clientTaskList,serverInstanceCount,clientInstanceCount, benchmarkProp,queryName,newlyGeneratedReportPath);
			tc.run();
		}
		else{
			QueryDataCollector qc = new QueryDataCollector(clientTaskList,serverInstanceCount,clientInstanceCount, benchmarkProp,queryName,newlyGeneratedReportPath);
			qc.run();
		}
	}
	
	public void LoadWorkSpace() throws IOException{
		//Delete local report folder
		try {
			FileUtils.deleteDirectory(new File(tempReportPath));
		} catch (IOException e) {
			logger.error("Unable to delete folder: " + tempReportPath);
		}	
		try {
			FileUtils.copyDirectory(new File(targetReportPath), new File(tempReportPath));
		} catch (IOException e) {
			logger.error("Unable to copy folder from " + targetReportPath + " to " + tempReportPath, e.fillInStackTrace());
			throw e;
		}
		File file = new File(newlyGeneratedReportPath);
		if(!file.exists()){
			try {
				FileUtils.forceMkdir(file);
			} catch (IOException e) {
				logger.error("Create folder: " + newlyGeneratedReportPath, e.fillInStackTrace());
				throw e;
			}
		}
		
	}
	
	public void ArchiveReport() throws IOException{
		//Delete local report folder
		FileUtils.deleteDirectory(new File(targetReportPath));
		FileUtils.copyDirectory(new File(tempReportPath), new File(targetReportPath));
		gitHelper.addCommitAndPushReport();
	}
}
