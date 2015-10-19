package vmdrsystem;

import vmdrsystem.VMDRSystem_Config;
import vmdrsystem.Util;

import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VMSnapshotManagerThread implements Runnable {
	public void run(){

		try{
			while(true){

				takeHostSnapshot();
				
				takeVMSnapshot();

				Thread.currentThread().sleep(VMDRSystem_Config.getSnapshotThreadDelay()); 
			}
		}
		catch(Exception e){
			System.out.println("Snapshot could not be captured due to following error(s) : "+ e);

		}
	}

	public  void takeVMSnapshot(){

		//Taking VM snapshots
		for (int j=0; j<Util.vms.length; j++)
		{
			if(Util.vms[j] instanceof VirtualMachine)
			{
				removeOld((VirtualMachine)Util.vms[j]);
				takeNew((VirtualMachine)Util.vms[j]);
			}
		}
	}

	public void takeHostSnapshot(){
		VirtualMachine vmHost;
		try{
			for(int j=0;j<Util.hosts.length;j++){

				if(Util.hosts[j] instanceof HostSystem){
					String hostInaVcenter = Util.getHostInVcenter(Util.hosts[j].getName());
					vmHost=(VirtualMachine)new InventoryNavigator(Util.vCenterManagerRootFolder).searchManagedEntity("VirtualMachine", hostInaVcenter);
					removeOld(vmHost);  
					takeNew(vmHost);
				}

			}
		}
		catch(Exception e){
			//Emtpy for now...
		}
	}

	public void removeOld(VirtualMachine vm){
		try{
			Task task = ((VirtualMachine) vm).removeAllSnapshots_Task();      
			if(task.waitForMe()== Task.SUCCESS) 
			{
				//System.out.println("Removing all old  snapshots for : "+ vm.getName());
			}
		}
		catch(Exception e){
			System.out.println("Old snapshot could not be removed for " +vm.getName()+  " due to following error(s) : "+ e);
		}

	}

	@SuppressWarnings("deprecation")
	public void takeNew(VirtualMachine vm){
		synchronized(vm){

			try{
				Task task = ((VirtualMachine) vm).createSnapshot_Task(
						vm.getName()+"_snapshot", null,false, false);

				if(task.waitForMe()==Task.SUCCESS)
				{
					//System.out.println("Snapshot was created. for : "+vm.getName());
				}

			}
			catch(Exception e){

				System.out.println("Snapshot could not be created.");
			}
		}
	}

}
