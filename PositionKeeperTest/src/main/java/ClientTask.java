
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.SftpClient;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;


public class ClientTask extends AwsTask{
	public static Logger logger = LogManager.getLogger(ClientTask.class.getName());
	public ClientTask(Instance instance){
		super(instance);
	}
	
	@Override
	public void StartTask(String queryname){
		ResetEnv();
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		ClientSession session;
		InputStream inputStream = null;
		try {
			//Exec command
			session = client.connect(getInstance().getPublicIpAddress(), 22).await().getSession();
			session.authPassword("voltdb", "voltdb").await().isSuccess();
			ClientChannel channel = session.createExecChannel("cd /home/voltdb/voltdb-3.5.0.1/examples && "
															+ "git clone https://github.com/galatea-aws/Positionkeeper.git >> gitcloneresult && "
															+ "cd Positionkeeper && "
															+ "./run.sh positionkeeper " + queryname + " >> " + queryname + "_detail");
			channel.open().await();
			channel.waitFor(ClientChannel.CLOSED, 0);
			
			//Download result file
	        SftpClient c = session.createSftpClient();
	        inputStream = c.read("/home/voltdb/voltdb-3.5.0.1/examples/Positionkeeper/" + queryname + "_detail");
	        FileUtils.copyInputStreamToFile(inputStream, new File(queryname + "_" + getInstance().getInstanceId()));
	        c.close();
		} catch (Exception e) {
			logger.error("Exception in running query " + queryname +" on client instance "+ instance.getInstanceId(),e.fillInStackTrace());
		}
		finally{
		    client.stop();	
		}
	}
	
	@Override
	public void ResetEnv(){
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		ClientSession session;
		//Exec command
		try{
		session = client.connect(instance.getPublicIpAddress(), 22).await().getSession();
		session.authPassword("voltdb", "voltdb").await().isSuccess();
		ClientChannel channel = session.createExecChannel("cd /home/voltdb/voltdb-3.5.0.1/examples && "
														+ "rm -rf Positionkeeper");
		channel.open().await();
		//channel.waitFor(ClientChannel.CLOSED, 0);
		}catch (Exception e) {
			logger.error("Exception in resetting environment on client instance "+ instance.getInstanceId(),e.fillInStackTrace());
		}
		finally{
		    client.stop();	
		}
	}
}
