// CHECKSTYLE_OFF
/*
 * Copyright 2008 Fred Sauer
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
 * 
 * Modified by Google
 */
package com.google.gwt.libideas.logging.client.impl;

/**
 * {@link com.allen_sauer.gwt.log.client.util.DOMUtil} implementation for
 * standard browsers.
 */
public class DOMUtilImplStandard extends DOMUtilImpl {
  // CHECKSTYLE_JAVADOC_OFF

  public String adjustTitleLineBreaks(String message) {
    return message.replaceAll("\r\n|\r|\n", " / ");
  }
}
