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
	private ByteArrayOutputStream outputStream;
	private Executor exec;
	private PumpStreamHandler streamHandler;
	public GitHelper(String folderPath){
		this.folderPath = folderPath;
		outputStream = new ByteArrayOutputStream();
		exec = new DefaultExecutor();
		streamHandler = new PumpStreamHandler(outputStream);
	}
	
	public GitHelper(){
		outputStream = new ByteArrayOutputStream();
		exec = new DefaultExecutor();
		streamHandler = new PumpStreamHandler(outputStream);
	}
	
	public void commitAndPush() throws IOException{
		
		loadGitFolder(folderPath);
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
		loadGitFolder(folderPath+"/report");
		
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
		loadGitFolder(folderPath);
	    //get revision
	    CommandLine cl;
		cl = new CommandLine("git");
		cl.addArgument("rev-parse");
		cl.addArgument("HEAD");
	    try {
			exec.execute(cl);
		}catch (IOException e) {
			e.printStackTrace();
		}
	    return(outputStream.toString().replace("\n", ""));
	}
	
	private void loadGitFolder(String path){
		exec = new DefaultExecutor();
		File file = new File(path);
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.error("Unable to create file: " + file.getPath(), e.fillInStackTrace());
			}
		outputStream = new ByteArrayOutputStream();
		streamHandler = new PumpStreamHandler(outputStream);
		exec.setWorkingDirectory(file);
	    exec.setStreamHandler(streamHandler);
	}
}
