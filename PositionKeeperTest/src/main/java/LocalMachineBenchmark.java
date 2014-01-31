import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.services.ec2.model.Instance;


public class LocalMachineBenchmark {
	
	public static Logger logger = LogManager.getLogger(LocalMachineBenchmark.class.getName());
	
	public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerException {
		PositionKeeperBenchmark pt;
		try {
			BufferedReader br = new BufferedReader(new FileReader("TaskList"));
			String line = null;
			pt = new PositionKeeperBenchmark();
			br.readLine();
			while((line=br.readLine())!=null){
				String[] taskArgs = line.split(",");
				pt.run(Integer.parseInt(taskArgs[0]),Integer.parseInt(taskArgs[1]), taskArgs[2], taskArgs[3],taskArgs[4]);
			}
/*			for(int i=pt.benchmarkServerIdList.size();i>=1;i--){
				logger.info("Running server instance: " + i);
				pt.run(i,1);
			}*/
		//	MailHelper.sendJobCompleteMail(pt.queryList, pt.benchmarkProp);
		} catch (Exception e) {
			logger.error("Positionkeeper benchmark stopped, please check logs",e.fillInStackTrace());
		//	MailHelper.sendJobFailMail();
		}
	}
}
