import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;


public class InstanceTools {
	public static String securityGroupName = "ly2278SecurityGroup";
	public static String keyName = "cloud";
	public static String default_imageId = "ami-76f0061f"; //Basic 32-bit Amazon Linux AMI
	public static int minInstanceCount = 1; // create 1 instance
	public static int maxInstanceCount = 1;
	public static AmazonCloudWatchClient cloudWatch = null;
    public static GetMetricStatisticsRequest statRequest = null;
	
    public static void CreateInstance(AmazonEC2 ec2, Employee Emp) {
    	if (Emp.imageIDs.size() == 0) {
    		Emp.imageIDs.add(default_imageId);
    	}
    	Emp.instanceIDs.clear();
    	Emp.instanceAvailableZones.clear();
    	
		for (String imageId : Emp.imageIDs) {
			RunInstancesRequest rir = new RunInstancesRequest();
			rir.withImageId(imageId);
			rir.withInstanceType("t1.micro");
			rir.withMinCount(minInstanceCount);
			rir.withMaxCount(maxInstanceCount);
			rir.withKeyName(keyName);
			rir.withSecurityGroups(securityGroupName);

			RunInstancesResult result = ec2.runInstances(rir);
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			List<Instance> resultInstance = result.getReservation()
					.getInstances();
			String createdInstanceId = null;
			String createdInstanceAvailableZone = null;
			for (Instance ins : resultInstance) {
				createdInstanceId = ins.getInstanceId();
				createdInstanceAvailableZone = ins.getPlacement()
						.getAvailabilityZone();
			}
			List<String> resources = new LinkedList<String>();
			List<Tag> tags = new LinkedList<Tag>();
			Tag nameTag = new Tag("Name", "Assign1-" + Emp.name);
			resources.add(createdInstanceId);
			tags.add(nameTag);
			CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
			ec2.createTags(ctr);
			
			//Deregister image
			if (!imageId.equalsIgnoreCase(default_imageId)) {
				DeregisterImageRequest dir = new DeregisterImageRequest(imageId);
				ec2.deregisterImage(dir);
			}

			Emp.instanceIDs.add(createdInstanceId);
			Emp.instanceAvailableZones.add(createdInstanceAvailableZone);
		}
		
		Emp.imageIDs.clear();
    }
    
    public static void SolveBusy(AmazonEC2 ec2, Employee Emp) {
    	int current_index = Emp.instanceIDs.size();
    	
    	RunInstancesRequest rir = new RunInstancesRequest();
		rir.withImageId(default_imageId);
		rir.withInstanceType("t1.micro");
		rir.withMinCount(minInstanceCount);
		rir.withMaxCount(maxInstanceCount);
		rir.withKeyName(keyName);
		rir.withSecurityGroups(securityGroupName);
		
		RunInstancesResult result = ec2.runInstances(rir);
		List<Instance> resultInstance = result.getReservation()
				.getInstances();
		String createdInstanceId = null;
		String createdInstanceAvailableZone = null;
		for (Instance ins : resultInstance) {
			createdInstanceId = ins.getInstanceId();
			createdInstanceAvailableZone = ins.getPlacement()
					.getAvailabilityZone();
		}
		List<String> resources = new LinkedList<String>();
		List<Tag> tags = new LinkedList<Tag>();
		Tag nameTag = new Tag("Name", "Assign1-" + Emp.name);
		resources.add(createdInstanceId);
		tags.add(nameTag);
		CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
		ec2.createTags(ctr);
		
		Emp.instanceIDs.add(createdInstanceId);
		Emp.instanceAvailableZones.add(createdInstanceAvailableZone);
		System.out.println("New instance :" + createdInstanceId + "is created for use: " + Emp.name);
		
		System.out.println("New instance is pending, please wait...");
		try {
			Thread.sleep(60 * 1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (!InstanceTools.CheckRunning(ec2, Emp)) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				//
			}
		}
		
		InstanceTools.CreateExtraVolume(ec2, Emp, current_index);
		InstanceTools.AttachVolume(ec2, Emp, current_index);
		System.out.print("Attach new volumes: " + Emp.instanceIDs.get(current_index) + "<-" 
				+ Emp.volumeIDs.get(current_index));
    }
    
    public static void CreateVolume(AmazonEC2 ec2, Employee Emp) {
    	if (Emp.volumeIDs.size() == 0) {
    		for (String instanceAvailableZone : Emp.instanceAvailableZones) {
    			CreateVolumeRequest cvr = new CreateVolumeRequest();
    	        cvr.setAvailabilityZone(instanceAvailableZone);
    	        cvr.setSize(1);
    	        CreateVolumeResult volumeResult = ec2.createVolume(cvr);
    	    	String createdVolumeId = volumeResult.getVolume().getVolumeId();
    	    	Emp.volumeIDs.add(createdVolumeId);
    		}
    	}
    }
    
    public static void CreateExtraVolume(AmazonEC2 ec2, Employee Emp, int index) {
    	String instanceAvailableZone = Emp.instanceAvailableZones.get(index);
    	CreateVolumeRequest cvr = new CreateVolumeRequest();
        cvr.setAvailabilityZone(instanceAvailableZone);
        cvr.setSize(1);
        CreateVolumeResult volumeResult = ec2.createVolume(cvr);
    	String createdVolumeId = volumeResult.getVolume().getVolumeId();
    	Emp.volumeIDs.add(createdVolumeId);
    }
    
    public static void AttachVolume(AmazonEC2 ec2, Employee Emp) {
    	for (int i = 0; i < Emp.instanceIDs.size(); i++) {
    		String instanceID = Emp.instanceIDs.get(i);
    		String volumeID = Emp.volumeIDs.get(i);
    		AttachVolumeRequest avr = new AttachVolumeRequest();
        	avr.setVolumeId(volumeID);
        	avr.setInstanceId(instanceID);
        	avr.setDevice("/dev/sdf");
        	ec2.attachVolume(avr);
    	}
    }
    
    public static void AttachVolume(AmazonEC2 ec2, Employee Emp, int index) {
    	String instanceID = Emp.instanceIDs.get(index);
		String volumeID = Emp.volumeIDs.get(index);
		AttachVolumeRequest avr = new AttachVolumeRequest();
		avr.setVolumeId(volumeID);
		avr.setInstanceId(instanceID);
		avr.setDevice("/dev/sdf");
		ec2.attachVolume(avr);
    }
    
    public static void DetachVolume(AmazonEC2 ec2, Employee Emp) {
    	for (int i = 0; i < Emp.instanceIDs.size(); i++) {
    		String instanceID = Emp.instanceIDs.get(i);
    		String volumeID = Emp.volumeIDs.get(i);
        	DetachVolumeRequest dvr = new DetachVolumeRequest();
        	dvr.setVolumeId(volumeID);
        	dvr.setInstanceId(instanceID);
        	ec2.detachVolume(dvr);
    	}
    }
    
    public static void BackImage(AmazonEC2 ec2, Employee Emp) {
    	for (String instanceID : Emp.instanceIDs) {
    		CreateImageRequest cir = new CreateImageRequest(instanceID, "AMI-" + instanceID);
    		CreateImageResult cire = ec2.createImage(cir);
    		Emp.imageIDs.add(cire.getImageId());
    	}
    }
    
    public static Boolean CheckRunning(AmazonEC2 ec2, Employee Emp) {
    	DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
    	List<Reservation> reservations = describeInstancesRequest.getReservations();
        Set<Instance> instances = new HashSet<Instance>();
        for (Reservation reservation : reservations) {
        	instances.addAll(reservation.getInstances());
        }
		for (Instance ins : instances) {
			for (String instanceID : Emp.instanceIDs) {
				if (ins.getInstanceId().equalsIgnoreCase(instanceID)) {
					if (ins.getState().getName().equalsIgnoreCase("pending")) {
						return false;
					}
				}
			}
		}
		return true;
    }
}
