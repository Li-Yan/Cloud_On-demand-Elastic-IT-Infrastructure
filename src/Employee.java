import java.util.ArrayList;


public class Employee {
	public String name;
	public ArrayList<String> instanceIDs;
	public ArrayList<String> instanceAvailableZones;
	public ArrayList<String> volumeIDs;
	public ArrayList<String> imageIDs;
	
	public Employee(String Name) {
		name = Name;
		instanceIDs = new ArrayList<String>();
		instanceAvailableZones = new ArrayList<String>();
		volumeIDs = new ArrayList<String>();
		imageIDs = new ArrayList<String>();
	}
}
