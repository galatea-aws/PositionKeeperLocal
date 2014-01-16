import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;


public class Start {
	public static void main(String[] args) throws IOException{
		
		//Git commit and push
		Executor exec = new DefaultExecutor();
		File file = new File("C:\\Users\\wsun\\Desktop\\Positionkeeper");
		if(!file.exists())
			file.createNewFile();
		PumpStreamHandler streamHandler = new PumpStreamHandler();
		exec.setWorkingDirectory(file);
		exec.setStreamHandler(streamHandler);
		//commit
		CommandLine cl = new CommandLine("git");
		cl.addArgument("commit");
		cl.addArgument("-m");
		cl.addArgument("'test'");
		exec.execute(cl);
		//push
		cl = new CommandLine("git");
		cl.addArgument("push");
		exec.execute(cl);
		
		//Connect to aws
	}
}
