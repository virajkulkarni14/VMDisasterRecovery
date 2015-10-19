package vmdrsystem;

import java.util.ArrayList;

import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VMFailureRecoveryThread implements Runnable{


	VHostOperations vhop = new VHostOperations();
	VMOperations vmop = new VMOperations();
	public static ArrayList<String> ongoingRecoveryVM = new ArrayList<String>();



	public void run()
	{
		try{
			while(true){
				Thread.currentThread().sleep(VMDRSystem_Config.getHeartbeatThreadDelay()); 
				monitorVMHeartBeat();
			}	
		}
		catch (Exception e){
			System.out.println("VMFailure Recovery Thread has following error(s):" + e);
		}
	}


	public void monitorVMHeartBeat(){

		System.out.println("------------------------------- Initiate HeartBeat Checking Cycle -------------------------------------------------");
		try{
			for(int j=0; j<Util.vms.length; j++)
			{
				if(Util.vms[j] instanceof VirtualMachine)
				{
					VirtualMachine vm = (VirtualMachine) Util.vms[j];

					System.out.println("------ VM Name : "+vm.getName()+" ----- "+"VM IP Address : "+vm.getGuest().getIpAddress() +" ------ " +vm.getGuestHeartbeatStatus());

					if(ongoingRecoveryVM.contains(vm.getName()))
					{
						if(!Util.ping(vm.getGuest().getIpAddress()))
						{

							if(vm.getRuntime().getPowerState()==vm.getRuntime().powerState.poweredOff)
							{
								vmop.vmPowerOn(vm);
							}
							System.out.println(vm.getName()+"  : Failure recovery already in progress!");
							continue;
						}
						else
							ongoingRecoveryVM.remove(vm.getName());
					}

					if(!Util.ping(vm.getGuest().getIpAddress()))  // If VM does not respond to ping
					{

						System.out.println("Pinging VM : "+ vm.getName()+"  failed.");

						if(vm.getRuntime().getPowerState() == vm.getRuntime().powerState.poweredOff) 
						{

							if(VMAlarmManager.getAlarmStatus(vm))  // checking whether user has powered off by checking alarmstate
								System.out.println("----- "+vm.getName() + " powered off by the user. No failure recovery required. -----");
							else
								System.out.println("User did not power off the system, could be some other error!");
						}else
							overcomeVMFailOver(vm);
					}
					else // VM is responds to ping
						System.out.println(vm.getName() +" : successfully responding to ping and is healthy. ");

				}
			}
			System.out.println("----------------------------- HeartBeat Checking Cycle Ends ---------------------------------------------------");
		}
		catch (Exception e){
			System.out.println("Heartbeat Monitor threw following exception(s) : "+e);
		}
	}

	public void overcomeVMFailOver(VirtualMachine vm){
		System.out.println("------ Failure Recovery for VM "+ vm.getName() +"  initiated -------");

		HostSystem parentHost = null;

		parentHost = vhop.returnParentVhost(vm);

		System.out.println("Parent vHost of : " +vm.getName()+" is "+ parentHost.getName());

		try{

			if(Util.ping(vhop.getHostIp(parentHost))) //case 1 : vHost is running properly. restart the VM and apply the snapshot in same vhost.
			{
				System.out.println("Parent vHost is responding to ping...");
				Task task = vm.revertToCurrentSnapshot_Task(null);


				if(task.waitForTask()==Task.SUCCESS)
				{
					ongoingRecoveryVM.add(vm.getName());             //add vm to the recovery list
					System.out.println("------------------ "+vm.getName()+" restored to latest stable snapshot. ---------------------");

					if(vm.getRuntime().getPowerState()==vm.getRuntime().powerState.poweredOff)
					{
						vmop.vmPowerOn(vm);
					}


				}else
					System.out.println("----- Failure in restoring  VM : "+vm.getName() +" to its snapshot. -----");

			}
			else   //case 2  vhost ping failed, start vhost recovery
			{ 
				System.out.println("------- Parent vHost: "+ parentHost.getName()+" failed to respond to ping! Initiating failure recovery for this vHost... --------");
				if(vhop.revertHostToSnapshot(parentHost)) //step 1 restore vhost to snapshot
				{
					ongoingRecoveryVM.add(vm.getName());     //add vm to the recovery list
					System.out.println("-------------------- vHost successfully restored to snapshot -------------------");
					
					//check host state and if powered off, power it on.
					
					if(vhop.getHostPowerState(parentHost) == vm.getRuntime().getPowerState().poweredOff)
					{
						vhop.powerOnHost(parentHost);

					}

					if(vm.getRuntime().getPowerState() == vm.getRuntime().powerState.poweredOff)
					{
						vmop.vmPowerOn(vm);
					}

				}
				else   
				{

					System.out.println("vHost could not be recovered by restoring to a stable snapshot.");
					System.out.println("Searching another vHost for migrating VM .......");
					String newHostIp= giveAnotherHostIp(parentHost);

					if(!newHostIp.isEmpty())  //case 3 migrate to another host
					{

						System.out.println("New healthy host found : "+ newHostIp);

						System.out.println("--- Initiating VM Migration to New Host ----");
						vm.powerOffVM_Task();
						migrateVM(vm,newHostIp);
						vm.powerOnVM_Task(null);

					}
					else                        //case 4 add new host and migrate vm to it
					{
						System.out.println("No healthy Host Found. Adding a New Host...");
						String newIp=  vhop.addNewVhost();
						vm.powerOffVM_Task();
						migrateVM(vm,newIp);
						vm.powerOnVM_Task(null);
					}

				}

			}
		}
		catch (Exception e)
		{		
			System.out.println("VM Failover generated following exception(s): "+e);
		}
	}


	public String giveAnotherHostIp(HostSystem parentHost){
		String newHostIp=null;
		try{

			if(Util.hosts.length>1) 
			{
				for(int i=0 ;i<Util.hosts.length;i++)
				{
					if(Util.hosts[i].getName()!=parentHost.getName())
					{
						if(Util.ping(vhop.getHostIp((HostSystem)Util.hosts[i])))
						{
							newHostIp=vhop.getHostIp((HostSystem)Util.hosts[i]);
							System.out.println("New healthy Host : "+vhop.getHostIp((HostSystem)Util.hosts[i])+" found.");
							return newHostIp;
						}  
						//remove vhost from list as it is unreachable
					}
				}

				// call to addingVhost()  as we didnt found any working vshot
			}
		}
		catch (Exception e){}

		return newHostIp;

	}


	
	public static void migrateVM(VirtualMachine vm, String newHost) throws Exception {
		
		HostSystem hs= (HostSystem) new InventoryNavigator(Util.rootFolder).searchManagedEntity("HostSystem", newHost);
		ComputeResource cr = (ComputeResource) hs.getParent();
		Task task = vm.migrateVM_Task(cr.getResourcePool(), hs, VirtualMachineMovePriority.highPriority, VirtualMachinePowerState.poweredOff);
		System.out.println("Trying to migrate " + vm.getName() + " to " + hs.getName());
		if (task.waitForTask() == task.SUCCESS) {
			System.out.println("Migrated virtual machine: " + vm.getName() + " successfully!");
		} else {
			System.out.println("VM Migration failed!");
		}
	}


}
