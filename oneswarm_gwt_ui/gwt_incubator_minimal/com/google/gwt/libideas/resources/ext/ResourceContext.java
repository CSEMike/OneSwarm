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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

import java.net.URL;

/**
 * Context object for ResourceGenerators. An instance of this type will be
 * provided by the resource generation framework to implementations of
 * ResourceGenerator via {@link ResourceGenerator#init}. Because this interface
 * is not intended to be implemented by end-users, the API provided by this
 * interface may be extended in the future.
 * <p>
 * Depending on the optimizations made by the implementation of {@link #deploy},
 * the resulting URL may or may not be compatible with standard
 * {@link com.google.gwt.http.client.RequestBuilder} / XMLHttpRequest security
 * semantics. If the resource is intended to be used with XHR, the
 * <code>xhrCompatible</code> paramater should be set to <code>true</code> when
 * invoking {@link #deploy}.
 * </p>
 */
public interface ResourceContext {

  /**
   * Cause a specific collection of bytes to be available in the program's
   * compiled output. The return value of this method is a Java expression which
   * will evaluate to the location of the resource at runtime. The exact format
   * should not be depended upon.
   * 
   * @param suggestedFileName an unobfuscated filename to possibly use for the
   *          resource
   * @param mimeType the MIME type of the data being provided
   * @param data the bytes to add to the output
   * @param xhrCompatible enforces compatibility with security restrictions if
   *          the resource is intended to be accessed via an XMLHttpRequest.
   * @return a Java expression which will evaluate to the location of the
   *         provided resource at runtime.
   */
  String deploy(String suggestedFileName, String mimeType, byte[] data,
      boolean xhrCompatible) throws UnableToCompleteException;

  /**
   * Cause a specific collection of bytes to be available in the program's
   * compiled output. The return value of this method is a Java expression which
   * will evaluate to the location of the resource at runtime. The exact format
   * should not be depended upon.
   * 
   * @param resource the resource to add to the compiled output
   * @param xhrCompatible enforces compatibility with security restrictions if
   *          the resource is intended to be accessed via an XMLHttpRequest.
   * @return a Java expression which will evaluate to the location of the
   *         provided resource at runtime.
   */
  String deploy(URL resource, boolean xhrCompatible)
      throws UnableToCompleteException;

  /**
   * Return the GeneratorContext in which the overall resource generation
   * framework is being run. Implementations of ResourceGenerator should prefer
   * {@link #addToOutput} over {@link GeneratorContext#tryCreateResource} in
   * order to take advantage of serving optimizations that can be performed by
   * the bundle architecture.
   */
  GeneratorContext getGeneratorContext();

  /**
   * Returns the simple source name of the implementation of the bundle being
   * generated. This can be used during code-generation to refer to the instance
   * of the bundle (e.g. via <code>SimpleSourceName.this</code>).
   * 
   * @throws IllegalStateException if this method is called during
   *           {@link ResourceGenerator#init} or
   *           {@link ResourceGenerator#prepare} methods.
   */
  String getImplementationSimpleSourceName() throws IllegalStateException;

  /**
   * Return the interface type of the resource bundle being generated.
   */
  JClassType getResourceBundleType();

  /**
   * Indicates if the runtime context supports data: urls. When data URLs are
   * supported by the context, aggregation of resource data into larger payloads
   * is discouraged, as it offers reduced benefit to the application at runtime.
   */
  boolean supportsDataUrls();
}
