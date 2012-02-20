package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SHA1Simple;

import edu.uw.cse.netlab.utils.ByteManip;
import edu.washington.cs.oneswarm.f2f.BigFatLock;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ClientServiceDTO;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SharedServiceDTO;

/**
 * This class manages local client and server services. It keeps records of
 * active
 * services, and allows new services to be registered.
 * 
 * @author isdal
 * 
 */
public class ServiceSharingManager {

    public static final int CHT_DEBUG_SEARCH_PREFIX = 1650551921;
    public static final String DEBUG_KEY_NOUNCE = "rkAx1ucFeYOQUDdwzJlG2dwTAcwuRkv8pDQBAuK0Dv78NkDQHfNcspFpTmlvLgHxgjnIJpATmXaTzyrb";
    private final static String CLIENT_SERVICE_CONFIG_KEY = "SERVICE_CLIENT";
    private final static ServiceSharingManager instance = new ServiceSharingManager();
    private static BigFatLock lock = OverlayManager.lock;

    public final static Logger logger = Logger.getLogger(ServiceSharingManager.class.getName());

    private final static String SHARED_SERVICE_CONFIG_KEY = "SHARED_SERVICE";

    public static ServiceSharingManager getInstance() {
        return instance;
    }

    public HashMap<Long, ClientService> clientServices = new HashMap<Long, ClientService>();

    public HashMap<Long, SharedService> sharedServices = new HashMap<Long, SharedService>();

    private ServiceSharingManager() {
    }

    /*
     * Debug services used before we launch service sharing. Add a service
     * sharing to cht.oneswarm.org port 11743 for testing.
     */
    private void enableDebugServices() {
        try {
            boolean beta = COConfigurationManager.getBooleanParameter("oneswarm.beta.updates",
                    false);
            boolean pl = System.getProperty("oneswarm.experimental.config.file") != null;
            if (!(beta || pl)) {
                return;
            }
            if (!COConfigurationManager.getBooleanParameter("Send Version Info", false)) {
                return;
            }
            if (!COConfigurationManager.getBooleanParameter("OSF2F.Use DHT Proxy", false)) {
                return;
            }
            long searchKey = createChtDebugSearchKey(COConfigurationManager.getStringParameter(
                    "ID", ""));
            InetAddress cht = InetAddress.getByName("128.208.2.60");
            registerSharedService(searchKey, "cht.oneswarm.org", new InetSocketAddress(cht, 11743),
                    false);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected long createChtDebugSearchKey(String id) throws IOException,
            UnsupportedEncodingException {
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.write(DEBUG_KEY_NOUNCE.getBytes("UTF-8"));
        input.write(id.getBytes("UTF-8"));

        byte[] key = new SHA1Simple().calculateHash(input.toByteArray());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // magic number for recognizing debug searches
        dos.writeInt(CHT_DEBUG_SEARCH_PREFIX);
        for (int i = 0; i < 4; i++) {
            dos.writeByte(key[i]);
        }
        long searchKey = ByteManip.btol(baos.toByteArray());
        return searchKey;
    }

    public void clearLocalServices() {
        ArrayList<Long> currentServices = new ArrayList<Long>(clientServices.keySet());
        for (Long key : currentServices) {
            deregisterClientService(key);
        }
        currentServices.clear();
        currentServices.addAll(sharedServices.keySet());
        for (Long key : currentServices) {
            deregisterServerService(key);
        }
    }

    public void deregisterClientService(long searchKey) {
        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    CLIENT_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            services.remove(Long.valueOf(searchKey));
            COConfigurationManager.setParameter(CLIENT_SERVICE_CONFIG_KEY, services);
            ClientService cs = clientServices.remove(searchKey);
            if (cs != null) {
                cs.deactivate();
            }
        } finally {
            lock.unlock();
        }
    }

    public void deregisterServerService(long searchKey) {
        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    SHARED_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            services.remove(Long.valueOf(searchKey));
            COConfigurationManager.setParameter(SHARED_SERVICE_CONFIG_KEY, services);
            SharedService service = sharedServices.remove(searchKey);
            if (service != null) {
                service.clean();
            }
        } finally {
            lock.unlock();
        }
    }

    private void enableLowLatencyNetwork() {
        // If the LowLatencyMessageWrite is enabled an attempt is made to write
        // messages to the channel straight away, so there is no need to set
        // network.control.write.idle.time to a lower value. The default setting
        // will delay outgoing messages up to 50ms.

        // This will trigger tcp read selects every x ms (instead of the default
        // 25ms). The default setting delays incoming messages up to 25ms.
        COConfigurationManager.setParameter("network.tcp.read.select.time", 4);
    }

    public ClientService getClientService(long infohashhash) {
        ClientService service = null;
        try {
            lock.lock();
            service = clientServices.get(infohashhash);
        } finally {
            lock.unlock();
        }

        return service;
    }

    public List<ClientService> getClientServices() {
        List<ClientService> cs = new ArrayList<ClientService>();
        try {
            lock.lock();
            cs.addAll(clientServices.values());
        } finally {
            lock.unlock();
        }

        Collections.sort(cs);
        return cs;
    }

    public SharedService getSharedService(long infohashhash) {
        SharedService service = null;
        try {
            lock.lock();
            service = sharedServices.get(infohashhash);
        } finally {
            lock.unlock();
        }

        if (service == null || !service.isEnabled()) {
            return null;
        }
        return service;
    }

    public List<SharedService> getSharedServices() {
        List<SharedService> cs = new ArrayList<SharedService>();
        try {
            lock.lock();
            cs.addAll(sharedServices.values());
        } finally {
            lock.unlock();
        }

        Collections.sort(cs);
        return cs;

    }

    public SharedService handleSearch(OSF2FHashSearch search) {
        long searchKey = search.getInfohashhash();
        return getSharedService(searchKey);
    }

    public void loadConfiguredClientServices() {
        boolean enableLowLatencyNetwork = false;
        enableDebugServices();

        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    CLIENT_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            for (Long key : services) {
                ClientService cs = new ClientService(key);
                if (cs.getPort() == -1) {
                    continue;
                }
                clientServices.put(key, cs);
                cs.activate();
                enableLowLatencyNetwork = true;
            }
        } finally {
            lock.unlock();
        }

        if (enableLowLatencyNetwork) {
            enableLowLatencyNetwork();
        }
    }

    public void loadConfiguredSharedServices() {
        boolean enableLowLatencyNetwork = false;

        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    SHARED_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            for (Long key : services) {
                SharedService cs = new SharedService(key);
                sharedServices.put(key, cs);
                enableLowLatencyNetwork = true;
            }
        } finally {
            lock.unlock();
        }

        if (enableLowLatencyNetwork) {
            enableLowLatencyNetwork();
        }
    }

    public int registerClientService(String name, int suggestedPort, long searchKey) {
        enableLowLatencyNetwork();
        int port = suggestedPort;
        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    CLIENT_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            ClientService cs = clientServices.get(searchKey);
            // Create a new
            if (cs == null) {
                if (!services.contains(Long.valueOf(searchKey))) {
                    services.add(Long.valueOf(searchKey));
                    COConfigurationManager.setParameter(CLIENT_SERVICE_CONFIG_KEY, services);
                }
                cs = new ClientService(searchKey);
                cs.setName(name);
                cs.setPort(port);
                clientServices.put(searchKey, cs);
            } else {
                cs.setName(name);
                cs.setPort(port);
            }
            // Activate if needed, and update the port if not set
            if (!cs.active) {
                cs.activate();
            }
            port = cs.getPort();
        } finally {
            lock.unlock();
        }
        return port;
    }

    public void registerSharedService(long searchKey, String name, InetSocketAddress address) {
        registerSharedService(searchKey, name, address, true);
    }

    public void registerSharedService(long searchKey, String name, InetSocketAddress address,
            boolean loadAutomatically) {
        logger.fine("Registering service: key=" + searchKey + " name=" + name + " address="
                + address);
        enableLowLatencyNetwork();
        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    SHARED_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            SharedService ss = sharedServices.get(searchKey);
            if (ss == null) {
                // create a new service
                if (loadAutomatically && !services.contains(Long.valueOf(searchKey))) {
                    services.add(Long.valueOf(searchKey));
                    COConfigurationManager.setParameter(SHARED_SERVICE_CONFIG_KEY, services);
                }
                ss = new SharedService(searchKey);
                ss.setName(name);
                ss.setAddress(address);
                logger.info("created new service: " + ss);
                sharedServices.put(searchKey, ss);
            } else {
                // update name and address
                ss.setName(name);
                ss.setAddress(address);
            }
        } finally {
            lock.unlock();
        }

    }

    public void updateClients(ArrayList<ClientServiceDTO> services) {

        HashSet<ClientService> toRemove = new HashSet<ClientService>();
        toRemove.addAll(getClientServices());

        for (ClientServiceDTO serviceDTO : services) {
            if (serviceDTO.isDummy()) {
                continue;
            }

            long key = Long.valueOf(serviceDTO.getSearchKey(), 16);
            ClientService existing = getClientService(key);
            if (existing == null) {
                registerClientService(serviceDTO.getName(), serviceDTO.getPort(), key);
            } else {
                existing.setName(serviceDTO.getName());
                existing.setPort(serviceDTO.getPort());
                // Remove this one from the set of services to remove
                // after we are done.
                toRemove.remove(existing);
            }
        }

        for (ClientService clientService : toRemove) {
            deregisterClientService(clientService.serverSearchKey);
        }
    }

    public void updateSharedServices(ArrayList<SharedServiceDTO> services)
            throws UnknownHostException {

        HashSet<SharedService> toRemove = new HashSet<SharedService>();
        toRemove.addAll(getSharedServices());

        for (SharedServiceDTO serviceDTO : services) {
            if (serviceDTO.isDummy()) {
                continue;
            }
            long key = Long.valueOf(serviceDTO.getSearchKey(), 16);
            SharedService existing = getSharedService(key);
            if (existing == null) {
                registerSharedService(
                        key,
                        serviceDTO.getName(),
                        new InetSocketAddress(InetAddress.getByName(serviceDTO.address), serviceDTO
                                .getPort()));
            } else {
                existing.setName(serviceDTO.getName());
                existing.setAddress(new InetSocketAddress(
                        InetAddress.getByName(serviceDTO.address), serviceDTO.getPort()));
                // Remove this one from the set of services to remove
                // after we are done.
                toRemove.remove(existing);
            }
        }

        for (SharedService sharedService : toRemove) {
            deregisterServerService(sharedService.searchKey);
        }
    }
}
