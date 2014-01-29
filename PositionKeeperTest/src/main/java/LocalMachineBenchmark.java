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
			pt = new PositionKeeperBenchmark("6","1");
/*			for(int i=pt.benchmarkServerIdList.size();i>=1;i--){
				logger.info("Running server instance: " + i);
				pt.run(i,1);
			}*/
			pt.run(1,1);
		//	MailHelper.sendJobCompleteMail(pt.queryList, pt.benchmarkProp);
		} catch (Exception e) {
			logger.error("Positionkeeper benchmark stopped, please check logs",e.fillInStackTrace());
		//	MailHelper.sendJobFailMail();
		}
	}
}
