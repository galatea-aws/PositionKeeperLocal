import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.sshd.SshClient;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;


public abstract class AwsTask {
	protected Instance instance;
	
	public AwsTask(Instance instance){
		this.instance = instance;
	}
	public void StartTask() throws LoginFailException{
		
	}
	
	public void StartTask(String queryname) throws LoginFailException {
		
	}
	
	public void ResetEnv() throws LoginFailException{
		
	}
	
	public Instance getInstance() {
		return instance;
	}
}
