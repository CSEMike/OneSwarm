package edu.washington.cs.oneswarm.f2f;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Properties;

import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

public class Log {

    public final static int LT_INFO = LoggerChannel.LT_INFORMATION;
    public final static int LT_WARNING = LoggerChannel.LT_WARNING;
    public final static int LT_ERROR = LoggerChannel.LT_ERROR;

    private static Logger logger;
    private final static long startTime = System.currentTimeMillis();
    private final static NumberFormat numberFormat = new DecimalFormat("000,000.000");

    public static void setLogger(Logger l) {
        logger = l;
        Exception error = null;
        try {
            File logSettings = new File("F2FLogSettings.properties");
            Properties logProps = new Properties();
            FileInputStream in;
            in = new FileInputStream(logSettings);
            logProps.load(in);
            in.close();

            for (Object f2fClass : logProps.keySet()) {
                if (f2fClass instanceof String) {
                    String classUrl = (String) f2fClass;
                    Object val = logProps.get(classUrl);
                    if (val instanceof String) {
                        String v = (String) val;
                        if (v.toLowerCase().equals("true") || v.equals("1")
                                || v.toLowerCase().equals("on") || v.toLowerCase().equals("yes")) {
                            try {
                                Class c = l.getPluginInterface().getPluginClassLoader()
                                        .loadClass(classUrl);
                                Field f = c.getDeclaredField("logToStdOut");
                                f.setBoolean(null, true);
                                System.out.println("enable logging for: '" + classUrl + "'");
                            } catch (ClassNotFoundException e) {
                                error = e;
                            } catch (SecurityException e) {
                                error = e;
                            } catch (NoSuchFieldException e) {
                                error = e;
                            } catch (IllegalArgumentException e) {
                                error = e;
                            } catch (IllegalAccessException e) {
                                error = e;
                            }
                        }
                    }
                }

            }
        } catch (FileNotFoundException e) {
            // this is ok
        } catch (IOException e) {
            error = e;
        }
        if (error != null) {
            System.out.println("got Error when setting friend-to-friend log settings: " + error);
        }
    }

    static {

    }

    public static void main(String[] args) {
        // just test the static init
    }

    public static void log(String text) {
        plog(LoggerChannel.LT_INFORMATION, text, true);
    }

    public static void log(int level, String text) {
        plog(level, text, true);
    }

    public static void log(String text, boolean enabled) {
        plog(LoggerChannel.LT_INFORMATION, text, enabled);
    }

    public static void log(int level, String text, boolean enabled) {
        plog(level, text, enabled);
    }

    private static HashMap<String, LoggerChannel> channels = new HashMap<String, LoggerChannel>();

    private static void plog(int level, String text, boolean enabled) {
        if (enabled) {
            String className;
            String methodName;
            int lineNumber;

            Exception e = new Exception();
            StackTraceElement[] st = e.getStackTrace();
            StackTraceElement first_line = st[2];
            className = first_line.getClassName();
            className = className.substring(className.lastIndexOf(".") + 1);
            methodName = first_line.getMethodName();
            lineNumber = first_line.getLineNumber();

            String channelKey = "osf2f:" + className;
            if (!channels.containsKey(channelKey)) {
                channels.put(channelKey, logger.getChannel(channelKey));
            }
            channels.get(channelKey).log(level, text);
            if (enabled) {
                System.out
                        .println(numberFormat.format((System.currentTimeMillis() - startTime) / 1000.0)
                                + ": "
                                + className
                                + "."
                                + methodName
                                + ":"
                                + lineNumber
                                + ": "
                                + text);
            }
        }
    }
}
