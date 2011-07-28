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
import com.google.gwt.libideas.resources.rg.DataResourceGenerator;

/**
 * A non-text resource.
 * 
 * @deprecated Superseded by
 *             {@link com.google.gwt.resources.client.DataResource}
 */
@Deprecated
@ResourceGeneratorType(DataResourceGenerator.class)
public interface DataResource extends ResourcePrototype {
  /**
   * Retrieves a URL by which the contents of the resource can be obtained. This
   * will be an absolute URL.
   */
  String getUrl();
}
