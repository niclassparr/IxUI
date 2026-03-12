package se.ixanon.ixui.server.singleton;

public class CommandUtils {

	private static CommandUtils instance = null;
	
	private CommandUtils() {
		
	}
	
	public static CommandUtils getInstance() {
		if (instance == null)
	        instance = new CommandUtils();
	    return instance;
	}
	
	public boolean runCommand(String command) {
		
		Boolean cmdResult = false;
		
        try {
        	int r = Runtime.getRuntime().exec("/usr/bin/ixuiconf --" + command).waitFor();

			if(r == 0){
				cmdResult = true;
			}
        }
        catch(Exception e){
        	//System.out.println("Failed to run ixuiconf, " + e.getMessage());
        }
        
        if(!cmdResult) {
        	System.out.println("Failed to run ixuiconf, " + command);
        }
        
        return cmdResult;	
	
	}
	
	
}
