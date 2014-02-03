import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.Instance;

/**
 * @author wsun
 *
 */
/**
 * @author wsun
 * 
 */
public class PositionKeeperBenchmark {
	public static Logger logger = LogManager.getLogger(PositionKeeperBenchmark.class.getName());
	
	public static String benchmarkconfig = "benchmarkconfig.properties";
	public Properties benchmarkProp = new Properties();
	
	private String serverConfig;
	private String tradesimulatorConfig;
	private String deployment;
	private String ddl;
	
	private AwsHelper awsHelper;
	private String imageName;
	public String instanceType;
	private String securityGroup;
	// Whether launch new instance
	private boolean launchInstance = false;
	// Whether terminate instance after benchmark finish
	private boolean terminatetInstance = false;
	
	private boolean reloadVoltdb = false;
	private String reloadVoltdbQuery;
	private boolean shutdownVoltdb = true;
	
	private ArrayList<String> benchmarkServerIdList = new ArrayList<String>();
	private ArrayList<String> benchmarkClientIdList = new ArrayList<String>();
	public ArrayList<Instance> serverInstanceList = new ArrayList<Instance>();
	public ArrayList<Instance> clientInstanceList = new ArrayList<Instance>();
	private ArrayList<ServerTask> serverTaskList = new ArrayList<ServerTask>();
	private ArrayList<ClientTask> clientTaskList = new ArrayList<ClientTask>();
	public	ArrayList<String> queryList = new ArrayList<String>();
	
	private GitHelper gitHelper = new GitHelper();
	private String  revision;
	public BenchmarkCoordinator bc = null;
	public Boolean isStartingServer = false;
	public Boolean waitingForOtherBenchmark = false;
	
	private int tradeVolume;
	private int sitesperhost;
	private int kfactor;
	private boolean withNext;
	private int serverInstanceStart;
	private int serverInstanceEnd;
	private int clientInstanceStart;
	private int clientInstanceEnd;
	private int queryRepeatTimes = 3;
	
	public int serverInstanceCount;
	public int clientInstanceCount;
	public String uuid;
	public String gitFolder;
/*	public static void main(String[] args) {
		PositionKeeperBenchmark pt;
		try {
			pt = new PositionKeeperBenchmark();
			for(int i=pt.benchmarkServerIdList.size();i>=1;i--){
				logger.info("Running server instance: " + i);
				pt.run(i,1);
			}
			pt.run(1,1);
		//	MailHelper.sendJobCompleteMail(pt.queryList, pt.benchmarkProp);
		} catch (Exception e) {
			logger.error("Positionkeeper benchmark stopped, please check logs",e.fillInStackTrace());
		//	MailHelper.sendJobFailMail();
		}
	}*/

	/**
	 * 
	 * @throws IOException
	 */
	public PositionKeeperBenchmark() throws IOException{
		benchmarkProp = new Properties();
		try {
			
			benchmarkProp.load(new FileInputStream(benchmarkconfig));
			
			//Aws Instance config
			launchInstance = Boolean.valueOf(benchmarkProp.getProperty("launchinstance"));
			terminatetInstance = Boolean.valueOf(benchmarkProp.getProperty("terminatetinstance"));
			benchmarkServerIdList = new ArrayList<String>(Arrays.asList(benchmarkProp.getProperty("serverinstanceids").split(",")));
			benchmarkClientIdList = new ArrayList<String>(Arrays.asList(benchmarkProp.getProperty("clientinstanceids").split(",")));
			imageName = benchmarkProp.getProperty("imagename");
			instanceType = 	benchmarkProp.getProperty("instancetype");
			securityGroup =		benchmarkProp.getProperty("securitygroup");
			
			//Git commit directory path
			gitFolder = benchmarkProp.getProperty("gitfolder");
			gitHelper = new GitHelper(gitFolder);
			
			//Config for voltdb server
			serverConfig = gitFolder + benchmarkProp.getProperty("serverconfig");
			deployment = gitFolder + benchmarkProp.getProperty("deployment");
			ddl=gitFolder + benchmarkProp.getProperty("ddl");
					
			//Config for tradesimulator
			tradesimulatorConfig = gitFolder + benchmarkProp.getProperty("tradesimulatorconfig");
			reloadVoltdb = Boolean.valueOf(benchmarkProp.getProperty("reloadvoltdb"));
			shutdownVoltdb = Boolean.valueOf(benchmarkProp.getProperty("shutdownvoltdb"));
			
			//Query config
			if(reloadVoltdb){
				queryList.add(benchmarkProp.getProperty("reloadvoltdbquery"));
				reloadVoltdbQuery = benchmarkProp.getProperty("reloadvoltdbquery");
			}
			queryList.addAll(Arrays.asList(benchmarkProp.getProperty("querylist").split(",")));

		} catch (FileNotFoundException e) {
			logger.error("Benchmark config file: " + benchmarkconfig + " not found", e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("Can not read benchmark", e.fillInStackTrace());
			throw e;
		}
	}
	

	/**
	 * 
	 * @param serverInstanceCount
	 * @param clientInstanceCount
	 * @throws Exception
	 */
	public void run() throws Exception {
		
		logger.info("Start benchmark ServerInstanceCount: " + (getServerInstanceEnd()-getServerInstanceStart()) + 
				" ClientInstanceCount: " + (getClientInstanceEnd()-getClientInstanceStart()) +
				" Trade Volumen: " + getTradeVolume() + " Kfactor" + getKfactor() + " Sitesperhost: " + getSitesperhost());
		benchmarkProp.setProperty("tradevolume", String.valueOf(getTradeVolume()));
		benchmarkProp.setProperty("kfactor", String.valueOf(getKfactor()));
		benchmarkProp.setProperty("sitesperhost", String.valueOf(getSitesperhost()));
		
		serverInstanceCount = getServerInstanceEnd() - getServerInstanceStart();
		clientInstanceCount = getClientInstanceEnd() - getClientInstanceStart();
		
		if (serverInstanceCount > benchmarkServerIdList.size()|| clientInstanceCount >benchmarkClientIdList.size()) {
			logger.warn("Not enough instances for benchmark");
			throw new Exception();
		}
		
		// Create connection to AWS
		logger.info("Create connection to AWS");
		awsHelper = AwsHelper.getAwsHelper();

		try {
			if (launchInstance) {
				//Launch new instances
				launchInstance();
			} else {
				//Instances are running, get their information
				serverInstanceList = new ArrayList<Instance>(
						awsHelper.updateInstancesByIds(new ArrayList<String>(benchmarkServerIdList.subList(getServerInstanceStart(), getServerInstanceEnd()))));
				logger.info("Server instance count:" + serverInstanceList.size());
				clientInstanceList = new ArrayList<Instance>(
						awsHelper.updateInstancesByIds(new ArrayList<String>(benchmarkClientIdList.subList(getClientInstanceStart(), getClientInstanceEnd()))));
				logger.info("Client instance count:" + clientInstanceList.size());
				
				//Validate Instance Type.
				for(Instance i: serverInstanceList){
					if(!i.getInstanceType().equals(instanceType)){
						Exception e = new Exception();
						logger.error("Instance Type Mismatch", e.fillInStackTrace());
						throw e;
					}
				}
				
				for(Instance i: clientInstanceList){
					if(!i.getInstanceType().equals(instanceType)){
						Exception e = new Exception();
						logger.error("Instance Type Mismatch");
						throw e;
					}
				}
				
			}
			if(bc ==null){
				// Update configs
				updateConfigFile();
				// Push to GitHub
				pushToGit();	
			}
			else{
				waitingForOtherBenchmark = true;
				bc.addBenchmark(this);
				while(waitingForOtherBenchmark){
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				// Update configs
				updateConfigFile();
				// Push to GitHub
				pushToGit();	
			}
			
			revision = gitHelper.getHeadRevision();
			
			// Do test
			startTest();
		} catch (Exception e) {
			throw e;
		} finally {
			if (terminatetInstance)
				terminateInstance();
			else if(shutdownVoltdb){
				try {
					ResetServerInstanceEnv();
					ResetClientInstanceEnv();
				} catch (LoginFailException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
		}
	}
	
	public void launchInstance() throws Exception {
		//Launch new server instances
		serverInstanceList = new ArrayList<Instance>(awsHelper.lanuchInstance(
				serverInstanceCount, imageName, instanceType, securityGroup));
		//Launch new client instances
		clientInstanceList = new ArrayList<Instance>(awsHelper.lanuchInstance(
				clientInstanceCount, imageName, instanceType, securityGroup));

		// Check instance count
		if (serverInstanceList.size() == 0 || clientInstanceList.size() == 0) {
			if (serverInstanceList.size() == 0)
				logger.error("No server instance lanuched");

			if (clientInstanceList.size() == 0)
				logger.error("No client instance lanuched");
		}

		// Wait for all instances to start
		try {
			logger.info("Stop 60s for instance running");
			Thread.sleep(60 * 1000);
		} catch (InterruptedException e) {
			logger.error("Thread excepetion", e.fillInStackTrace());
		}

		// Wait for instances status check initializing
		try {
			logger.info("Stop 180s for instance status check initializing");
			Thread.sleep(180 * 1000);
		} catch (InterruptedException e) {
			logger.error("Thread excepetion", e.fillInStackTrace());
		}

		long monitoringTimes = System.currentTimeMillis() + 1000 * 600;
		while (awsHelper.isStatusChecksInitializing(serverInstanceList)
				|| awsHelper.isStatusChecksInitializing(clientInstanceList)) {
			try {
				// Sleep 20s and check again
				Thread.sleep(20 * 1000);
			} catch (InterruptedException e) {
				logger.error("Thread excepetion", e.fillInStackTrace());
			}
			if (System.currentTimeMillis() > monitoringTimes) {
				logger.info("Several instances status check initializing");
				return;
			}
		}

		//Update public DNS name and public ip
		serverInstanceList = new ArrayList<Instance>(
				awsHelper.updateInstances(serverInstanceList));
		clientInstanceList = new ArrayList<Instance>(
				awsHelper.updateInstances(clientInstanceList));
	}
	
	/**
	 * 
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public void updateConfigFile() throws IOException,
			ParserConfigurationException, TransformerException {
		if (serverInstanceList.size() == 0)
			return;

		//Update voltdb deployment.xml
		updateDeployment();
		//Update voltdb server config
		updateServerConfig();
		//Update throughput test config
		updateTradesimulatorConfig();
		//Update DDL sql
		updateDDL();
	}
	
	/**
	 * 
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public void updateDeployment() throws ParserConfigurationException, TransformerException{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			//Root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("deployment");
			doc.appendChild(rootElement);
	
			//Add cluster element to root element
			Element cluster = doc.createElement("cluster");
			rootElement.appendChild(cluster);
	
			//Set attributes to cluster element
			//Attribute hostcount
			Attr attr = doc.createAttribute("hostcount");
			attr.setValue(String.valueOf(serverInstanceList.size()));
			cluster.setAttributeNode(attr);
	
			//Attribute sitesperhost
			attr = doc.createAttribute("sitesperhost");
			attr.setValue(benchmarkProp.getProperty("sitesperhost"));
			cluster.setAttributeNode(attr);
	
			//Attribute kfactor
			attr = doc.createAttribute("kfactor");
			attr.setValue(benchmarkProp.getProperty("kfactor"));
			cluster.setAttributeNode(attr);
			
			
			//Add systemsettings element to root element
			Element systemsettings = doc.createElement("systemsettings");
			rootElement.appendChild(systemsettings);
	
			//Add temptables element to root systemsettings
			Element temptables = doc.createElement("temptables");
			systemsettings.appendChild(temptables);
			
			//Set attributes to temptables element
			//Attribute maxzie
			attr = doc.createAttribute("maxsize");
			attr.setValue(benchmarkProp.getProperty("temptablesize"));
			temptables.setAttributeNode(attr);
					
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
	
			StreamResult result = new StreamResult(new File(deployment));
			transformer.transform(source, result);
		} catch (ParserConfigurationException e) {
			logger.error("Can not parse deployment.xml", e.fillInStackTrace());
			throw e;
		} catch (TransformerException e) {
			logger.error("Unable to write deployment.xml:" + deployment, e.fillInStackTrace());
			throw e;
		}
	}
	
	/**
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void updateServerConfig() throws FileNotFoundException, IOException {

		// Update ServerConfig
		Properties serverProp = new Properties();

		try {
			serverProp.load(new FileInputStream(serverConfig));
			serverProp.setProperty("host", serverInstanceList.get(0).getPrivateIpAddress());
			serverProp.setProperty("maxheapsize", benchmarkProp.getProperty("maxheapsize"));
	
			StringBuilder sb = new StringBuilder();
			for (Instance instance : serverInstanceList) {
				sb.append(instance.getPrivateIpAddress()).append(",");
			}
	
			serverProp.setProperty("servers", sb.toString());
			serverProp.store(new FileOutputStream(serverConfig), "test");
		}
		catch (FileNotFoundException e) {
			logger.error("Server config file: " + serverConfig + " not found", e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("Unable to write Server config file: " + serverConfig, e.fillInStackTrace());
			throw e;
		}
	}
	
	/**
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void updateTradesimulatorConfig() throws FileNotFoundException, IOException {

		// Update tradesimulator config
		Properties tradesimulatorProp = new Properties();
		
		try {
		tradesimulatorProp.load(new FileInputStream(tradesimulatorConfig));
		tradesimulatorProp.setProperty("tradevolume",benchmarkProp.getProperty("tradevolume"));
		tradesimulatorProp.setProperty("accounts",benchmarkProp.getProperty("accounts"));
		tradesimulatorProp.setProperty("products",benchmarkProp.getProperty("products"));
		tradesimulatorProp.setProperty("tradedays",benchmarkProp.getProperty("tradedays"));
		tradesimulatorProp.setProperty("probabilitybyisin",benchmarkProp.getProperty("probabilitybyisin"));
		tradesimulatorProp.store(new FileOutputStream(tradesimulatorConfig), "test");
		}
		catch (FileNotFoundException e) {
			logger.error("Tradesimulator config file : " + tradesimulatorConfig + " not found", e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("Unable to write Tradesimulator config file: " + tradesimulatorConfig, e.fillInStackTrace());
			throw e;
		}
	}
	
	/**
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void updateDDL() throws FileNotFoundException, IOException {
		File ddlFile = new File(ddl);
		FileUtils.copyFile(new File("dbschema/ddl.sql"), ddlFile);
		BufferedWriter bw = new BufferedWriter(new FileWriter(ddl, true));
		String tableIndex = String.format("CREATE INDEX trades_account_id ON trades (%s);", benchmarkProp.getProperty("tradetableindex"));
		String tableParition = String.format("PARTITION TABLE trades ON COLUMN %s;", benchmarkProp.getProperty("tradetableparition"));
		bw.write(tableIndex);
		bw.newLine();
		bw.write(tableParition);
		bw.flush();
		bw.close();
	}
	
	public void pushToGit() throws IOException{
		gitHelper.commitAndPush();
	}

	/**
	 * 
	 * @throws IOException
	 * @throws LoginFailException
	 */
	public void startTest() throws IOException, LoginFailException {
		serverTaskList = new ArrayList<ServerTask>();
		clientTaskList = new ArrayList<ClientTask>();
		
		if(reloadVoltdb){
			
			//Create server task
			for (Instance i : serverInstanceList) {
				ServerTask serverTask = new ServerTask(i);
				serverTaskList.add(serverTask);
			}
			
			ResetServerInstanceEnv();

			for (final ServerTask serverTask : serverTaskList) {
				logger.info("Create server task on instance: "
						+ serverTask.getInstance().getInstanceId());
				Thread t = new Thread(new Runnable() {
							public void run() {
								try {
									serverTask.StartTask();
								} catch (LoginFailException e) {
									// TODO Auto-generated catch block
									logger.error("Thread excepetion", e.fillInStackTrace());
								}
							}
						});
				t.start();
			}
			
/*			for (final ServerTask serverTask : serverTaskList) {
				logger.info("Create server task on instance: "
						+ serverTask.getInstance().getInstanceId());
				serverTask.StartTask();
			}*/
			
			// Wait for all voltdb complete initialization
			try {
				int waitingforvoltdb = Integer.parseInt(benchmarkProp.getProperty("waitingforvoltdb","60"));
				logger.info("Stop " + waitingforvoltdb + "s for voltdb complete initialization");
				Thread.sleep(waitingforvoltdb * 1000);
			} catch (InterruptedException e) {
				logger.error("Thread excepetion", e.fillInStackTrace());
			}
			
		}
		
		//Create client task
		for (Instance i : clientInstanceList) {
			logger.info("Create client task on instance: "
					+ i.getInstanceId());
			clientTaskList.add(new ClientTask(i,uuid));
		}
		
		//Reset client instance env
		if(reloadVoltdb)
			ResetClientInstanceEnv();
		
		isStartingServer = false;
		
		ReportGenerator reportGenerator = new ReportGenerator(clientTaskList, gitHelper, benchmarkProp,serverInstanceCount, revision, uuid);
		reportGenerator.LoadWorkSpace();
		
		//Run query
		for(int i = 0; i<queryList.size();i++){
			int repeatTimes = queryRepeatTimes;
			if(queryList.get(i).equals(reloadVoltdbQuery)){
				repeatTimes = 1;
			}
			
			for(int j=1;j<=repeatTimes;j++){
				launchClientTask(queryList.get(i));
				reportGenerator.GenerateReport(queryList.get(i));
			}
		}
		
		reportGenerator.ArchiveReport();
	}
	
	public void ResetServerInstanceEnv() throws LoginFailException{
		for (ServerTask serverTask : serverTaskList) {
			logger.info("Reset Server Env on instance: "
					+ serverTask.getInstance().getInstanceId());
			serverTask.ResetEnv();
		}
		
		// Wait for all voltdb start
		try {
			logger.info("Stop 15 s for voltdb shutdown");
			Thread.sleep(15 * 1000);
		} catch (InterruptedException e) {
			logger.error("Thread excepetion", e.fillInStackTrace());
		}
	}

	public void ResetClientInstanceEnv() throws LoginFailException{
		for (ClientTask clientTask : clientTaskList) {
			logger.info("Reset Client Env on instance: "
					+ clientTask.getInstance().getInstanceId());
			clientTask.ResetEnv();
		}
	}
	
	public void launchClientTask(final String queryName) {
		for (final ClientTask clientTask : clientTaskList) {
			Thread t = new Thread(new Runnable() {
					public void run() {
						logger.info("Run query:" + queryName + " on instance: "
								+ clientTask.getInstance().getInstanceId());
						try {
							clientTask.StartTask(queryName);
						} catch (LoginFailException e) {
							// TODO Auto-generated catch block
							logger.error("Thread excepetion", e.fillInStackTrace());
						}
					}
				});
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
				logger.error("Thread excepetion", e.fillInStackTrace());
			}
		}
	}

	/**
	 * Close all server instances
	 */
	public void terminateInstance(){

		if (terminatetInstance) {
			awsHelper.terminateInstance(serverInstanceList);
			awsHelper.terminateInstance(clientInstanceList);
		}
	}


	public int getQueryRepeatTimes() {
		return queryRepeatTimes;
	}


	public void setQueryRepeatTimes(int queryRepeatTimes) {
		this.queryRepeatTimes = queryRepeatTimes;
	}


	public int getClientInstanceEnd() {
		return clientInstanceEnd;
	}


	public void setClientInstanceEnd(int clientInstanceEnd) {
		this.clientInstanceEnd = clientInstanceEnd;
	}


	public int getClientInstanceStart() {
		return clientInstanceStart;
	}


	public void setClientInstanceStart(int clientInstanceStart) {
		this.clientInstanceStart = clientInstanceStart;
	}


	public int getServerInstanceEnd() {
		return serverInstanceEnd;
	}


	public void setServerInstanceEnd(int serverInstanceEnd) {
		this.serverInstanceEnd = serverInstanceEnd;
	}


	public int getServerInstanceStart() {
		return serverInstanceStart;
	}


	public void setServerInstanceStart(int serverInstanceStart) {
		this.serverInstanceStart = serverInstanceStart;
	}


	public boolean isWithNext() {
		return withNext;
	}


	public void setWithNext(boolean withNext) {
		this.withNext = withNext;
	}


	public int getKfactor() {
		return kfactor;
	}


	public void setKfactor(int kfactor) {
		this.kfactor = kfactor;
	}


	public int getSitesperhost() {
		return sitesperhost;
	}


	public void setSitesperhost(int sitesperhost) {
		this.sitesperhost = sitesperhost;
	}


	public int getTradeVolume() {
		return tradeVolume;
	}


	public void setTradeVolume(int tradeVolume) {
		this.tradeVolume = tradeVolume;
	}
}
