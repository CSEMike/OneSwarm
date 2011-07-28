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
package com.google.gwt.libideas.resources.rebind.context;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.libideas.resources.client.ImmutableResourceBundle;
import com.google.gwt.libideas.resources.client.ResourcePrototype;
import com.google.gwt.libideas.resources.ext.ResourceBundleFields;
import com.google.gwt.libideas.resources.ext.ResourceBundleRequirements;
import com.google.gwt.libideas.resources.ext.ResourceContext;
import com.google.gwt.libideas.resources.ext.ResourceGenerator;
import com.google.gwt.libideas.resources.ext.ResourceGeneratorType;
import com.google.gwt.libideas.resources.rg.BundleResourceGenerator;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The base class for creating new ResourceBundle implementations.
 */
public abstract class AbstractResourceBundleGenerator extends Generator {

  /**
   * An implementation of FieldAccumulator that immediately composes its output
   * into a StringWriter.
   */
  private static class FieldsImpl implements ResourceBundleFields {
    private final NameFactory factory = new NameFactory();
    private final StringWriter buffer = new StringWriter();
    private final PrintWriter pw = new PrintWriter(buffer);

    public void addName(String name) {
      factory.addName(name);
    }

    public String define(JType type, String name) {
      return define(type, name, null, true, false);
    }

    public String define(JType type, String name, String initializer,
        boolean isStatic, boolean isFinal) {

      assert Util.isValidJavaIdent(name) : name
          + " is not a valid Java identifier";

      String ident = factory.createName(name);

      pw.print("private ");

      if (isStatic) {
        pw.print("static ");
      }

      if (isFinal) {
        pw.print("final ");
      }

      pw.print(type.getQualifiedSourceName());
      pw.print(" ");
      pw.print(ident);

      if (initializer != null) {
        pw.print(" = ");
        pw.print(initializer);
      }

      pw.println(";");

      return ident;
    }

    public String toString() {
      pw.flush();
      return buffer.getBuffer().toString();
    }
  }

  private static class RequirementsImpl implements ResourceBundleRequirements {
    private final Set<String> axes = new HashSet<String>();
    private final PropertyOracle oracle;

    public RequirementsImpl(PropertyOracle oracle) {
      this.oracle = oracle;
    }

    public void addPermutationAxis(String propertyName)
        throws BadPropertyValueException {
      oracle.getPropertyValue(TreeLogger.NULL, propertyName);
      axes.add(propertyName);
    }
  }

  public final String generate(TreeLogger logger,
      GeneratorContext generatorContext, String typeName)
      throws UnableToCompleteException {

    // The TypeOracle knows about all types in the type system
    TypeOracle typeOracle = generatorContext.getTypeOracle();

    // Get a reference to the type that the generator should implement
    JClassType sourceType = typeOracle.findType(typeName);

    // Ensure that the requested type exists
    if (sourceType == null) {
      logger.log(TreeLogger.ERROR, "Could not find requested typeName");
      throw new UnableToCompleteException();
    } else if (sourceType.isInterface() == null) {
      // The incoming type wasn't a plain interface, we don't support
      // abstract base classes
      logger.log(TreeLogger.ERROR, sourceType.getQualifiedSourceName()
          + " is not an interface.", null);
      throw new UnableToCompleteException();
    }

    /*
     * This associates the methods to implement with the ResourceGenerator class
     * that will generate the implementations of those methods.
     */
    Map<Class<? extends ResourceGenerator>, List<JMethod>> taskList = createTaskList(
        logger, typeOracle, sourceType);

    /*
     * Additional objects that hold state during the generation process.
     */
    AbstractResourceContext resourceContext = createResourceContext(logger,
        generatorContext, sourceType);
    FieldsImpl fields = new FieldsImpl();
    RequirementsImpl requirements = new RequirementsImpl(
        generatorContext.getPropertyOracle());

    /*
     * Initialize the ResourceGenerators and prepare them for subsequent code
     * generation.
     */
    Map<ResourceGenerator, List<JMethod>> generators = initAndPrepare(logger,
        taskList, resourceContext, requirements);

    /*
     * Now that the ResourceGenerators have been initialized and prepared, we
     * can compute the actual name of the implementation class in order to
     * ensure that we use a distinct name between permutations.
     */
    String generatedSimpleSourceName = generateSimpleSourceName(logger,
        resourceContext, requirements);

    // Begin writing the generated source.
    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        sourceType.getPackage().getName(), generatedSimpleSourceName);

    // The generated class needs to be able to determine the module base URL
    f.addImport(GWT.class.getName());

    // Used by the map methods
    f.addImport(ResourcePrototype.class.getName());

    // The whole point of this exercise
    f.addImplementedInterface(sourceType.getQualifiedSourceName());

    String createdClassName = f.getCreatedClassName();

    // All source gets written through this Writer
    PrintWriter out = generatorContext.tryCreate(logger,
        sourceType.getPackage().getName(), generatedSimpleSourceName);

    // If an implementation already exists, we don't need to do any work
    if (out != null) {
      SourceWriter sw = f.createSourceWriter(generatorContext, out);

      // Set the now-calculated simple source name
      resourceContext.setSimpleSourceName(generatedSimpleSourceName);

      // Write the generated code to disk
      createFieldsAndAssignments(logger, sw, generators, resourceContext,
          fields);

      // Print the accumulated field definitions
      sw.println(fields.toString());

      /*
       * The map-accessor methods use JSNI and need a fully-qualified class
       * name, but should not include any sub-bundles.
       */
      taskList.remove(BundleResourceGenerator.class);
      writeMapMethods(logger, createdClassName, sw, taskList);

      sw.commit(logger);
    }

    finish(logger, resourceContext, generators.keySet());

    // Return the name of the concrete class
    return createdClassName;
  }

  /**
   * Create the ResourceContext object that will be used by
   * {@link ResourceGenerator} subclasses. This is the primary way to implement
   * custom logic in the resource generation pass.
   */
  protected abstract AbstractResourceContext createResourceContext(
      TreeLogger logger, GeneratorContext context, JClassType resourceBundleType);

  /**
   * Create fields and assignments for a single ResourceGenerator.
   */
  private boolean createFieldsAndAssignments(TreeLogger logger,
      ResourceContext resourceContext, ResourceGenerator rg,
      List<JMethod> generatorMethods, SourceWriter sw,
      ResourceBundleFields fields) {

    // Defer failure until this phase has ended
    boolean fail = false;

    // Write all field values
    try {
      rg.createFields(logger.branch(TreeLogger.DEBUG, "Creating fields"),
          resourceContext, fields);
    } catch (UnableToCompleteException e) {
      return false;
    }

    // Create the instance variables in the IRB subclass by calling
    // writeAssignment() on the ResourceGenerator
    for (JMethod m : generatorMethods) {
      String rhs;

      try {
        rhs = rg.createAssignment(logger.branch(TreeLogger.DEBUG,
            "Creating assignment for " + m.getName()), resourceContext, m);
      } catch (UnableToCompleteException e) {
        fail = true;
        continue;
      }

      String ident = fields.define(m.getReturnType().isClassOrInterface(),
          m.getName(), null, true, false);

      // Strip off all but the access modifiers
      sw.print(m.getReadableDeclaration(false, true, true, true, true));
      sw.println(" {");
      sw.indent();
      sw.println("if (" + ident + " == null) {");
      sw.indent();
      sw.println(ident + " = " + rhs + ";");
      sw.outdent();
      sw.println("}");
      sw.println("return " + ident + ";");
      sw.outdent();
      sw.println("}");
    }

    if (fail) {
      return false;
    }

    return true;
  }

  /**
   * Create fields and assignments for multiple ResourceGenerators.
   */
  private void createFieldsAndAssignments(TreeLogger logger, SourceWriter sw,
      Map<ResourceGenerator, List<JMethod>> generators,
      ResourceContext resourceContext, ResourceBundleFields fields)
      throws UnableToCompleteException {
    // Try to provide as many errors as possible before failing.
    boolean success = true;

    // Run the ResourceGenerators to generate implementations of the methods
    for (Map.Entry<ResourceGenerator, List<JMethod>> entry : generators.entrySet()) {

      success &= createFieldsAndAssignments(logger, resourceContext,
          entry.getKey(), entry.getValue(), sw, fields);
    }

    if (!success) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Given an ImmutableResourceBundle subtype, compute which ResourceGenerators
   * should implement which methods.
   */
  private Map<Class<? extends ResourceGenerator>, List<JMethod>> createTaskList(
      TreeLogger logger, TypeOracle typeOracle, JClassType sourceType)
      throws UnableToCompleteException {

    Map<Class<? extends ResourceGenerator>, List<JMethod>> toReturn = new HashMap<Class<? extends ResourceGenerator>, List<JMethod>>();

    JClassType bundleType = typeOracle.findType(ImmutableResourceBundle.class.getName());
    assert bundleType != null;

    JClassType resourcePrototypeType = typeOracle.findType(ResourcePrototype.class.getName());
    assert resourcePrototypeType != null;

    // Accumulate as many errors as possible before failing
    boolean throwException = false;

    // Using overridable methods allows composition of interface types
    for (JMethod m : sourceType.getOverridableMethods()) {
      JClassType returnType = m.getReturnType().isClassOrInterface();

      if (m.getEnclosingType().equals(bundleType)) {
        // Methods that we must generate, but that are not resources
        continue;

      } else if (!m.isAbstract()) {
        // Covers the case of an abstract class base type
        continue;

      } else if (returnType == null
          || !(returnType.isAssignableTo(resourcePrototypeType) || returnType.isAssignableTo(bundleType))) {
        // Primitives and random other abstract methods
        logger.log(TreeLogger.ERROR, "Unable to implement " + m.getName()
            + " because it does not derive from "
            + resourcePrototypeType.getQualifiedSourceName() + " or "
            + bundleType.getQualifiedSourceName());
        throwException = true;
        continue;
      }

      try {
        Class<? extends ResourceGenerator> clazz = findResourceGenerator(
            logger, typeOracle, m);
        List<JMethod> generatorMethods;
        if (toReturn.containsKey(clazz)) {
          generatorMethods = toReturn.get(clazz);
        } else {
          generatorMethods = new ArrayList<JMethod>();
          toReturn.put(clazz, generatorMethods);
        }

        generatorMethods.add(m);
      } catch (UnableToCompleteException e) {
        throwException = true;
      }
    }

    if (throwException) {
      throw new UnableToCompleteException();
    }

    return toReturn;
  }

  /**
   * Given a JMethod, find the a ResourceGenerator class that will be able to
   * provide an implementation of the method.
   */
  private Class<? extends ResourceGenerator> findResourceGenerator(
      TreeLogger logger, TypeOracle typeOracle, JMethod method)
      throws UnableToCompleteException {

    JClassType resourceType = method.getReturnType().isClassOrInterface();
    assert resourceType != null;

    List<JClassType> searchTypes = new ArrayList<JClassType>();
    searchTypes.add(resourceType);

    ResourceGeneratorType generatorType = null;

    while (!searchTypes.isEmpty()) {
      JClassType current = searchTypes.remove(0);
      generatorType = current.getAnnotation(ResourceGeneratorType.class);
      if (generatorType != null) {
        break;
      }

      if (current.getSuperclass() != null) {
        searchTypes.add(current.getSuperclass());
      }

      for (JClassType t : current.getImplementedInterfaces()) {
        searchTypes.add(t);
      }
    }

    if (generatorType == null) {
      logger.log(TreeLogger.ERROR, "No @"
          + ResourceGeneratorType.class.getName()
          + " was specifed for resource type "
          + resourceType.getQualifiedSourceName() + " or its supertypes");
      throw new UnableToCompleteException();
    }

    return generatorType.value();
  }

  /**
   * Call finish() on several ResourceGenerators.
   */
  private void finish(TreeLogger logger, ResourceContext context,
      Collection<ResourceGenerator> generators)
      throws UnableToCompleteException {
    boolean fail = false;
    // Finalize the ResourceGenerator
    for (ResourceGenerator rg : generators) {
      try {
        rg.finish(
            logger.branch(TreeLogger.DEBUG, "Finishing ResourceGenerator"),
            context);
      } catch (UnableToCompleteException e) {
        fail = true;
      }
    }
    if (fail) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Given a user-defined type name, determine the type name for the generated
   * class based on accumulated requirements.
   */
  private String generateSimpleSourceName(TreeLogger logger,
      ResourceContext context, RequirementsImpl requirements)
      throws UnableToCompleteException {
    StringBuilder toReturn = new StringBuilder(
        context.getResourceBundleType().getQualifiedSourceName().replaceAll(
            "[.$]", "_"));
    Set<String> permutationAxes = new HashSet<String>(requirements.axes);
    permutationAxes.add("locale");

    try {
      PropertyOracle oracle = context.getGeneratorContext().getPropertyOracle();
      for (String property : permutationAxes) {
        String value = oracle.getPropertyValue(logger, property);
        toReturn.append("_" + value);
      }
    } catch (BadPropertyValueException e) {
    }

    toReturn.append("_" + getClass().getSimpleName());

    return toReturn.toString();
  }

  private Map<ResourceGenerator, List<JMethod>> initAndPrepare(
      TreeLogger logger,
      Map<Class<? extends ResourceGenerator>, List<JMethod>> taskList,
      ResourceContext resourceContext, ResourceBundleRequirements requirements)
      throws UnableToCompleteException {
    // Try to provide as many errors as possible before failing.
    boolean success = true;
    Map<ResourceGenerator, List<JMethod>> toReturn = new IdentityHashMap<ResourceGenerator, List<JMethod>>();

    // Run the ResourceGenerators to generate implementations of the methods
    for (Map.Entry<Class<? extends ResourceGenerator>, List<JMethod>> entry : taskList.entrySet()) {

      ResourceGenerator rg = instantiateResourceGenerator(logger,
          entry.getKey());
      toReturn.put(rg, entry.getValue());

      success &= initAndPrepare(logger, resourceContext, rg, entry.getValue(),
          requirements);
    }

    if (!success) {
      throw new UnableToCompleteException();
    }

    return toReturn;
  }

  private boolean initAndPrepare(TreeLogger logger,
      ResourceContext resourceContext, ResourceGenerator rg,
      List<JMethod> generatorMethods, ResourceBundleRequirements requirements) {
    try {
      rg.init(
          logger.branch(TreeLogger.DEBUG, "Initializing ResourceGenerator"),
          resourceContext);
    } catch (UnableToCompleteException e) {
      return false;
    }

    boolean fail = false;

    // Prepare the ResourceGenerator by telling it all methods that it is
    // expected to produce.
    for (JMethod m : generatorMethods) {
      try {
        rg.prepare(logger.branch(TreeLogger.DEBUG, "Preparing method "
            + m.getName()), resourceContext, requirements, m);
      } catch (UnableToCompleteException e) {
        fail = true;
      }
    }

    return !fail;
  }

  /**
   * Utility method to construct a ResourceGenerator that logs errors.
   */
  private <T extends ResourceGenerator> T instantiateResourceGenerator(
      TreeLogger logger, Class<T> generatorClass)
      throws UnableToCompleteException {
    try {
      return generatorClass.newInstance();
    } catch (InstantiationException e) {
      logger.log(TreeLogger.ERROR, "Unable to initialize ResourceGenerator", e);
    } catch (IllegalAccessException e) {
      logger.log(TreeLogger.ERROR, "Unable to instantiate ResourceGenerator. "
          + "Does it have a public default constructor?", e);
    }
    throw new UnableToCompleteException();
  }

  private void writeMapMethods(TreeLogger logger, String createdClassName,
      SourceWriter sw,
      Map<Class<? extends ResourceGenerator>, List<JMethod>> taskList)
      throws UnableToCompleteException {

    // Complete the IRB contract
    sw.println("public ResourcePrototype[] getResources() {");
    sw.indent();
    sw.println("return new ResourcePrototype[] {");
    sw.indent();
    for (List<JMethod> methods : taskList.values()) {
      for (JMethod m : methods) {
        sw.println(m.getName() + "(), ");
      }
    }
    sw.outdent();
    sw.println("};");
    sw.outdent();
    sw.println("}");

    // Use a switch statement as a fast map
    sw.println("public native ResourcePrototype "
        + "getResource(String name) /*-{");
    sw.indent();
    sw.println("switch (name) {");
    sw.indent();
    for (List<JMethod> list : taskList.values()) {
      for (JMethod m : list) {
        sw.println("case '" + m.getName() + "': return this.@"
            + createdClassName + "::" + (m.getName() + "()()") + ";");
      }
    }
    sw.outdent();
    sw.println("}");
    sw.println("return null;");
    sw.outdent();
    sw.println("}-*/;");
  }
}
