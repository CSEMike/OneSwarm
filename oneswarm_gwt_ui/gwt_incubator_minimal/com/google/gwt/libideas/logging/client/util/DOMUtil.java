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
 */

package com.google.gwt.libideas.logging.client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.libideas.logging.client.impl.DOMUtilImpl;

/**
 * Provides DOM utility methods.
 */
public class DOMUtil {
  private static DOMUtilImpl impl;

  static {
    impl = GWT.create(DOMUtilImpl.class);
  }

  /**
   * Adjust line breaks within in the provided title for optimal readability and
   * display length for the current user agent.
   * 
   * @param title the desired raw text
   * @return formatted and escaped text
   */
  public static String adjustTitleLineBreaks(String title) {
    return impl.adjustTitleLineBreaks(title);
  }
}
