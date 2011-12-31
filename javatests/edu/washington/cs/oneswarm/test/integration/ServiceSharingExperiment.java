package edu.washington.cs.oneswarm.test.integration;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.f2f.ExperimentInterface;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;

public class ServiceSharingExperiment implements ExperimentInterface {
    private static Logger logger = Logger.getLogger(ServiceSharingExperiment.class.getName());

    @Override
    public String[] getKeys() {
        return new String[] { "share_service" };
    }

    @Override
    public void execute(String command) {
        String[] toks = command.toLowerCase().split("\\s+");
        if (!toks[0].equals("share_service")) {
            logger.warning("unknown command " + command);
            return;
        }
        String name = toks[1];
        long searchKey = Long.parseLong(toks[2]);
        String address = toks[3];
        int port = Integer.parseInt(toks[4]);
        // final OSF2FMain f2fMain = OSF2FMain.getSingelton();
        ServiceSharingManager.getInstance().registerSharedService(searchKey, name,
                new InetSocketAddress(address, port));
        logger.info("adding service: "
                + ServiceSharingManager.getInstance().getSharedService(searchKey));
    }

    @Override
    public void load() {
        return;
    }

}
