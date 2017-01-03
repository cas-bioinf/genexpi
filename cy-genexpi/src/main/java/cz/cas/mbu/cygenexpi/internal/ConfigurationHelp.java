package cz.cas.mbu.cygenexpi.internal;

import cz.cas.mbu.cygenexpi.internal.tasks.ConfigurationTask;

/**
 * A class that provides UI for hints in the {@link ConfigurationTask} via a custom tunable handler.
 * @author MBU
 *
 */
public class ConfigurationHelp {
	public enum MainMessage {None, InitError, NoDevices, DeviceTestFailed}
	
	private MainMessage mainMessage;
	private String details;
	
	
	public ConfigurationHelp(MainMessage mainMessage, String details) {
		super();
		this.mainMessage = mainMessage == null ? MainMessage.None : mainMessage;
		this.details = details == null ? "" : details;
	}

	

	public MainMessage getMainMessage() {
		return mainMessage;
	}



	public String getDetails() {
		return details;
	}	
	
}
