import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;


public class ServerTask extends AwsTask{
	public static Logger logger = LogManager.getLogger(ServerTask.class.getName());
	public ServerTask(Instance instance){
		super(instance);
	}
	
	@Override
	public void StartTask(){
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		ClientSession session;
		try {
			session = client.connect(instance.getPublicIpAddress(), 22).await().getSession();
			session.authPassword("voltdb", "voltdb").await().isSuccess();
			ClientChannel channel = session.createExecChannel("cd /home/voltdb/voltdb-3.5.0.1/examples && "
															+ "git clone https://github.com/galatea-aws/Positionkeeper.git >> gitcloneresult && "
															+ "cd Positionkeeper && "
															+ "./server.sh >> result");
			channel.open().await();
		} catch (Exception e) {
			logger.error("Exception in starting voltdb on server instance "+ instance.getInstanceId(),e.fillInStackTrace());
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
															+ "rm -rf Positionkeeper >> a && "
															+ "pkill -9 java");
			channel.open().await();
		}catch (Exception e) {
			logger.error("Exception in resetting environment on server instance "+ instance.getInstanceId(),e.fillInStackTrace());
		}
		finally{
		    client.stop();	
		}
	}
}
