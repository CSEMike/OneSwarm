/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.libideas.resources.ext;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.libideas.resources.client.ImmutableResourceBundle.Resource;
import com.google.gwt.libideas.resources.client.ImmutableResourceBundle.Transform;
import com.google.gwt.libideas.resources.rebind.Transformer;

import java.net.URL;

/**
 * Utility methods for building ResourceGenerators.
 */
public final class ResourceGeneratorUtil {

  /**
   * Apply Transformers to a resource. The presence of one or more
   * {@link Transform} annotation values will specify the {@link Transformer}
   * class to use. Multiple transformations will be applied in their declared
   * order.
   * 
   * @param <T> the Java type that encapsulates the value of the resource
   * @param logger the TreeLogger context
   * @param method the method to examine
   * @param baseType a class literal which specifies the base type
   * @param input the value to transform
   * @return the transformed value.
   * @throws UnableToCompleteException
   * @deprecated with no replacement
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public static <T> T applyTransformations(TreeLogger logger, JMethod method,
      Class<T> baseType, T input) throws UnableToCompleteException {
    String[] transformerNames;

    if (method.getAnnotation(Transform.class) != null) {
      transformerNames = method.getAnnotation(Transform.class).value();
    } else {
      transformerNames = new String[0];
    }

    logger = logger.branch(TreeLogger.DEBUG,
        "Applying transformations for method " + method.getName());

    try {
      // TODO Implement a short-name system
      for (String className : transformerNames) {

        // This may throw a ClassCastException, which we trap below
        Class<? extends Transformer> clazz = Class.forName(className).asSubclass(
            Transformer.class);

        Transformer<?> t = clazz.newInstance();

        // This, too, may throw a ClassCastException.
        Transformer<T> t2 = t.asSubclass(baseType);
        input = t2.transform(method, input);
      }

      return input;
    } catch (ClassCastException e) {
      logger.log(TreeLogger.ERROR, "The class specified by the Transform"
          + " annotation is not a Transformer<" + baseType.getName() + ">",
          null);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Could not find Transformer", e);
    } catch (IllegalAccessException e) {
      logger.log(TreeLogger.ERROR, "TextTransformers must be public", e);
    } catch (InstantiationException e) {
      logger.log(TreeLogger.ERROR,
          "TextTransformers must have public, no-arg constructors", e);
    }

    throw new UnableToCompleteException();
  }

  /**
   * Return the base filename of a resource. The behavior is similar to the unix
   * command <code>basename</code>.
   * 
   * @param resource the URL of the resource
   * @return the final name segment of the resource
   */
  public static String baseName(URL resource) {
    String path = resource.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  /**
   * Find all resources referenced by a method in a bundle. The method's
   * {@link Resource} annotation will be examined and the specified locations
   * will be expanded into URLs by which they may be accessed on the local
   * system.
   * <p>
   * This method is sensitive to the <code>locale</code> deferred-binding
   * property and will attempt to use a best-match lookup by removing locale
   * components.
   * <p>
   * The current Thread's context ClassLoader will be used to resolve resource
   * locations. If it is necessary to alter the manner in which resources are
   * resolved, use the overload that accepts an arbitrary ClassLoader.
   * 
   * @param logger a TreeLogger that will be used to report errors or warnings
   * @param context the ResourceContext in which the ResourceGenerator is
   *          operating
   * @param method the method to examine for {@link Resource} annotations
   * @return URLs for each {@link Resource} annotation defined on the method.
   * @throws UnableToCompleteException if the method has no resource annotations
   *           or ore or more of the resources could not be found. The error
   *           will be reported via the <code>logger</code> provided to this
   *           method
   */
  public static URL[] findResources(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {
    return findResources(logger,
        Thread.currentThread().getContextClassLoader(), context, method);
  }

  /**
   * Find all resources referenced by a method in a bundle. The method's
   * {@link Resource} annotation will be examined and the specified locations
   * will be expanded into URLs by which they may be accessed on the local
   * system.
   * <p>
   * This method is sensitive to the <code>locale</code> deferred-binding
   * property and will attempt to use a best-match lookup by removing locale
   * components.
   * 
   * @param logger a TreeLogger that will be used to report errors or warnings
   * @param context the ResourceContext in which the ResourceGenerator is
   *          operating
   * @param classLoader the ClassLoader to use when locating resources
   * @param method the method to examine for {@link Resource} annotations
   * @return URLs for each {@link Resource} annotation defined on the method.
   * @throws UnableToCompleteException if the method has no resource annotations
   *           or ore or more of the resources could not be found. The error
   *           will be reported via the <code>logger</code> provided to this
   *           method
   */
  public static URL[] findResources(TreeLogger logger, ClassLoader classLoader,
      ResourceContext context, JMethod method) throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG, "Finding resources");

    String[] resources;
    // Check for Resource annotations first:
    if (method.getAnnotation(Resource.class) != null) {
      Resource resource = method.getAnnotation(Resource.class);
      resources = resource.value();
    } else {
      logger.log(TreeLogger.ERROR, "Method " + method.getName()
          + " does not have a @" + Resource.class.getName() + " annotations.");
      throw new UnableToCompleteException();
    }

    String locale;
    try {
      PropertyOracle oracle = context.getGeneratorContext().getPropertyOracle();
      locale = oracle.getPropertyValue(logger, "locale");
    } catch (BadPropertyValueException e) {
      locale = null;
    }

    URL[] toReturn = new URL[resources.length];

    boolean error = false;
    int tagIndex = 0;
    for (String resource : resources) {
      // Try to find the resource relative to the package.
      URL resourceURL = tryFindResource(classLoader, getPathRelativeToPackage(
          method.getEnclosingType().getPackage(), resource), locale);

      // If we didn't find the resource relative to the package, assume it is
      // absolute.
      if (resourceURL == null) {
        resourceURL = tryFindResource(classLoader, resource, locale);
      }

      if (resourceURL == null) {
        logger.log(TreeLogger.ERROR, "Resource " + resource
            + " not found on classpath. Is the name specified as "
            + "Class.getResource() would expect?");
        error = true;
      }

      toReturn[tagIndex++] = resourceURL;
    }

    if (error) {
      throw new UnableToCompleteException();
    }

    /*
     * In the future, it would be desirable to be able to automatically
     * determine the resource name to use from the method declaration. We're
     * currently limited by the inability to list the contents of the classpath
     * and not having a set number of file extensions to empirically test. It
     * would also be worthwhile to be able to search for globs of files.
     */

    return toReturn;
  }

  /**
   * Converts a package relative path into an absolute path.
   * 
   * @param pkg the package
   * @param path a path relative to the package
   * @return an absolute path
   */
  private static String getPathRelativeToPackage(JPackage pkg, String path) {
    return pkg.getName().replace('.', '/') + '/' + path;
  }

  /**
   * This performs the locale lookup function for a given resource name.
   * 
   * @param classLoader the ClassLoader to use to load the resources
   * @param resourceName the string name of the desired resource
   * @param locale the locale of the current rebind permutation
   * @return a URL by which the resource can be loaded, <code>null</code> if one
   *         cannot be found
   */
  private static URL tryFindResource(ClassLoader classLoader,
      String resourceName, String locale) {
    URL toReturn = null;

    // Look for locale-specific variants of individual resources
    if (locale != null) {
      // Convert language_country_variant to independent pieces
      String[] localeSegments = locale.split("_");
      int lastDot = resourceName.lastIndexOf(".");
      String prefix = lastDot == -1 ? resourceName : resourceName.substring(0,
          lastDot);
      String extension = lastDot == -1 ? "" : resourceName.substring(lastDot);

      for (int i = localeSegments.length - 1; i >= -1; i--) {
        String localeInsert = "";
        for (int j = 0; j <= i; j++) {
          localeInsert += "_" + localeSegments[j];
        }

        toReturn = classLoader.getResource(prefix + localeInsert + extension);
        if (toReturn != null) {
          break;
        }
      }
    } else {
      toReturn = classLoader.getResource(resourceName);
    }

    return toReturn;
  }

  /**
   * Utility class.
   */
  private ResourceGeneratorUtil() {
  }
}
