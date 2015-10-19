package vmdrsystem;

import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class Util {

	public  static ServiceInstance si;
	public static ServiceInstance vCenterManagerSi;
	public static Folder rootFolder;
	public static Folder vCenterManagerRootFolder;
	public  static ManagedEntity[] dcs;
	public  static ManagedEntity[] hosts;
	public  static ManagedEntity[] vms;

	public Util(){

		try{

			si = new ServiceInstance(new URL(VMDRSystem_Config.getVCenterURL()), VMDRSystem_Config.getVCenterUsername(), 
					VMDRSystem_Config.getVCenterPassword(), true);

			vCenterManagerSi= new ServiceInstance(new URL("https://130.65.132.19/sdk"), VMDRSystem_Config.getAdminVCenterUsername(), 
					VMDRSystem_Config.getVCenterPassword(), true);

			rootFolder = si.getRootFolder();
			vCenterManagerRootFolder=vCenterManagerSi.getRootFolder();

			dcs = new InventoryNavigator(rootFolder).searchManagedEntities(
					new String[][] { {"Datacenter", "name" }, }, true);

			hosts = new InventoryNavigator(rootFolder).searchManagedEntities(
					new String[][] { {"HostSystem", "name" }, }, true);

			vms = new InventoryNavigator(rootFolder).searchManagedEntities(
					new String[][] { {"VirtualMachine", "name" }, }, true);
		}
		catch (Exception e){
			System.out.println("VMMonitor object initialization error : " + e);

		}	
	}

	// pinging the given ip to check whether host is reachable

	public static boolean ping(String ip) throws Exception {
		String cmd = "";

		if (System.getProperty("os.name").startsWith("Windows")) {
			// For Windows
			cmd = "ping -n 3 " + ip;
		} else {
			// For Linux and OSX
			cmd = "ping -c 3 " + ip;
		}

		System.out.println("Ping "+ ip + "......");
		Process process = Runtime.getRuntime().exec(cmd);
		process.waitFor();		
		return process.exitValue() == 0;
	}


	//fetch vHost Name in vCenter Manager

	public static String getHostInVcenter(String host){

		return VHOSTMAP.get(host);
	}

	// store mapping of vHost Names in vCenter Manager

	public static final HashMap<String, String> VHOSTMAP = new HashMap<String, String>() {
		{
			put("130.65.133.31", "T15-vHost01_133.31");
			put("130.65.133.32", "T15-vHost02_133.32");
			put("130.65.133.33", "T15-vHost03_133.33");
			put("130.65.133.34", "T15-vHost01_133.34");
		}
	};

}
