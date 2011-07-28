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
package com.google.gwt.libideas.resources.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.resource.impl.PathPrefix;
import com.google.gwt.dev.resource.impl.PathPrefixSet;
import com.google.gwt.dev.resource.impl.ResourceFilter;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.libideas.resources.client.CssResource;
import com.google.gwt.libideas.resources.client.DataResource;
import com.google.gwt.libideas.resources.client.ExternalTextResource;
import com.google.gwt.libideas.resources.client.ImageResource;
import com.google.gwt.libideas.resources.client.ImmutableResourceBundle;
import com.google.gwt.libideas.resources.client.ResourcePrototype;
import com.google.gwt.libideas.resources.client.TextResource;
import com.google.gwt.util.tools.ArgHandlerDir;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

/**
 * Given a Java package, create an {@link ImmutableResourceBundle} interface.
 */
public class MakeBundle extends ToolBase {
  private static final List<String> JAVA_KEYWORDS = Arrays.asList("abstract",
      "continue", "for", " new", "switch", "assert", "default", "goto",
      "package", "synchronized", "boolean", "do", "if", " private", "this",
      "break", "double", "implements", "protected", "throw", "byte", "else",
      "import", "public", "throws", "case", "enum", "instanceof", "return",
      "transient", "catch", "extends", "int", "short", "try", "char", "final",
      "interface", "static", "void", "class", "finally", "long", "strictfp",
      "volatile", "const", "float", "native", "super", "while");

  private static final String[] IMAGE_TYPES = {"gif", "jpg", "png"};
  private static final String[] SOUND_TYPES = {"mp3", "wav"};
  private static final String[] TEXT_TYPES = {
      "htm", "html", "java", "json", "txt", "xhtml", "xml"};

  private static final Map<String, Class<? extends ResourcePrototype>> typeMap = new TreeMap<String, Class<? extends ResourcePrototype>>();

  static {
    typeMap.put("css", CssResource.class);

    for (String type : IMAGE_TYPES) {
      typeMap.put(type, ImageResource.class);
    }


    // This loop runs twice
    boolean external = false;
    do {
      external = !external;
      for (String type : TEXT_TYPES) {
        typeMap.put((external ? "e" : "") + type, external
            ? ExternalTextResource.class : TextResource.class);
      }
    } while (external);
  }

  /**
   * Entry point.
   */
  public static void main(String[] args) {
    PrintWriter pw = new PrintWriter(System.out);
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(pw);
    logger.setMaxDetail(TreeLogger.INFO);
    try {
      (new MakeBundle()).exec(logger, args);
    } finally {
      pw.close();
    }
  }

  private String bundleName;
  private final ArgHandlerString bundleNameHandler = new ArgHandlerString() {
    @Override
    public String getPurpose() {
      return "The bundle's class simple source name";
    }

    @Override
    public String getTag() {
      return "-name";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"FooBundle"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }

    @Override
    public boolean setString(String str) {
      bundleName = str;
      return true;
    }
  };

  private File outDir;
  private final ArgHandlerDir outDirHandler = new ArgHandlerDir() {

    @Override
    public String getPurpose() {
      return "The output directory";
    }

    @Override
    public String getTag() {
      return "-out";
    }

    @Override
    public void setDir(File dir) {
      outDir = dir;
    }
  };

  private boolean showTypes;
  private final ArgHandlerFlag showTypesHandler = new ArgHandlerFlag() {

    @Override
    public String getPurpose() {
      return "Show the file extension to resource type map";
    }

    @Override
    public String getTag() {
      return "-showTypes";
    }

    @Override
    public boolean setFlag() {
      return showTypes = true;
    }
  };

  private String packageName;
  private final ArgHandlerString packageNameHandler = new ArgHandlerString() {

    @Override
    public String getPurpose() {
      return "The binary name of the package to look for resources in";
    }

    @Override
    public String getTag() {
      return "-package";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"com.foo.client"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }

    @Override
    public boolean setString(String str) {
      packageName = str;
      return true;
    }
  };

  private final List<URL> files = new ArrayList<URL>();
  private final ArgHandlerExtra filesHandler = new ArgHandlerExtra() {

    @Override
    public boolean addExtraArg(String arg) {
      try {
        files.add((new File(arg)).toURL());
      } catch (IOException e) {
        return false;
      }
      return true;
    }

    @Override
    public String getPurpose() {
      return "Restrict the files included in the Bundle";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"file1.txt", "subpackage/file2.png"};
    }
  };

  /**
   * Utility class.
   */
  private MakeBundle() {
  }

  @Override
  protected String getDescription() {
    return "Generate an ImmutableResourceBundle definition.  Run this class "
        + "with the same classpath used to invoke the GWT compiler on your"
        + "module.";
  }

  /**
   * Instantiate the ResourceOracle used to find resources via the context class
   * loader.
   */
  private ResourceOracle createResourceOracle(TreeLogger logger) {
    logger = logger.branch(TreeLogger.DEBUG, "Creating ResourceOracle");
    ResourceOracleImpl oracle = new ResourceOracleImpl(logger);

    PathPrefixSet pps = new PathPrefixSet();
    pps.add(new PathPrefix(packageName.replace('.', '/') + '/',
    // Eliminate stuff that's definitely not source material
        new ResourceFilter() {
          public boolean allows(String path) {
            return !(path.endsWith(".class") || path.contains("/."));
          }
        }));
    oracle.setPathPrefixes(pps);
    ResourceOracleImpl.refresh(logger, oracle);
    return oracle;
  }

  /**
   * Open a PrintWriter to write into the output file.
   */
  private PrintWriter createWriter(TreeLogger logger) {
    File output = new File(outDir, packageName.replace(".", File.separator)
        + File.separator + bundleName);
    logger = logger.branch(TreeLogger.INFO, "Writing to " + output.getPath());
    output.getParentFile().mkdirs();
    PrintWriter out;
    try {
      out = new PrintWriter(new FileWriter(output));
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to create output file", e);
      return null;
    }

    return out;
  }

  private void exec(TreeLogger logger, String[] args) {
    registerHandler(showTypesHandler);
    registerHandler(packageNameHandler);
    registerHandler(bundleNameHandler);
    registerHandler(outDirHandler);
    registerHandler(filesHandler);

    if (!processArgs(args)) {
      return;
    }

    if (showTypes) {
      for (Map.Entry<String, Class<? extends ResourcePrototype>> entry : typeMap.entrySet()) {
        logger.log(TreeLogger.INFO, entry.getKey() + " : "
            + entry.getValue().getSimpleName());
      }
    }

    if (outDir == null) {
      try {
        outDir = new File(".").getCanonicalFile();
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR,
            "Unable to determine current working directory", e);
        return;
      }
    }

    ResourceOracle oracle = createResourceOracle(logger);
    PrintWriter out = createWriter(logger);

    // Sort the entries to provide consistent behavior
    TreeSet<Resource> sortedResources = new TreeSet<Resource>(
        new Comparator<Resource>() {
          public int compare(Resource o1, Resource o2) {
            return o1.getPath().compareTo(o2.getPath());
          }
        });
    sortedResources.addAll(oracle.getResources());

    writeMethods(logger, out, sortedResources);

    out.close();
  }

  /**
   * Determine the type of {@link ResourcePrototype} that will be used to
   * represent the resource.
   */
  private Class<? extends ResourcePrototype> getResourceType(String path) {
    int start = path.lastIndexOf('/');
    path = path.substring(start + 1);

    int end = path.lastIndexOf('.');
    if (end != -1) {
      path = path.substring(end + 1).toLowerCase();
      if (typeMap.containsKey(path)) {
        return typeMap.get(path);
      }
    }

    return DataResource.class;
  }

  /**
   * Create a Java identifier for a resource.
   */
  private String makeShortName(String path) {
    int start = path.lastIndexOf('/');
    path = path.substring(start + 1);

    int end = path.lastIndexOf('.');
    if (end != -1) {
      path = path.substring(0, end);
    }
    if (JAVA_KEYWORDS.contains(path)) {
      path += "_";
    }
    assert Util.isValidJavaIdent(path);
    return path;
  }

  /**
   * Write the contents of the Bundle.
   */
  private void writeMethods(TreeLogger logger, PrintWriter out,
      Set<Resource> resources) {
    logger = logger.branch(TreeLogger.DEBUG, "Writing contents");
    out.println("// AUTOMATICALLY GENERATED CLASS -- DO NOT EDIT");
    out.println("package " + packageName + ";");
    out.println("/*" + "* Compose this interface or use with GWT.create(). */");
    out.println("public interface " + makeShortName(bundleName) + " extends "
        + ImmutableResourceBundle.class.getCanonicalName() + " {");

    for (Resource s : resources) {
      // Check the resource against the restricted set of files
      if (!(files.isEmpty() || files.contains(s.getURL()))) {
        logger.log(TreeLogger.DEBUG, "Ignoring " + s.getLocation());
        continue;
      }

      logger.log(TreeLogger.DEBUG, s.getLocation() + "   " + s.getPath());
      String shortName = makeShortName(s.getPath());

      out.println("  @Resource(\"" + s.getPath() + "\")");

      Class<? extends ResourcePrototype> clazz = getResourceType(s.getPath());
      out.println("  " + clazz.getCanonicalName() + " " + shortName + "();\n");
    }

    out.println("}");
  }
}
