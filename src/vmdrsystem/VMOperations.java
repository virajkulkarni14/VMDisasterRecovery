package vmdrsystem;

import java.net.URL;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

public class VMOperations {

	public  String getVMIp(VirtualMachine vm){
		return vm.getGuest().getIpAddress();
	}


	public boolean vmPowerOn(VirtualMachine vm){
		boolean powerOnSuccess=false;
		try{
			if(vm!=null){
				Task task = vm.powerOnVM_Task(null);
				String status = 	task.waitForMe();
				if(status==Task.SUCCESS)
				{
					System.out.println("VM " + vm.getName() + " powered On.");
					powerOnSuccess=true;
				}
				else
				{
					System.out.println("VM " + vm.getName() + " failed to power on.");
					powerOnSuccess=false;
				}			
			}

		}
		catch (Exception e){
			System.out.println("VM could not power on due to following error(s) : " +e );
		}
		return powerOnSuccess;

	}

	public boolean vmPowerOff(VirtualMachine vm){
		boolean powerOffSuccess=false;
		try{
			if(vm!=null){
				Task task = vm.powerOffVM_Task();
				String status = 	task.waitForMe();
				if(status==Task.SUCCESS)
				{
					System.out.println("VM " + vm.getName() + " powered Off.");
					powerOffSuccess=true;
				}
				else
				{
					System.out.println("VM " + vm.getName() + " failed to power off.");
					powerOffSuccess=false;
				}			
			}

		}
		catch (Exception e){
			System.out.println("VM could not power off due to following error(s) : "+e);
		}
		return powerOffSuccess;

	}

}



