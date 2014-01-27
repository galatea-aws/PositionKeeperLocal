import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;


public class AwsHelper {
	private static AwsHelper awsHelper = null;
	public static Logger logger = LogManager.getLogger(AwsHelper.class.getName());
	public AmazonEC2Client amazonEC2Client;
	
	public static AwsHelper getAwsHelper() throws Exception{
		if(awsHelper==null)
		try {
			awsHelper = new AwsHelper();
		} catch (Exception e) {
			logger.error("Aws client not connected");
			throw e;
		}
		return awsHelper;
	}
	
	private AwsHelper() throws Exception {
		AWSCredentials credentials = null;
		try {
			credentials = new PropertiesCredentials((new FileInputStream(
					"rootkey.csv")));
		} catch (FileNotFoundException e) {
			logger.error("Unable to find Credential file");
			throw e;
		} catch (IOException e) {
			logger.error("Unable to read Credential file");
			throw e;
		}
		this.amazonEC2Client = new AmazonEC2Client(credentials);
	}
	
	public ArrayList<Instance> lanuchInstance(int serverNumber, String imageName, String instanceType, String securityGroup) throws AmazonClientException {
		try {
			// Send run instance request
			RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
			runInstancesRequest.withImageId(imageName)
					.withInstanceType(instanceType).withMinCount(serverNumber)
					.withMaxCount(serverNumber)
					.withSecurityGroups(securityGroup);

			// Get result
			RunInstancesResult runInstancesResult = amazonEC2Client
					.runInstances(runInstancesRequest);
			Reservation reservation = runInstancesResult.getReservation();
			ArrayList<Instance> instanceList = new ArrayList<Instance>(
					reservation.getInstances());
			return instanceList;
		} catch (AmazonClientException e) {
			logger.error("Unable to launch instance", e.fillInStackTrace());
			throw e;
		}
	}
	
	public boolean isStatusChecksInitializing(ArrayList<Instance> instanceList) {
		// Send request
		ArrayList<String> instanceIds = new ArrayList<String>();
		for (Instance i : instanceList) {
			instanceIds.add(i.getInstanceId());
		}

		DescribeInstanceStatusRequest describeInstanceRequest = new DescribeInstanceStatusRequest()
				.withInstanceIds(instanceIds);
		DescribeInstanceStatusResult describeInstanceResult = amazonEC2Client
				.describeInstanceStatus(describeInstanceRequest);
		List<InstanceStatus> instanceStatusList = describeInstanceResult
				.getInstanceStatuses();
		for (InstanceStatus instanceStatus : instanceStatusList) {
			System.out.println(instanceStatus.getInstanceStatus().getStatus());
			if (instanceStatus.getInstanceStatus().getStatus().equals("initializing")) {
				System.out.println("Instances" + instanceStatus.getInstanceId()
						+ " status checks initialzing");
				return true;
			}

		}
		return false;
	}
	
	public void terminateInstance(ArrayList<Instance> instanceList) throws AmazonClientException{
		try {
			// Send request
			ArrayList<String> instanceIds = new ArrayList<String>();
			for (Instance i : instanceList) {
				instanceIds.add(i.getInstanceId());
			}
			TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
			terminateInstancesRequest.setInstanceIds(instanceIds);
			// Get result
			TerminateInstancesResult terminateInstancesResult = amazonEC2Client
					.terminateInstances(terminateInstancesRequest);
			for(InstanceStateChange isc: terminateInstancesResult.getTerminatingInstances()){
				logger.info("Instance " + isc.getInstanceId() + " PreviousStatus: " + isc.getPreviousState() + " CurrentState: " + isc.getCurrentState());
			}
		} catch (AmazonClientException e) {
			logger.error("Unable to terminate instances: " + instanceList.toString(), e.fillInStackTrace());
			throw e;
		}
	}
	
	public ArrayList<Instance> updateInstances(ArrayList<Instance> instanceList){
		ArrayList<String> instanceIds = new ArrayList<String>();
		for (Instance i : instanceList) {
			instanceIds.add(i.getInstanceId());
		}
		return updateInstancesByIds(instanceIds);
	}
	
	public ArrayList<Instance> updateInstancesByIds(ArrayList<String> instanceIdList){
		DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest()
		.withInstanceIds(instanceIdList);
		DescribeInstancesResult describeInstanceResult = amazonEC2Client
				.describeInstances(describeInstanceRequest);
		ArrayList<Reservation> reservationList = new ArrayList<Reservation>(
				describeInstanceResult.getReservations());
		
		// Create server task and start voltdb
		if (reservationList.size() > 0) {
			return new ArrayList<Instance>(reservationList.get(0).getInstances());
		} else {
			System.out.println("Server reservationlist size is 0");
			return new ArrayList<Instance>();
		}
	}
}
