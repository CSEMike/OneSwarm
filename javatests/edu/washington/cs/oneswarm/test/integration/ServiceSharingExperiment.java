package edu.washington.cs.oneswarm.test.integration;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.f2f.ExperimentInterface;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;

public class ServiceSharingExperiment implements ExperimentInterface {
    private static Logger logger = Logger.getLogger(ServiceSharingExperiment.class.getName());

    @Override
    public String[] getKeys() {
        return new String[] { "share_service", "expose_client" };
    }

    @Override
    public void execute(String command) {
        String[] toks = command.toLowerCase().split("\\s+");
        if (toks[0].equals("share_service")) {
            String name = toks[1];
            long searchKey = Long.parseLong(toks[2]);
            String address = toks[3];
            int port = Integer.parseInt(toks[4]);
            // final OSF2FMain f2fMain = OSF2FMain.getSingelton();
            ServiceSharingManager.getInstance().registerSharedService(searchKey, name,
                    new InetSocketAddress(address, port));
            logger.info("adding service: "
                    + ServiceSharingManager.getInstance().getSharedService(searchKey));
        } else if (toks[0].equals("expose_client")) {
            String name = toks[1];
            long key = Long.parseLong(toks[2]);
            int port = Integer.parseInt(toks[3]);
            ServiceSharingManager.getInstance().registerClientService(name, port, key);
            logger.info("adding client: "
                    + ServiceSharingManager.getInstance().getClientService(key));
        } else {
            logger.warning("Unknown Service command: " + toks[0]);
            return;
        }
    }

    @Override
    public void load() {
        return;
    }

}
