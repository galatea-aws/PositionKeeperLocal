import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;


public class ReportGenerator {
	private ArrayList<ClientTask> clientTaskList = new ArrayList<ClientTask>();
	private int serverInstanceCount;
	private int clientInstanceCount;
	private Properties benchmarkProp = new Properties();
	private GitHelper gitHelper;
	//Folder for pushing reports to github
	private String targetReportPath;
	//Temp folder for saving all reports locally
	private String tempReportPath = "/report";	
	//Folder for saving newly generated reports;
	private String newlyGeneratedReportPath;
	private String instanceType;
	public ReportGenerator(ArrayList<ClientTask> clientTaskList, GitHelper gitHelper, Properties benchmarkProp,int serverInstanceCount, int clientInstanceCount){
		this.clientTaskList = clientTaskList;
		this.serverInstanceCount = serverInstanceCount;
		this.clientInstanceCount = clientInstanceCount;
		this.benchmarkProp = benchmarkProp;	
		this.gitHelper = gitHelper;
		this.targetReportPath = benchmarkProp.getProperty("gitfolder")+"/report";
		this.newlyGeneratedReportPath = tempReportPath+"/Archive/"+instanceType;
		this.instanceType = benchmarkProp.getProperty("instanceType");
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
		FileUtils.deleteDirectory(new File(tempReportPath));	
		FileUtils.copyDirectory(new File(targetReportPath), new File(tempReportPath));
		File file = new File(newlyGeneratedReportPath);
		if(!file.exists()){
			FileUtils.forceMkdir(file);
		}
	}
	
	public void ArchiveReport() throws IOException{
		//Delete local report folder
		FileUtils.deleteDirectory(new File(targetReportPath));
		FileUtils.copyDirectory(new File(tempReportPath), new File(targetReportPath));
		gitHelper.addCommitAndPushReport();
	}
}
