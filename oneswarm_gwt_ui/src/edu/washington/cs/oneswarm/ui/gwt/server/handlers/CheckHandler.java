package edu.washington.cs.oneswarm.ui.gwt.server.handlers;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

class CheckHandler extends AbstractHandler {

    public void handle(String target, HttpServletRequest request, HttpServletResponse response,
            int dispatch) throws IOException, ServletException {

        System.out.println("serving check: " + target);
        response.setContentType("image/jpeg");
        response.setStatus(HttpServletResponse.SC_OK);

        System.err.println("***** serving check.");
        InputStream inputstream = getClass().getResourceAsStream(
                FileHandler.mServerRootPath + "1by1.jpg");

        ServletOutputStream outputstream = response.getOutputStream();

        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputstream.read(buffer)) > 0) {
            outputstream.write(buffer, 0, len);
        }
        outputstream.flush();
        outputstream.close();
        inputstream.close();
        // response.getWriter().println("<h1>Hello</h1>");
        ((Request) request).setHandled(true);

    }
}
