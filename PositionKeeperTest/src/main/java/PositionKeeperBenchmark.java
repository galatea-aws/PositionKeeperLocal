import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	private static String benchmarkconfig = "benchmarkconfig.properties";
	private Properties benchmarkProp = new Properties();
	
	private String serverConfig;
	private String tradesimulatorConfig;
	private String deployment;

	private AwsHelper awsHelper;
	private String imageName;
	private String instanceType;
	private String securityGroup;
	// Whether launch new instance
	private boolean launchInstance = false;
	// Whether terminate instance after benchmark finish
	private boolean terminatetInstance = false;
	

	private ArrayList<String> benchmarkServerIdList = new ArrayList<String>();
	private ArrayList<String> benchmarkClientIdList = new ArrayList<String>();
	public ArrayList<Instance> serverInstanceList = new ArrayList<Instance>();
	public ArrayList<Instance> clientInstanceList = new ArrayList<Instance>();
	private ArrayList<ServerTask> serverTaskList = new ArrayList<ServerTask>();
	private ArrayList<ClientTask> clientTaskList = new ArrayList<ClientTask>();
	private	ArrayList<String> queryList = new ArrayList<String>();
	
	private GitHelper gitHelper = new GitHelper();
	
	
	public static void main(String[] args) {
		PositionKeeperBenchmark pt;
		try {
			pt = new PositionKeeperBenchmark();
			for(int i=pt.benchmarkServerIdList.size();i>=1;i--){
				logger.info("Running server instance: " + i);
				pt.run(i,1);
			}
			MailHelper.sendJobCompleteMail(pt.queryList);
		} catch (Exception e) {
			logger.error("Positionkeeper benchmark stopped, please check logs");
			MailHelper.sendJobFailMail();
		}
	}

	public PositionKeeperBenchmark() throws IOException{
		benchmarkProp = new Properties();
		try {
			benchmarkProp.load(new FileInputStream(benchmarkconfig));
			launchInstance = Boolean.valueOf(benchmarkProp
					.getProperty("launchinstance"));
			terminatetInstance = Boolean.valueOf(benchmarkProp
					.getProperty("terminatetinstance"));
			benchmarkServerIdList = new ArrayList<String>(
					Arrays.asList(benchmarkProp
							.getProperty("serverinstanceids").split(",")));
			benchmarkClientIdList = new ArrayList<String>(
					Arrays.asList(benchmarkProp
							.getProperty("clientinstanceids").split(",")));
			queryList = new ArrayList<String>(
					Arrays.asList(benchmarkProp
							.getProperty("querylist").split(",")));
			gitHelper = new GitHelper(benchmarkProp.getProperty("gitfolder"));
			serverConfig = gitHelper + benchmarkProp.getProperty("serverconfig");
			tradesimulatorConfig = gitHelper + benchmarkProp.getProperty("tradesimulatorconfig");
			deployment = gitHelper + benchmarkProp.getProperty("deployment");
		} catch (FileNotFoundException e) {
			logger.error("Benchmark config file: " + benchmarkconfig + " not found", e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("Can not read benchmark", e.fillInStackTrace());
			throw e;
		}
	}

	/**
	 * Core process
	 * @throws Exception 
	 */
	public void run(int serverInstanceCount, int clientInstanceCount) throws Exception {

		if (serverInstanceCount > benchmarkServerIdList.size()|| clientInstanceCount >benchmarkClientIdList.size()) {
			logger.warn("Not enough instances for benchmark");
			throw new Exception();
		}
		// Create connection to AWS
		logger.info("Create connection to AWS");
		awsHelper = AwsHelper.getAwsHelper();

		try {
			if (launchInstance) {
				launchInstance(serverInstanceCount, clientInstanceCount);
			} else {
				serverInstanceList = new ArrayList<Instance>(
						awsHelper.updateInstancesByIds(new ArrayList<String>(benchmarkServerIdList.subList(0, serverInstanceCount))));
				logger.info("Server instance count:" + serverInstanceList.size());
				clientInstanceList = new ArrayList<Instance>(
						awsHelper.updateInstancesByIds(new ArrayList<String>(benchmarkClientIdList.subList(0, clientInstanceCount))));
				logger.info("Client instance count:" + clientInstanceList.size());
			}
			// Update server ip list
			updateConfigFile();
			// Push to GitHub
			pushToGit();
			// Do test
			startTest(serverInstanceCount, clientInstanceCount);
		} catch (Exception e) {
			throw e;
		} finally {
			if (terminatetInstance)
				terminateInstance();
			else
				resetEnv();
		}
	}

	public void launchInstance(int serverInstanceCount, int clientInstanceCount) throws Exception {
		serverInstanceList = new ArrayList<Instance>(awsHelper.lanuchInstance(
				serverInstanceCount, imageName, instanceType, securityGroup));
		clientInstanceList = new ArrayList<Instance>(awsHelper.lanuchInstance(
				clientInstanceCount, imageName, instanceType, securityGroup));

		// Check instance number
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

		// Wait for instance status check initializing
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

		serverInstanceList = new ArrayList<Instance>(
				awsHelper.updateInstances(serverInstanceList));
		clientInstanceList = new ArrayList<Instance>(
				awsHelper.updateInstances(clientInstanceList));
	}

	public void updateConfigFile() throws IOException,
			ParserConfigurationException, TransformerException {
		if (serverInstanceList.size() == 0)
			return;

		updateDeployment();
		updateServerConfig();
		updateTradesimulatorConfig();
	}

	public void updateDeployment() throws ParserConfigurationException, TransformerException{
		// Update Deployment.xml
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("deployment");
			doc.appendChild(rootElement);
	
			// cluster elements
			Element cluster = doc.createElement("cluster");
			rootElement.appendChild(cluster);
	
			// set attribute to cluster element
			Attr attr = doc.createAttribute("hostcount");
			attr.setValue(String.valueOf(serverInstanceList.size()));
			cluster.setAttributeNode(attr);
	
			attr = doc.createAttribute("sitesperhost");
			attr.setValue(benchmarkProp.getProperty("sitesperhost"));
			cluster.setAttributeNode(attr);
	
			attr = doc.createAttribute("kfactor");
			attr.setValue(benchmarkProp.getProperty("kfactor"));
			cluster.setAttributeNode(attr);
			
			
			// systemsettings elements
			Element systemsettings = doc.createElement("systemsettings");
			rootElement.appendChild(systemsettings);
	
			// set attribute to systemsettings element
			Element temptables = doc.createElement("temptables");
			systemsettings.appendChild(temptables);
			
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
			logger.error("Can not parese deployment.xml", e.fillInStackTrace());
			throw e;
		} catch (TransformerException e) {
			logger.error("Can not write deployment.xml to " + deployment, e.fillInStackTrace());
			throw e;
		}
	}

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
			logger.error("Unable to read Server config file: " + serverConfig, e.fillInStackTrace());
			throw e;
		}
	}
	
	public void updateTradesimulatorConfig() throws FileNotFoundException, IOException {

		// Update ServerConfig
		Properties tradesimulatorProp = new Properties();
		
		try {
		tradesimulatorProp.load(new FileInputStream(tradesimulatorConfig));
		tradesimulatorProp.setProperty("tradevolume",benchmarkProp.getProperty("tradevolume"));
		tradesimulatorProp.setProperty("accounts",benchmarkProp.getProperty("accounts"));
		tradesimulatorProp.setProperty("products",benchmarkProp.getProperty("products"));
		tradesimulatorProp.setProperty("tradedays",benchmarkProp.getProperty("tradedays"));
		tradesimulatorProp.store(new FileOutputStream(tradesimulatorConfig), "test");
		}
		catch (FileNotFoundException e) {
			logger.error("Tradesimulator config file : " + tradesimulatorConfig + " not found", e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("Unable to read Tradesimulator config file: " + tradesimulatorConfig, e.fillInStackTrace());
			throw e;
		}
	}
	
	public void pushToGit() throws IOException{
		gitHelper.commitAndPush();
	}

	/**
	 * @throws IOException 
	 * 
	 */
	public void startTest(int serverInstanceCount, int clientInstanceCount) throws IOException {
		serverTaskList = new ArrayList<ServerTask>();
		clientTaskList = new ArrayList<ClientTask>();
		
		// Launch server task
		for (Instance i : serverInstanceList) {
			logger.info("Create server task on instance: "
					+ i.getInstanceId());
			final ServerTask serverTask = new ServerTask(i);
			Thread t = new Thread(new Runnable() {
						public void run() {
							serverTask.StartTask();
						}
					});
			t.start();
			serverTaskList.add(serverTask);
		}

		// Wait for all voltdb start
		try {
			logger.info("Stop 60s for voltdb start");
			Thread.sleep(60 * 1000);
		} catch (InterruptedException e) {
			logger.error("Thread excepetion", e.fillInStackTrace());
		}

		// Launch client task
		for (Instance i : clientInstanceList) {
			logger.info("Create client task on instance: "
					+ i.getInstanceId());
			clientTaskList.add(new ClientTask(i));
		}
		
		ReportGenerator reportGenerator = new ReportGenerator(clientTaskList, gitHelper, benchmarkProp,serverInstanceCount, clientInstanceCount);
		reportGenerator.LoadWorkSpace();
		//Collect Data	
		for(int i = 0; i<queryList.size();i++){
			launchClientTask(queryList.get(i));
			reportGenerator.GenerateReport(queryList.get(i));
		}
		reportGenerator.ArchiveReport();
	}

	public void launchClientTask(final String queryName) {
		for (final ClientTask clientTask : clientTaskList) {
			Thread t = new Thread(new Runnable() {
					public void run() {
						clientTask.StartTask(queryName);
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

	public void resetEnv() {
		// Reset Env
		logger.info("Shutdown server");
		for (ServerTask serverTask : serverTaskList)
				serverTask.ResetEnv();

		logger.info("Shutdown client");
		for (ClientTask clientTask : clientTaskList)
				clientTask.ResetEnv();
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
}
