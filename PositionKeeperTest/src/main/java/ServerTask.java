import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.SftpClient;
import org.apache.sshd.client.future.OpenFuture;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;


public class ServerTask extends AwsTask{
	public static Logger logger = LogManager.getLogger(ServerTask.class.getName());
	public ServerTask(Instance instance){
		super(instance);
	}
	
	/**
	 * Clone code from github
	 * Start voltdb
	 */
	@Override
	public void StartTask() throws LoginFailException{
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		ClientSession session;
		try {
			session = client.connect(instance.getPublicIpAddress(), 22).await().getSession();
			Boolean loginSuccess = session.authPassword("voltdb", "voltdb").await().isSuccess();
			int retry = 0;
			while(!loginSuccess&&retry<5){
				logger.info("Instance " + getInstance().getInstanceId() + " login result: " + loginSuccess);
				loginSuccess = session.authPassword("voltdb", "voltdb").await().isSuccess();
				retry++;
			}
			if(retry==5){
				logger.error("Can not login instance:" + getInstance().getInstanceId());
				throw new LoginFailException();
			}
			ClientChannel channel = session.createExecChannel("cd /home/voltdb/voltdb-3.5.0.1/examples && "
															+ "echo $(date) moved to voltdb directory >> server.log && "
															+ "git clone https://github.com/galatea-aws/Positionkeeper.git > gitcloneresult && "
															+ "cd Positionkeeper && "
															+ "./server.sh >> result");
			OpenFuture  openFuture = channel.open().await();
			logger.info("Instance " + getInstance().getInstanceId() + " channel isopened: " + openFuture.isOpened());
			logger.info("Instance " + getInstance().getInstanceId() + " channel isdone: " + openFuture.isDone());
			Thread.sleep(3 * 1000);
		} catch (Exception e) {
			logger.error("Exception in starting voltdb on server instance "+ instance.getInstanceId(),e.fillInStackTrace());
		}
		finally{
		    client.stop();	
		}
	}
	
	/**
	 * Remove voltdb code
	 * Stop voltdb
	 */
	@Override
	public void ResetEnv() throws LoginFailException{
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		ClientSession session;
		//Exec command
		try{
			session = client.connect(instance.getPublicIpAddress(), 22).await().getSession();
			session.authPassword("voltdb", "voltdb").await().isSuccess();
			ClientChannel channel = session.createExecChannel("cd /home/voltdb/voltdb-3.5.0.1/examples && "
															+ "rm -rf Positionkeeper && "
															+ "pkill -9 java");
			OpenFuture  openFuture =  channel.open().await();
			logger.info("Instance " + getInstance().getInstanceId() + " channel isopened: " + openFuture.isOpened());
			logger.info("Instance " + getInstance().getInstanceId() + " channel isdone: " + openFuture.isDone());
			Thread.sleep(3 * 1000);
		}catch (Exception e) {
			logger.error("Exception in resetting environment on server instance "+ instance.getInstanceId(),e.fillInStackTrace());
		}
		finally{
		    client.stop();	
		}
	}
	
	public void DownloadLog(String gitRevision) throws LoginFailException{
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		ClientSession session;
		InputStream inputStream = null;
		try {
			//Exec command
			session = client.connect(getInstance().getPublicIpAddress(), 22).await().getSession();
			session.authPassword("voltdb", "voltdb").await().isSuccess();
			logger.info("download log from" + getInstance().getInstanceId());
			//Download result file
	        SftpClient c = session.createSftpClient();
	        inputStream = c.read("/home/voltdb/voltdb-3.5.0.1/examples/Positionkeeper/log/volt.log");
	        FileUtils.copyInputStreamToFile(inputStream, new File("voltdblog/" + gitRevision + "_" + getInstance().getInstanceId().replace("-", "_")));
	        c.close();
		} catch (Exception e) {
			logger.error("Exception download log from "+ instance.getInstanceId(),e.fillInStackTrace());
		}
		finally{
		    client.stop();	
		    logger.info("Download ended");
		}
	}
}
