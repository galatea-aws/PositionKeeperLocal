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
}
