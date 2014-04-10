import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.amazonaws.services.ec2.model.Instance;


public class RunBenchmark {
	
	public static Logger logger = LogManager.getLogger(RunBenchmark.class.getName());
	
	public static void main(String[] args) throws Exception {
		PositionKeeperBenchmark pt;
		ConfigurableApplicationContext context = 
				new ClassPathXmlApplicationContext("App.xml");
		ArrayList<PositionKeeperBenchmark> taskList = (ArrayList<PositionKeeperBenchmark>)context.getBean("taskList");
		final BenchmarkCoordinator bc = new BenchmarkCoordinator();
		Thread benchmarkCoordinatorThread = new Thread(new Runnable() {
			public void run() {
				bc.run();
			}
		});
		benchmarkCoordinatorThread.start();
		
		ArrayList<Thread> threadList = new ArrayList<Thread>();
		ArrayList<String> uuidList = new ArrayList<String>();
		for(final PositionKeeperBenchmark pb : taskList){
			String newUuid = UUID.randomUUID().toString();
			pb.uuid = newUuid;
			pb.bc = bc;
			uuidList.add(newUuid);
			Thread pbThread = new Thread(new Runnable() {
				public void run() {
					try {
						pb.run();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						MailHelper.sendJobFailMail();
					}
				}
			});
			pbThread.start();
			threadList.add(pbThread);
			
			if(!pb.isWithNext()){
				for(Thread t: threadList){
					t.join();
				}
				logger.info("Summarize Report Start");
				for(String queryName:pb.queryList){
					for(String uuid:uuidList){
						String sourceXLS = pb.gitFolder + "report/tmp/" + uuid + "_" + queryName + ".xls";
						String targetXLS = pb.gitFolder + "report/Archive/" + pb.instanceType + "/" + queryName + ".xls";
						ReportGenerator.SummarizeReport(sourceXLS, targetXLS);
					}
				}
				logger.info("Summarize Report End");
				uuidList.clear();
				threadList.clear();
				pb.gitHelper.addCommitAndPushReport();
			}
		}
		bc.keepRunning = false;
	}
}
