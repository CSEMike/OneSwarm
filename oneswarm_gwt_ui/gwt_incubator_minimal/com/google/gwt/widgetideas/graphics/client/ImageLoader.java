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
package com.google.gwt.widgetideas.graphics.client;

import com.google.gwt.dom.client.ImageElement;

import java.util.ArrayList;

/**
 * Provides a mechanism for deferred execution of a callback 
 * method once all specified Images are loaded.
 */
public class ImageLoader {

  /**
   * Interface to allow anonymous instantiation of a CallBack
   * Object with method that gets invoked when all the images
   * are loaded.
   */
  public interface CallBack {
    void onImagesLoaded(ImageElement[] imageElements);
  }
  
  /**
   * Static internal collection of ImageLoader instances.
   * ImageLoader is not instantiable externally.
   */
  private static ArrayList<ImageLoader> imageLoaders = new ArrayList<ImageLoader>();
  
  /**
   * Takes in an array of url Strings corresponding to the images needed to
   * be loaded. The onImagesLoaded() method in the specified CallBack
   * object is invoked with an array of ImageElements corresponding to
   * the original input array of url Strings once all the images report
   * an onload event.
   * 
   * @param urls Array of urls for the images that need to be loaded
   * @param cb CallBack object
   */
  public static void loadImages(String[] urls, CallBack cb) {
    ImageLoader il = new ImageLoader();
    for (int i = 0;i < urls.length;i++) {
      il.addHandle(il.prepareImage(urls[i]));
    }
    il.finalize(cb);
    ImageLoader.imageLoaders.add(il);
    // Go ahead and fetch the images now
    for (int i = 0; i < urls.length; i++) {
      il.images.get(i).setSrc(urls[i]);
    }
  }
  
  private CallBack callBack = null;
  private ArrayList<ImageElement> images = new ArrayList<ImageElement>();
  private int loadedImages = 0;
  private int totalImages = 0;
  
  private ImageLoader() {
  }
  
  /**
   * Stores the ImageElement reference so that when all the images report
   * an onload, we can return the array of all the ImageElements.
   * @param img
   */
  private void addHandle(ImageElement img) {
    this.totalImages++;
    this.images.add(img);
  }
  
  /**
   * Invokes the onImagesLoaded method in the CallBack if all the
   * images are loaded AND we have a CallBack specified.
   * 
   * Called from the JSNI onload event handler.
   */
  @SuppressWarnings("unused")
  private void dispatchIfComplete() {
    if (callBack != null && isAllLoaded()) {
      callBack.onImagesLoaded((ImageElement[]) images.toArray(new ImageElement[0]));
      // remove the image loader
      ImageLoader.imageLoaders.remove(this);
    }
  }
  
  /**
   * Sets the callback object for the ImageLoader.
   * Once this is set, we may invoke the callback once all images that
   * need to be loaded report in from their onload event handlers.
   * 
   * @param cb
   */
  private void finalize(CallBack cb) {
    this.callBack = cb;
  }
  
  @SuppressWarnings("unused")
  private void incrementLoadedImages() {
    this.loadedImages++;
  }
  
  private boolean isAllLoaded() {
    return (loadedImages == totalImages);
  }
  
  /**
   * Returns a handle to an img object. Ties back to the ImageLoader instance
   */
  private native ImageElement prepareImage(String url)/*-{
    // if( callback specified )
    // do nothing
     
    var img = new Image();
    var __this = this;
     
    img.onload = function() {
      if(!img.__isLoaded) {
       
        // __isLoaded should be set for the first time here.
        // if for some reason img fires a second onload event
        // we do not want to execute the following again (hence the guard)
        img.__isLoaded = true;       
        __this.@com.google.gwt.widgetideas.graphics.client.ImageLoader::incrementLoadedImages()();
        img.onload = null;
        
        // we call this function each time onload fires
        // It will see if we are ready to invoke the callback
        __this.@com.google.gwt.widgetideas.graphics.client.ImageLoader::dispatchIfComplete()();   
      } else {
        // we invoke the callback since we are already loaded
        __this.@com.google.gwt.widgetideas.graphics.client.ImageLoader::dispatchIfComplete()();   
      }
    }
    
    return img;
  }-*/;
}
