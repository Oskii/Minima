package org.minima;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.minima.database.MinimaDB;
import org.minima.objects.base.MiniString;
import org.minima.system.Main;
import org.minima.system.commands.Command;
import org.minima.system.params.GeneralParams;
import org.minima.system.params.GlobalParams;
import org.minima.system.params.ParamConfigurer;
import org.minima.utils.MiniFormat;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;

public class Minima {

	public Minima() {}
	
	/**
	 * Call main() with a set of variables
	 */
	public void mainStarter(final String[] zArgs) {
		
		//Create a separate thread
		Runnable mainrunner = new Runnable() {
			@Override
			public void run() {
				//And call it..
				main( zArgs );
			}
		};
		
		//Run it..
		Thread mainthread=new Thread(mainrunner);
		mainthread.start();
	}
	
	/**
	 * Get ther Main class
	 */
	public static Main getMain() {
		return Main.getInstance();
	}
	
	/**
	 * Run a command on Minima and return the result
	 */
	
	public String runMinimaCMD(String zInput){
		return runMinimaCMD(zInput, true);
	}
	
	public String runMinimaCMD(String zInput, boolean zPrettyJSON){
		//trim it..
		String input = zInput.trim();
    	
    	//Run it..
    	JSONArray res = Command.runMultiCommand(input);
    	
    	//Get the result.. 
    	String result = null;
    	if(zPrettyJSON) {
    		result = MiniFormat.JSONPretty(res);
    	}else {
    		if(res.size() == 1) {
    			result = res.get(0).toString();
    		}else {
    			result = res.toJSONString();
    		}
    	}
    	
		return result;
	}
	
	/**
	 * Main entry point for the Java Application
	 * 
	 * Called by fireStarter on Android
	 */
	public static void main(String[] zArgs) {
		
		//Set the main data folder
		File dataFolder 	= new File(System.getProperty("user.home"),".minima");
		
		//Depends on the VERSION
		File minimafolder 			= new File(dataFolder,GlobalParams.MINIMA_BASE_VERSION);
		minimafolder.mkdirs();
		
		//Set this globally
		GeneralParams.DATA_FOLDER 	= minimafolder.getAbsolutePath();

		ParamConfigurer configurer = null;
		try {
			configurer = new ParamConfigurer()
					.usingConfFile(zArgs)
					.usingEnvVariables(System.getenv())
					.usingProgramArgs(zArgs)
					.configure();
		} catch (ParamConfigurer.UnknownArgumentException ex) {
			System.out.println(ex);
			System.exit(1);
		}

		boolean daemon = configurer.isDaemon();
		boolean rpcenable = configurer.isRpcenable();
		boolean shutdownhook = configurer.isShutDownHook();
		
		//Set the Ports.. If Minima port has changed
		GeneralParams.RPC_PORT 			= GeneralParams.MINIMA_PORT+1;
		GeneralParams.MDSFILE_PORT 		= GeneralParams.MINIMA_PORT+2;
		GeneralParams.MDSCOMMAND_PORT 	= GeneralParams.MINIMA_PORT+3;
		
		//Now lets go..
		MinimaLogger.log("**********************************************");
		MinimaLogger.log("*  __  __  ____  _  _  ____  __  __    __    *");
		MinimaLogger.log("* (  \\/  )(_  _)( \\( )(_  _)(  \\/  )  /__\\   *");
		MinimaLogger.log("*  )    (  _)(_  )  (  _)(_  )    (  /(__)\\  *");
		MinimaLogger.log("* (_/\\/\\_)(____)(_)\\_)(____)(_/\\/\\_)(__)(__) *");
		MinimaLogger.log("*                                            *");
		MinimaLogger.log("**********************************************");
		MinimaLogger.log("Welcome to Minima v"+GlobalParams.MINIMA_VERSION+" - for assistance type help. Then press enter.");
		
		//Main handler..
		Main main = new Main();

		//Are we enabling RPC..
		if(rpcenable) {
			MinimaDB.getDB().getUserDB().setRPCEnabled(true);
			main.getNetworkManager().startRPC();
		}
		
		//A shutdown hook..
		if(shutdownhook) {
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run(){
					MinimaLogger.log("[!] Shutdown Hook..");
					
					//Shut down the whole system
					main.shutdown();
				}
			});
		}

		//Daemon mode has no stdin input
		if(daemon) {
	    	MinimaLogger.log("Daemon Started..");
			
			//Loop while running..
			while (!main.isShutdownComplete()) {
                try {Thread.sleep(250);} catch (InterruptedException e) {}
            }
			
			MinimaLogger.log("Bye bye..");
			
			//All done.. The shutdown hook will exit the system..
			return;
	    }
		
		//Listen for input
		InputStreamReader is    = new InputStreamReader(System.in, MiniString.MINIMA_CHARSET);
	    BufferedReader bis      = new BufferedReader(is);
	    
	    //Loop until finished..
	    while(main.isRunning()){
	        try {
	            //Get a line of input
	            String input = bis.readLine();
	            
	            //Check valid..
	            if(input!=null && !input.equals("")) {
	            	//trim it..
	            	input = input.trim();
	            	
	            	//Run it..
	            	JSONArray res = Command.runMultiCommand(input);
	            	
	            	//Print it out
	            	if(!input.equals("quit")) {
	            		System.out.println(MiniFormat.JSONPretty(res));
	            	}
	            	
	                //Is it quit..
	            	boolean quit = false;
	            	Iterator<JSONObject> it = res.iterator();
	            	while(it.hasNext()) {
	            		JSONObject json = it.next();
	            		if(json.get("command").equals("quit")) {
	            			quit = true;
	            			break;
	            		}
	            	}
	            	
	            	if(quit) {
	            		break;
	            	}
	            }
	            
	        } catch (Exception ex) {
	            MinimaLogger.log(ex);
	        }
	    }
	    
	    //Cross the streams..
	    try {
	        bis.close();
	        is.close();
	    } catch (IOException ex) {
	    	MinimaLogger.log(""+ex);
	    }
	    
	    MinimaLogger.log("Bye bye..");
	}
}
