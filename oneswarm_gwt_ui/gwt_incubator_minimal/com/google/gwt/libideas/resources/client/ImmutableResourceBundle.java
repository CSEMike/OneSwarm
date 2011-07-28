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
package com.google.gwt.libideas.resources.client;

import com.google.gwt.libideas.resources.ext.ResourceGeneratorType;
import com.google.gwt.libideas.resources.rg.BundleResourceGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The use of this interface is similar to that of ImageBundle. Declare
 * no-argument functions that return subclasses of {@link ResourcePrototype},
 * which are annotated with {@link ImmutableResourceBundle.Resource} annotations
 * specifying the classpath location of the resource to include in the output.
 * At runtime, the functions will return an object that can be used to access
 * the data in the original resource.
 * 
 * @deprecated Superseded by
 *             {@link com.google.gwt.resources.client.ClientBundle}.
 */
@Deprecated
@ResourceGeneratorType(BundleResourceGenerator.class)
public interface ImmutableResourceBundle {
  // TODO(bobv): Rename to ClientBundle

  /**
   * Specifies the classpath location of the resource or resources associated
   * with the {@link ResourcePrototype}.
   * 
   * @deprecated Superseded by
   *             {@link com.google.gwt.resources.client.ClientBundle.Source}.
   */
  @Deprecated
  @Target(ElementType.METHOD)
  public @interface Resource {
    // TODO(bobv): Rename to @Source
    String[] value();
  }

  /**
   * Specifies a sequence of transformation to be applied to the resource. The
   * transformation may be a well-known short name or a fully-qualified class
   * name.
   * 
   * @deprecated with no replacement
   */
  @Deprecated
  @Target(ElementType.METHOD)
  public @interface Transform {
    String[] value();
  }

  /**
   * Find a resource by the name of the function in which it is declared.
   * 
   * @param name the name of the desired resource
   * @return the resource, or <code>null</code> if no such resource is defined.
   * @deprecated Superseded by
   *             {@link com.google.gwt.resources.client.ClientBundleWithLookup#getResource(String)}
   *             .
   */
  @Deprecated
  // TODO(bobv): Move this to ClientBundleWithLookup
  ResourcePrototype getResource(String name);

  /**
   * A convenience method to iterate over all ResourcePrototypes contained in
   * the ResourceBundle.
   * 
   * @deprecated Superseded by
   *             {@link com.google.gwt.resources.client.ClientBundleWithLookup#getResources}
   *             .
   */
  // TODO(bobv): Move this to ClientBundleWithLookup
  @Deprecated
  ResourcePrototype[] getResources();
}
