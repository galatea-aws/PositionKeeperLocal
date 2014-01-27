import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.amazonaws.services.ec2.model.Instance;


public class LocalMachineBenchmark {
	public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerException {
		PositionKeeperBenchmark pt = new PositionKeeperBenchmark();
		ArrayList<Instance> serverInstanceList = new ArrayList<Instance>();
		ArrayList<Instance> clientInstanceList = new ArrayList<Instance>();
		Instance i = new Instance();
		i.setPrivateIpAddress("192.168.52.128");
		i.setPublicIpAddress("192.168.52.128");
		i.setInstanceId("Server1");
		serverInstanceList.add(i);
		i = new Instance();
		i.setPrivateIpAddress("192.168.52.131");
		i.setPublicIpAddress("192.168.52.131");
		i.setInstanceId("Client1");
		clientInstanceList.add(i);
		pt.serverInstanceList = serverInstanceList;
		pt.clientInstanceList = clientInstanceList;
		
		// Update server ip list
		pt.updateConfigFile();
		// Push to GitHub
		pt.pushToGit();
		// Do test
		pt.startTest(1, 1);
	}
}
