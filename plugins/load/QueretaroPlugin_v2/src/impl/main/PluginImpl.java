package impl.main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CopyStrategy;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import mx.uabc.mxl.iing.azul.dataplugin.util.FileUtil;
import mx.uabc.mxl.iing.azul.dataplugin.util.ProcessUtil;

public class PluginImpl extends Plugin {

    private final PluginDescriptor DESCRIPTOR;
    private final URL SCRIPT;
    private final URL HELP;
    private final CopyStrategy COPY_STRATEGY;

    private String helpContent;


	public PluginImpl(PluginDescriptor descriptor, CopyStrategy strategy, URL ... files) {
        DESCRIPTOR = descriptor;
        SCRIPT = files[0];
        HELP = files[1];
        COPY_STRATEGY = strategy;
	}

    @Override
    protected PluginDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    protected String getHelpContent() {
        if (helpContent != null) {
            return helpContent;
        }
        try {
            helpContent = FileUtil.streamToString(HELP.openStream());
        } catch (IOException e) {
            MessageMediator.sendMessage(getName(), "error reading help content",
                    MessageMediator.ERROR_MESSAGE);
            helpContent = null;
        }

        return helpContent;
    }

    @Override
	public int execute(File pluginDir, String... args) {
		final String PYTHON_COMMAND = "python";

		int ok = 0;

    	if (args != null && args.length > 1) {
	    	File[] copiedScripts = null;
	    	//(Input)
	    	File dirRaw = new File(args[0]);
	    	//(Output) Creating all temporary results in plugin working dir (temp)
	    	File dirResult = new File(args[1]);
	    	String fromDate = (args.length > 2) ? args[2] : "";
	    	String toDate = (args.length > 3) ? args[3] : "";
	    	
	    	if(args.length > 2 && !isValidDate(fromDate)){
	    		MessageMediator.sendMessage(getName(), "The date argument has to be in dd/MM/yyyy format",
	    				MessageMediator.ERROR_MESSAGE);
	    		fromDate = "";
	    	}
	    	
	    	if(args.length > 3 && !isValidDate(toDate)){
	    		MessageMediator.sendMessage(getName(), "The date argument has to be in dd/MM/yyyy format",
	    				MessageMediator.ERROR_MESSAGE);
	    		toDate = "";
	    	}
		    	
	    	try {
		    	//Copy temp scripts to desired directory
//		    	copiedScripts = FileUtil.copyFilesTo(dirRaw.getParentFile(), SCRIPT);
                //Copy script file to temporary directory
                File copiedScript = FileUtil.copyFilesTo(pluginDir, SCRIPT)[0];
		    	
		    	//Create command like
		    	//python [pluginDir]/GHGtoJSON.py <<raw data directory>> <<result data directory>> <<optional date>>
                List<String> command = Arrays.asList(PYTHON_COMMAND, copiedScript.getPath(), dirRaw.getPath(),
                        dirResult.getPath());
                //Add if args present
                if (!fromDate.isEmpty()) {
                    command.add(fromDate);
                }
                if (!toDate.isEmpty()) {
                    command.add(toDate);
                }

                //Log
		    	MessageMediator.sendMessage(getName(), "Executing " +
		    			copiedScript.getName(), MessageMediator.INFO_MESSAGE);

                ProcessUtil.executeProcess(command.toArray(new String[command.size()]));

                //What?
		    	File dataFile = new File(pluginDir, "data.json");
		    	File metadataFile = new File(pluginDir, "metadata.json");
		    	File resultFile = new File(pluginDir, "result.json");
		    	
		    	
		    	ArrayList<String> processedFiles = new ArrayList<>();
		    	if(dataFile.exists())
		    		processedFiles.add(dataFile.getPath());
		    	
		    	if(metadataFile.exists())
		    		processedFiles.add(metadataFile.getPath());
		    	
		    	if(resultFile.exists())
		    		processedFiles.add(resultFile.getPath());
		    		
		    	COPY_STRATEGY.copy("queretaro", "UTF-8", "json", processedFiles.toArray(new String[0]));

                MessageMediator.sendMessage(getName(), "Data export "
                        + "execution finished", MessageMediator.INFO_MESSAGE);
	    	} catch (IOException | InterruptedException ex) {
	    		MessageMediator.sendMessage(getName(), ex.toString(),
	    				MessageMediator.ERROR_MESSAGE);
	    		ok = -1;
	    	}
    	} else {
    		MessageMediator.sendMessage(getName(), "Two arguments are needed to execute"
    				+ " this plugin [Raw data directory, Result data directory]",
    				MessageMediator.ERROR_MESSAGE);
    		ok = 15;
    	}
    	return ok;
	}

	private boolean isValidDate(String dateString) {
		//Format dd/mm/yyyy
	    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
	    try {
	        df.parse(dateString);
	        return true;
	    } catch (ParseException e) {
	        return false;
	    }
	}
}