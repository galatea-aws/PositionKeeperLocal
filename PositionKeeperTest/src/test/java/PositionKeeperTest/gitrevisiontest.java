package PositionKeeperTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
public class gitrevisiontest {
	public static String execToString(String command) throws Exception {
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    CommandLine cl;
		cl = new CommandLine("git");
		cl.addArgument("rev-parse");
		cl.addArgument("HEAD");
	    DefaultExecutor exec = new DefaultExecutor();
	    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
	    exec.setStreamHandler(streamHandler);
	    exec.execute(cl);
	    return(outputStream.toString());
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(execToString("echo %cd%"));
	}
}
