/*
 * File    : BlockedIpImpl.java
 * Created : 12 déc. 2003}
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.core3.ipfilter.impl;

import org.gudy.azureus2.core3.ipfilter.BlockedIp;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.util.SystemTime;

/**
 * @author Olivier
 *
 */
public class BlockedIpImpl implements BlockedIp {

  private String ip;
  private long time;
  private IpRange range;
  private String torrentname;
  private boolean	loggable;
  
  public BlockedIpImpl(String ip,IpRange range, String torrent_name,boolean _loggable) {
    this.ip = ip;
    this.range = range;
    this.time = SystemTime.getCurrentTime();
    this.torrentname = torrent_name;
    loggable = _loggable;
  }
  
  public String getBlockedIp() {
    return this.ip;
  }

  public IpRange getBlockingRange() {
    return this.range;
  }
  
  public long getBlockedTime() {
    return time;
  }
  
  public String getTorrentName() {
    return torrentname;
  }

  public boolean
  isLoggable()
  {
	  return( loggable );
  }
}
