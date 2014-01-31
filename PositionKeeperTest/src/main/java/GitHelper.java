import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class GitHelper {
	private String folderPath;
	public static Logger logger = LogManager.getLogger(GitHelper.class.getName());
	
	public GitHelper(String folderPath){
		this.folderPath = folderPath;
	}
	
	public GitHelper(){
		
	}
	
	public void commitAndPush() throws IOException{
		Executor exec = new DefaultExecutor();
		File file = new File(folderPath);
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.error("Unable to create file: " + file.getPath(), e.fillInStackTrace());
				throw e;
			}
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		exec.setWorkingDirectory(file);
		exec.setStreamHandler(streamHandler);
		// commit
		CommandLine cl;
		cl = new CommandLine("git");
		cl.addArgument("commit");
		cl.addArgument("-m");
		cl.addArgument("'test'");
		cl.addArgument("-a");
		try {
			exec.execute(cl);
			logger.info(outputStream.toString());
			outputStream.reset();
		} catch (ExecuteException e) {
			logger.error("Unable to commit file: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("IO Exception in committing file: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		}

		// push
		cl = new CommandLine("git");
		cl.addArgument("push");
		try {
			exec.execute(cl);
			logger.info(outputStream.toString());
			outputStream.reset();
		} catch (ExecuteException e) {
			logger.error("Unable to push file to git: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("IO Exception in pushing file: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		}
	}
	
	public void addCommitAndPushReport()throws IOException{
		Executor exec = new DefaultExecutor();
		File file = new File(folderPath+"/report");
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.error("Unable to create file: " + file.getPath(), e.fillInStackTrace());
				throw e;
			}
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		exec.setWorkingDirectory(file);
		exec.setStreamHandler(streamHandler);

		// add
		CommandLine cl;
		cl = new CommandLine("git");
		cl.addArgument("add");
		cl.addArgument(".");
		try {
			exec.execute(cl);
			logger.info(outputStream.toString());
			outputStream.reset();
		} catch (ExecuteException e) {
			logger.error("Unable to add file: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("IO Exception in adding file: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		}
		
		// commit
		cl = new CommandLine("git");
		cl.addArgument("commit");
		cl.addArgument("-m");
		cl.addArgument("'test'");
		cl.addArgument("-a");
		try {
			exec.execute(cl);
			logger.info(outputStream.toString());
			outputStream.reset();
		} catch (ExecuteException e) {
			logger.error("Unable to commit file: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("IO Exception in committing file: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		}

		// push
		cl = new CommandLine("git");
		cl.addArgument("push");
		try {
			exec.execute(cl);
			logger.info(outputStream.toString());
			outputStream.reset();
		} catch (ExecuteException e) {
			logger.error("Unable to push file to git: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		} catch (IOException e) {
			logger.error("IO Exception in pushing file: " + cl.getExecutable(), e.fillInStackTrace());
			throw e;
		}
		
	}
	
	public  String getHeadRevision() {
		Executor exec = new DefaultExecutor();
		File file = new File(folderPath);
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.error("Unable to create file: " + file.getPath(), e.fillInStackTrace());
			}
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		exec.setWorkingDirectory(file);
	    CommandLine cl;
		cl = new CommandLine("git");
		cl.addArgument("rev-parse");
		cl.addArgument("HEAD");
	    exec.setStreamHandler(streamHandler);
	    try {
			exec.execute(cl);
		}catch (IOException e) {
			e.printStackTrace();
		}
	    return(outputStream.toString());
	}
}
