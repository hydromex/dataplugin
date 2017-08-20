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

    	File[] copiedScripts = null;
    	//Get the date range and check if is valid
    	String startDate = "";
    	String endDate = "";
    	
    	if(args != null){
    		//Get the date range and check if is valid
        	startDate = (args.length > 0) ? args[0] : "";
        	endDate = (args.length > 1) ? args[1] : "";
	    	if(!startDate.isEmpty() && !isValidDate(startDate)){
	    		MessageMediator.sendMessage(getName(), "The date argument has to be in YYYY-MM-DD format",
	    				MessageMediator.ERROR_MESSAGE);
	    		startDate = "";
	    	}
	    	
	    	if(!endDate.isEmpty() && !isValidDate(endDate)){
	    		MessageMediator.sendMessage(getName(), "The date argument has to be in YYYY-MM-DD format",
	    				MessageMediator.ERROR_MESSAGE);
	    		endDate = "";
	    	}
    	}
	    	
    	try {
            //Copy script file to temporary directory
            File copiedScript = FileUtil.copyFilesTo(pluginDir, SCRIPT)[0];
	    	
	    	//Create command like
	    	//python [pluginDir]/licor_summary.py [pluginDir] <<optional date range>>
            List<String> command = Arrays.asList(PYTHON_COMMAND, copiedScript.getPath(), pluginDir.getPath(), startDate, endDate);

            //Log
	    	MessageMediator.sendMessage(getName(), "Executing " +
	    			copiedScript.getName(), MessageMediator.INFO_MESSAGE);

            ProcessUtil.executeProcess(command.toArray(new String[command.size()]));

            //filename = 'summary_report_' + startDate + '_' + endDate + '.txt'
	    	File dataFile = new File(pluginDir, "data.json");
	    	MessageMediator.sendMessage(getName(), dataFile.exists() + "", MessageMediator.INFO_MESSAGE);
	    	
	    	ArrayList<String> processedFiles = new ArrayList<>();
	    	if(dataFile.exists())
	    		processedFiles.add(dataFile.getPath());
	    		
	    	COPY_STRATEGY.copy("queretarosummary", "UTF-8", "json", processedFiles.toArray(new String[0]));

            MessageMediator.sendMessage(getName(), "Data export "
                    + "execution finished", MessageMediator.INFO_MESSAGE);
    	} catch (IOException | InterruptedException ex) {
    		MessageMediator.sendMessage(getName(), ex.toString(),
    				MessageMediator.ERROR_MESSAGE);
    		ok = -1;
    	}
    	return ok;
	}

	private boolean isValidDate(String dateString) {
		//Format YYYY-MM-DD
	    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	    try {
	        df.parse(dateString);
	        return true;
	    } catch (ParseException e) {
	        return false;
	    }
	}
}