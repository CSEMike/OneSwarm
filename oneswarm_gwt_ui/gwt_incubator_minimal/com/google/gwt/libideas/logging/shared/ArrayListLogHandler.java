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

package com.google.gwt.libideas.logging.shared;

import java.util.ArrayList;

/**
 * This handler is used primarily for testing purposes. It stores all messages
 * received in an {@link ArrayList}.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class ArrayListLogHandler extends LogHandler {
  private ArrayList messages = new ArrayList();
  private ArrayList categories = new ArrayList();
  private ArrayList throwables = new ArrayList();

  public String getLastCategory() {
    return (String) categories.get(categories.size() - 1);
  }

  /**
   * Gets the last message published.
   * 
   * @return the last message.
   */
  public String getLastMessage() {
    return (String) messages.get(messages.size() - 1);
  }

  /**
   * Gets last throwable
   */
  public Throwable getLastThrowable() {
    return (Throwable) throwables.get(throwables.size() - 1);
  }

  public void publish(String string, Level level, String category, Throwable t) {
    messages.add(string);
    categories.add(category);
    throwables.add(t);
  }

}
