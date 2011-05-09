package edu.washington.cs.oneswarm.ui.gwt.server.handlers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.OsgwtuiMain;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.server.FlashVideoConverterPlayer;
import edu.washington.cs.oneswarm.ui.gwt.server.OneSwarmUIServiceImpl;
import edu.washington.cs.oneswarm.ui.gwt.server.PreviewImageGenerator;

public class MultiHandler extends ServletHandler {

    private static Logger logger = Logger.getLogger(MultiHandler.class.getName());

    private String sessionID = "";

    // private ServletHandler servletHandler;

    private final FileHandler fileHandler;
    private final BrowseHandler browseHandler;
    private final CheckHandler checkHandler;
    // private StatsHandler statsHandler = null;

    // private final VideoConvertionHandler videoHandler;
    //
    // private final VideoImageHandler videoImageHandler;

    private final CoreInterface coreInterface;
    private final boolean remote;

    public MultiHandler(CoreInterface coreInterface, boolean remote) {
        super();
        this.remote = remote;
        this.coreInterface = coreInterface;
        // initialize the handlers
        sessionID = coreInterface.getSessionID();
        logger.info("initializing");
        this.initializeServletHandler();

        this.fileHandler = new FileHandler();
        this.checkHandler = new CheckHandler();
        this.browseHandler = new BrowseHandler();

        /**
         * Only respond to stats requests if experiment mode is enabled (then
         * too, these require the same access as normal remote access)
         */
        // if( ExperimentConfigManager.isEnabled() ) {
        // this.statsHandler = new StatsHandler(coreInterface);
        // logger.info("Creating stats handler due to property set: oneswarm.experimental.config.file");
        // }

        // this.videoHandler = new VideoConvertionHandler(coreInterface);
        //
        // this.videoImageHandler = new VideoImageHandler(coreInterface);
    }

    @Override
    public void handle(String target, HttpServletRequest request, HttpServletResponse response,
            int dispatch) throws IOException, ServletException {

        Request baseRequest = request instanceof Request ? (Request) request : HttpConnection
                .getCurrentConnection().getRequest();
        Response baseResponse = response instanceof Response ? (Response) response : HttpConnection
                .getCurrentConnection().getResponse();

        String referer = request.getHeader("Referer");
        String userAgent = request.getHeader("User-Agent");

        logger.finest("request from: '" + request.getRemoteAddr() + "' '" + target + "' "
                + (referer != null ? referer + " " : "")
                + (userAgent != null ? userAgent + " " : ""));

        try {
            securityCheck(baseRequest, baseResponse);
        } catch (IOException e) {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                    response.getOutputStream()));
            out.append("Request canceled, got security exception: " + e.getMessage());
            out.close();
            throw e;
        }
        boolean hasCookie = hasCookie(baseRequest);
        // System.out.println(request.getSession().getId());

        // if the request doesn't have the cookie
        if (!hasCookie) {
            if (request.getMethod().endsWith("GET")) {
                putCookie(baseResponse, remote);
            } else {
                throw new IOException("no cookie for POST request");
            }
        }

        // fix path issues with 1.6
        if (target.startsWith("/oneswarmgwt/oneswarmgwt/")) {
            target = target.substring("/oneswarmgwt".length());
        }

        if (target.startsWith("/stream")) {
            logger.fine("GOT STREAM REQUEST");
            logger.fine("file=" + request.getParameter("file") + " start="
                    + request.getParameter("start"));
        }
        // TODO we should change the /image to something more random...
        if ((target.startsWith(OneSwarmConstants.servletPath) && hasCookie)
                || (target.startsWith("/oneswarmgwt/image") && hasCookie && !target
                        .startsWith("/oneswarmgwt/images/"))) {
            logger.finer("using servlet handler: " + target);
            super.handle(target, request, response, dispatch);
        } else if ((target.startsWith("/oneswarmgwt/flv_movie") && hasCookie)) {
            logger.fine("flash handler: " + target);
            (new FlashVideoConverterPlayer(remote)).doGet(request, response);
        } else if (target.startsWith(OneSwarmConstants.BROWSE_SHARE_PATH)
                || target.startsWith(OneSwarmConstants.DOWNLOAD_SHARE_PATH)) {
            logger.fine("browse handler: " + target);
            browseHandler.handle(target, request, response, dispatch);
            // } else if( target.startsWith(OneSwarmConstants.STATS_PATH) &&
            // statsHandler != null ) {
            // logger.fine("stats handler: " + target);
            // statsHandler.doGet(request, response);
        } else {
            logger.finer("file handler: " + target);
            // send everything else to the file handler
            fileHandler.handle(target, request, response, dispatch);
        }

    }

    private void initializeServletHandler() {
        logger.info("initializing UI service impl...");
        super.addServletWithMapping(new ServletHolder(new OneSwarmUIServiceImpl(coreInterface,
                remote)), OneSwarmConstants.servletPath);
        super.addServletWithMapping(new ServletHolder((new PreviewImageGenerator())),
                "/oneswarmgwt/image");
        // super.addServletWithMapping(new ServletHolder(
        // (new FlashVideoConverterPlayer())),
        // "/flv_movie");

        try {
            super.initialize();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return;
    }

    private void securityCheck(Request request, Response response) throws IOException {

        /*
         * if we are remote accessing, the remote host could be anything. The
         * security is verified with basic auth instead
         */
        if (remote) {
            return;
        }
        /*
         * check that the HOST field is 127.0.0.1
         */
        String hostHeader = request.getServerName();
        if (!OsgwtuiMain.LOCALHOST.equals(hostHeader)) {
            throw new IOException("request not for " + OsgwtuiMain.LOCALHOST
                    + ", dropping connection");
        }
        String remoteHost = request.getRemoteAddr();

        if (!checkRemoteHost(remoteHost)) {
            throw new IOException("request not from 127.0.0.1, dropping connection");
        }

    }

    private boolean checkRemoteHost(String addr) {
        if (addr.equals("127.0.0.1")) {
            return true;
        }
        return false;
    }

    private boolean hasCookie(Request request) {

        // return true;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getValue().equals(sessionID)) {
                    logger.finer("cookie exists: " + cookie.getName() + " " + cookie.getValue());
                    return true;
                } else {
                    logger.finer("found old cookie: " + cookie.getName() + " " + cookie.getValue());

                }
            }
        }
        return false;
    }

    private void putCookie(Response response, boolean usesProxy) {

        Cookie cookie = new Cookie("OneSwarm", sessionID);
        response.addCookie(cookie);
        if (usesProxy) {
            Cookie pCookie = new Cookie("OneSwarmProxy", "" + usesProxy);
            response.addCookie(pCookie);
        }
        logger.info("Adding cookie: " + cookie.getValue());

    }

}
