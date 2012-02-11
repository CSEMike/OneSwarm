package org.oneswarm.util;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/** A class of static convenience methods for working with classes reflectively. */
public class ReflectionUtils {

    private static Logger logger = Logger.getLogger(ReflectionUtils.class.getName());

    /**
     * Returns true iff running with experimental support. Used as an
     * optimization to prevent invoking reflective methods unnecessarily.
     */
    public static boolean isExperimental() {
        String experiment = System.getProperty("oneswarm.experimental.config.file");
        return experiment != null && experiment.trim().length() > 0;
    }

    /**
     * Attempts to invoke the given experimental method with the given
     * arguments.
     */
    public static Object invokeExperimentalMethod(String methodName, Object[] args, Class<?>[] types) {

        // Just in case we missed a check elsewhere.
        if (isExperimental() == false) {
            return null;
        }

        try {
            Class<?> expConfigManagerClass = Class
                    .forName("edu.washington.cs.oneswarm.planetlab.ExperimentConfigManager");
            if (expConfigManagerClass != null) {
                Method getMethod = expConfigManagerClass.getMethod("get");
                Object configManager = getMethod.invoke(null, new Object[] {});
                if (configManager != null) {
                    logger.finest("Got experimental manager");
                    Method givenMethod = expConfigManagerClass.getMethod(methodName, types);
                    return givenMethod.invoke(configManager, args);
                } else {
                    logger.warning("configManager is null -- classes found but experimental mode "
                            + "not enabled");
                }
            } else {
                logger.warning("Couldn't find ExperimentalConfigManager.");
            }

        } catch (ClassNotFoundException e) {
            logger.warning("PlanetLab classes not found -- not running in experimental mode.");
            e.printStackTrace();
        } catch (Exception e) {
            logger.warning("PlanetLab classes failed to load -- not running in experimental mode.");
            e.printStackTrace();
        }
        return null;
    }

}
