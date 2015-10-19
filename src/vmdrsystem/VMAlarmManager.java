package vmdrsystem;

import java.rmi.RemoteException;

import com.vmware.vim25.Action;
import com.vmware.vim25.AlarmAction;
import com.vmware.vim25.AlarmSpec;
import com.vmware.vim25.AlarmState;
import com.vmware.vim25.AlarmTriggeringAction;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.InvalidName;
import com.vmware.vim25.MethodAction;
import com.vmware.vim25.MethodActionArgument;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SendEmailAction;
import com.vmware.vim25.StateAlarmExpression;
import com.vmware.vim25.StateAlarmOperator;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.VirtualMachine;

public class VMAlarmManager {

	public static AlarmManager am = Util.si.getAlarmManager();

	public void setAlarmOnAllVM(){

		for (int j=0; j<Util.vms.length; j++)
		{
			try{
				if(Util.vms[j] instanceof VirtualMachine)
				{
					VirtualMachine vm = (VirtualMachine) Util.vms[j];
					Alarm alarms[] = am.getAlarm(vm);
					for (int i = 0; i < alarms.length; i++) {
						if (alarms[i].getAlarmInfo().getName()
								.contains("PoweredOff_"+vm.getName())) {
							alarms[i].removeAlarm();
						}
					}

					setalarm(vm);

				}
			}
			catch (Exception e){
				System.out.println("Alarm creation failed due to following error(s) : "+ e);			
			}
		}
	}


	static StateAlarmExpression createStateAlarmExpression()
	{

		StateAlarmExpression expression = 
				new StateAlarmExpression();
		expression.setType("VirtualMachine");
		expression.setStatePath("runtime.powerState");
		expression.setOperator(StateAlarmOperator.isEqual);
		expression.setRed("poweredOff");

		return expression;

	}

	static MethodAction createPowerOnAction() 
	{
		MethodAction action = new MethodAction();
		action.setName("PowerOnVM_Task");
		MethodActionArgument argument = new MethodActionArgument();
		argument.setValue(null);
		action.setArgument(new MethodActionArgument[] { argument });
		return action;
	}

	static SendEmailAction createEmailAction() 
	{
		System.out.println("create email action begins...");
		SendEmailAction action = new SendEmailAction();
		action.setToList("cmpe283t15@mailinator.com");
		//action.setCcList("");
		action.setSubject("Alarm - {alarmName} on {targetName}\n");
		action.setBody("Description:{eventDescription}\n"
				+ "TriggeringSummary:{triggeringSummary}\n"
				+ "newStatus:{newStatus}\n"
				+ "oldStatus:{oldStatus}\n"
				+ "target:{target}");
		System.out.println("create email action ends...");
		return action;
	}

	static AlarmTriggeringAction createAlarmTriggerAction(Action action) 
	{
		AlarmTriggeringAction alarmAction = 
				new AlarmTriggeringAction();
		alarmAction.setYellow2red(true);
		alarmAction.setAction(action);
		return alarmAction;
	}




	public void setalarm(VirtualMachine vm)
	{

		AlarmSpec spec = new AlarmSpec(); 
		StateAlarmExpression expression =  createStateAlarmExpression();
		AlarmAction methodAction = createAlarmTriggerAction(createPowerOnAction());
		spec.setExpression(expression);
		spec.setName("PoweredOff_"+vm.getName());

		spec.setDescription("Monitor VM powered off state");
		spec.setEnabled(true);  
		try {
			am.createAlarm(vm, spec);

			System.out.println("----- New alarm created for VM : "+ vm.getName()+" -----");
		} catch (InvalidName e) {
			e.printStackTrace();
		} catch (DuplicateName e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public static boolean getAlarmStatus(VirtualMachine vm)
	{
		AlarmState [] as=  vm.getTriggeredAlarmState();

		if(as!=null){
			if(as.length>0)
				return true;
			else return false;
		}
		return false;
	}


}
