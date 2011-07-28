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

package com.google.gwt.gen2.table.event.client;

import com.google.gwt.gen2.event.shared.AbstractEvent;

/**
 * Logical event fired when a page has finished loaded.
 */
public class PageLoadEvent extends AbstractEvent {
  /**
   * Event Key for {@link PageLoadEvent}.
   */
  public static final Type<PageLoadEvent, PageLoadHandler> TYPE = new Type<PageLoadEvent, PageLoadHandler>() {
    @Override
    protected void fire(PageLoadHandler handler, PageLoadEvent event) {
      handler.onPageLoad(event);
    }
  };

  /**
   * The page that was loaded.
   */
  private int page;

  /**
   * Construct a new {@link PageLoadEvent}.
   * 
   * @param page the page that was loaded
   */
  public PageLoadEvent(int page) {
    this.page = page;
  }

  /**
   * @return the page that has finished loading
   */
  public int getPage() {
    return page;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
