package org.gudy.azureus2.ui.webplugin.remoteui.xml.server;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;

public class XMLHTTPServerPlugin implements Plugin {
    public void initialize(PluginInterface pi) {
    	pi.getLogger().getChannel("xmlhttp-legacy").logAlert(LoggerChannel.LT_ERROR,
    			pi.getUtilities().getLocaleUtilities().getLocalisedMessageText("xmlhttp.legacy.error"));
    }
}