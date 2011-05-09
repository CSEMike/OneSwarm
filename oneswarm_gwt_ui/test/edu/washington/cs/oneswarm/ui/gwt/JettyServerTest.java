package edu.washington.cs.oneswarm.ui.gwt;

import org.gudy.azureus2.plugins.PluginException;

public class JettyServerTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new JettyServerTest();
    }

    public JettyServerTest() {
        try {
            new OsgwtuiMain().initialize(null);
        } catch (PluginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
