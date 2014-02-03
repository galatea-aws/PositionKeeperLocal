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
/*		try {
			BufferedReader br = new BufferedReader(new FileReader("TaskList"));
			String line = null;
			pt = new PositionKeeperBenchmark();
			br.readLine();
			while((line=br.readLine())!=null){
				String[] taskArgs = line.split(",");
				if(taskArgs.length<5)
					continue;
				int serverInstanceCount = Integer.parseInt(taskArgs[0]);
				int clientInstanceCount = Integer.parseInt(taskArgs[1]);
				String tradeVolume = taskArgs[2];
				String sitesperhost = taskArgs[3];
				String kfactor = taskArgs[4];
				pt.run(0, 4, 0, 1, tradeVolume, sitesperhost, kfactor);
			}
			MailHelper.sendJobCompleteMail(pt.queryList, pt.benchmarkProp);
		} catch (Exception e) {
			logger.error("Positionkeeper benchmark stopped, please check logs",e.fillInStackTrace());
			MailHelper.sendJobFailMail();
		}*/
		
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
						e.printStackTrace();
					}
				}
			});
			pbThread.start();
			threadList.add(pbThread);
			
			if(!pb.isWithNext()){
				for(Thread t: threadList){
					t.join();
				}
				for(String queryName:pb.queryList){
					for(String uuid:uuidList){
						String sourceXLS = pb.gitFolder + "report/tmp/" + uuid + "_" + queryName + ".xls";
						String targetXLS = pb.gitFolder + "report/Archive/" + pb.instanceType + "/" + queryName + ".xls";
						SummarizeReport(sourceXLS, targetXLS);
					}
				}
				uuidList.clear();
				threadList.clear();	
			}
		}
		bc.keepRunning = false;
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
