import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;


public class DataCollector {
	public ArrayList<ClientTask> clientTaskList = new ArrayList<ClientTask>();
	public int serverInstanceCount;
	public int clientInstanceCount;
	public String queryName;
	public Properties benchmarkProp = new Properties();
	public String reportPath;
	public DataCollector(ArrayList<ClientTask> clientTaskList, int serverInstanceCount, int clientInstanceCount, Properties benchmarkProp, String queryName, String reportPath){
		this.clientTaskList = clientTaskList;
		this.serverInstanceCount = serverInstanceCount;
		this.clientInstanceCount = clientInstanceCount;
		this.queryName = queryName;
		this.benchmarkProp = benchmarkProp;	
		this.reportPath = reportPath;
	}
	
	public void run(){
		
	}
}
