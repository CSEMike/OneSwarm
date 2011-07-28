/*
 * Copyright 2007 Google Inc.
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
 import flash.external.ExternalInterface;
 
//  haxe -swf SoundKernel.swf -main SoundKernel -swf-version 8 --flash-strict --no-traces
class SoundKernel {
  static var count : Int = 0;
  public static var sounds : IntHash<MySound> = new IntHash<MySound>();
  
  static function create(name : String) : MySound {
    trace("Going to create " + name);
    
    var handle = count++;
    var snd : MySound = new MySound(handle);
    snd.attachSound(name);
    sounds.set(handle, snd);
    
    trace("Created " + name + " with handle " + handle);
    return snd;
  }
  
  static function currentFrame() : Int {
    return flash.Lib.current._currentframe;
  }
  
  static function isComplete(handle : Int) : Bool {
    return !sounds.exists(handle);
  }
  
  static function main() {
    trace("main");
    if (flash.Lib._global.running != null) {
      trace("Duplicate detected");
      return;
    }
    flash.Lib._global.running = "Running";
    
    if (ExternalInterface.addCallback("GWTisComplete", null, isComplete)) {
      ExternalInterface.addCallback("GWTposition", null, position);
      ExternalInterface.addCallback("GWTsetPan", null, setPan);
      ExternalInterface.addCallback("GWTsetVolume", null, setVolume);
      ExternalInterface.addCallback("GWTstop", null, stop);
      ExternalInterface.addCallback("GWTcurrentFrame", null, currentFrame);
      ExternalInterface.addCallback("GWTplay", null, play);
      trace("Registered all callbacks");
    } else {
      trace("Failed to register callback");
    }
  }
  
  static function play(name : String, volume : Int, pan : Int) : Int {
    var snd : MySound = create(name);
    snd.setVolume(volume);
    snd.setPan(pan);
    snd.start();
    trace("Playing sound " + name);
    return snd.getHandle();
  }
  
  static function position(handle : Int) : Int {
    var snd : MySound = sounds.get(handle);
    if (snd == null) {
      return -1;
    }
    
    return Math.round(snd.position);
  }
  
  static function setPan(handle : Int, pan : Int) {
    var snd : MySound = sounds.get(handle);
    if (snd == null) {
      return;
    }
    
    snd.setPan(pan);
  }
  
  static function setVolume(handle : Int, volume : Int) {
    var snd : MySound = sounds.get(handle);
    if (snd == null) {
      return;
    }
    
    snd.setVolume(volume);
  }
  
  static function stop(handle : Int) {
    trace("Going to stop " + handle);
    if (sounds.exists(handle)) {
      sounds.get(handle).stop();
      sounds.remove(handle);
    }
    trace("Stopped");
  }
}


class MySound extends flash.Sound {
  var handle : Int;
  // Sounds must be attached to separate MovieClips to be independent
  var mc : flash.MovieClip;
    
  public function new(handle : Int) {
    mc = flash.Lib.current.createEmptyMovieClip("Sound " + handle, handle);
    super(mc);
    this.handle = handle;
  }
  
  public function getHandle() : Int {
    return handle;
  }
    
  public function onSoundComplete() {
    trace("Sound completed " + handle);
    SoundKernel.sounds.remove(handle);
    mc.removeMovieClip();
  }
}
