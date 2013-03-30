import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;


public class UserMonitor extends Thread {
	private String name;
	
	public UserMonitor(String Name) {
		name = Name;
	}
	
	public void run() {
		Employee employee = Assign1_Main.employeeMap.get(name);
		Boolean in_use = true;
		while (in_use) {
			AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(Assign1_Main.credentials);
			GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
			statRequest.setNamespace("AWS/EC2"); //namespace
			statRequest.setPeriod(60); //period of data
			ArrayList<String> stats = new ArrayList<String>();
			stats.add("Average"); 
			stats.add("Sum");
			statRequest.setStatistics(stats);
			statRequest.setMetricName("CPUUtilization");
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			calendar.add(GregorianCalendar.SECOND, -1 * calendar.get(GregorianCalendar.SECOND)); // 1 second ago
			Date endTime = calendar.getTime();
			calendar.add(GregorianCalendar.MINUTE, -10); // 10 minutes ago
			Date startTime = calendar.getTime();
			statRequest.setStartTime(startTime);
			statRequest.setEndTime(endTime);
			ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
			dimensions.add(new Dimension().withName("InstanceId").withValue(employee.instanceIDs.get(0)));
			statRequest.setDimensions(dimensions);
			GetMetricStatisticsResult statResult = cloudWatch.getMetricStatistics(statRequest);
			List<Datapoint> dataList = statResult.getDatapoints();
			Double averageCPU = null;
			for (Datapoint data : dataList){
				averageCPU = data.getAverage();
				System.out.println(name + " average CPU utlilization for last 10 minutes: "+averageCPU);
			}
			
			Date dNow = new Date( );
			SimpleDateFormat ft = new SimpleDateFormat ("HH");
			int hour = Integer.parseInt(ft.format(dNow));
			
			if (averageCPU != null) {
				if ((averageCPU < Assign1_Main.idle_bound) || ((hour > 17) && (hour < 24))) {
					// Detach volume
					InstanceTools.DetachVolume(Assign1_Main.ec2, employee);
					System.out.println("Detach volumes");

					// Backup the image
					InstanceTools.BackImage(Assign1_Main.ec2, employee);
					Assign1_Main.employeeMap.put(employee.name, employee);
					System.out.print("AMI created with imageID: ");
					for (String imageID : employee.imageIDs) {
						System.out.print(imageID + " ");
					}
					try {
						Thread.sleep(10 * 1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println();

					// Terminate the instance
					TerminateInstancesRequest tir = new TerminateInstancesRequest(
							employee.instanceIDs);
					Assign1_Main.ec2.terminateInstances(tir);
					System.out.println(name + "'s instance terminated");
					in_use = false;
				}
				else if ((averageCPU > Assign1_Main.busy_bound) && (employee.instanceIDs.size() < 2)) {
					InstanceTools.SolveBusy(Assign1_Main.ec2, employee);
					Assign1_Main.employeeMap.put(employee.name, employee);
					System.out.println(name + " is busy. New intance is added to him/her");
				}
			}
			
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
