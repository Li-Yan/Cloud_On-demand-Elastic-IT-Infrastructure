import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;


public class Assign1_Main {

	/**
	 * @param args
	 */
	
	static AmazonEC2 ec2;
	static HashMap<String, Employee> employeeMap = new HashMap<String, Employee>();
	static AWSCredentials credentials = null;
	static final int idle_bound = 30;
	static final int busy_bound = 50;
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		try {
			credentials = new PropertiesCredentials(
				Assign1_Main.class.getResourceAsStream("AwsCredentials.properties"));
			ec2 = new AmazonEC2Client(credentials);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.println("Please login with your name:");
			//First deal with instance
			Employee employee = null;
			try {
				String name = input.readLine().trim();
				if ((employee = employeeMap.get(name)) == null) {
					employee = new Employee(name);
					employeeMap.put(name, employee);
				}
				InstanceTools.CreateInstance(ec2, employee);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			employeeMap.put(employee.name, employee);
			System.out.print("Start the instance with instanceIDs: ");
			for (String instanceID : employee.instanceIDs) {
				System.out.print(instanceID + " ");
			}
			System.out.println("\nNew instance is pending, please wait...");
			while (!InstanceTools.CheckRunning(ec2, employee)) {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					//
				}
			}
			System.out.println("The instance is running");
			
			//Second create volume and attach volumeID
			InstanceTools.CreateVolume(ec2, employee);
			InstanceTools.AttachVolume(ec2, employee);
			employeeMap.put(employee.name, employee);
			System.out.print("Attach volumes: ");
			for (int i = 0; i < employee.instanceIDs.size(); i++) {
				System.out.print(employee.instanceIDs.get(i) + "<-" + employee.volumeIDs.get(i) + " ");
			}
			System.out.println();
			UserMonitor userMonitor = new UserMonitor(employee.name);
			userMonitor.start();
		}
	}

}
