/*
 * File    : LoggerChannelImpl.java
 * Created : 28-Dec-2003
 * By      : parg
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

package org.gudy.azureus2.pluginsimpl.local.logging;

import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogRelation;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

public class 
LoggerChannelImpl 
	implements LoggerChannel
{
	private static final LogIDs LOGID = org.gudy.azureus2.core3.logging.LogIDs.PLUGIN;
	final private Logger		logger;
	final private String		name;
	final private boolean		timestamp;
	final boolean		no_output;
	final List		listeners = new ArrayList();
	
	private AEDiagnosticsLogger	diagnostic_logger;
	
	protected
	LoggerChannelImpl(
		Logger		_logger,
		String		_name,
		boolean		_timestamp,
		boolean		_no_output )
	{
		logger		= _logger;
		name		= _name;
		timestamp	= _timestamp;
		no_output	= _no_output;
	}
		
	public Logger
	getLogger()
	{
		return( logger );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public boolean
	isEnabled()
	{
		return org.gudy.azureus2.core3.logging.Logger.isEnabled();
	}

	public void
	setDiagnostic()
	{
		setDiagnostic( 0, true );
	}
	
	public void 
	setForce(
		boolean forceToFile) 
	{
		diagnostic_logger.setForced( forceToFile );
	}
	
	public boolean
	getForce()
	{
		return( diagnostic_logger.isForced());
	}
	
	public void
	setDiagnostic(
		long	max_file_size,
		boolean	diag_timestamp )
	{
		if ( diagnostic_logger == null ){
			
			diagnostic_logger = AEDiagnostics.getLogger( FileUtil.convertOSSpecificChars( name, false ));
			
			if ( max_file_size > 0 ){
				
				diagnostic_logger.setMaxFileSize((int)max_file_size );
			}
						
			diagnostic_logger.enableTimeStamp( !timestamp && diag_timestamp );
			
			addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	content )
					{
						diagnostic_logger.log( content );
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						diagnostic_logger.log( str );
						diagnostic_logger.log( error );
					}
				});
		}
	}
	
	private int LogTypePluginToCore(int pluginLogType) {
    switch (pluginLogType) {
      case LT_INFORMATION:
        return LogEvent.LT_INFORMATION;
      case LT_WARNING:
        return LogEvent.LT_WARNING;
      case LT_ERROR:
        return LogEvent.LT_ERROR;
    }
    return LogEvent.LT_INFORMATION;
  }
	
	private void notifyListeners(int log_type, String data) {
		for (int i = 0; i < listeners.size(); i++) {
			try {
				LoggerChannelListener l = (LoggerChannelListener) listeners.get(i);
				l.messageLogged(log_type, data);
			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}
	
	private void notifyListeners(String listenersText, Throwable error) {
		for (int i = 0; i < listeners.size(); i++) {
			try {
				LoggerChannelListener l = (LoggerChannelListener) listeners.get(i);
				l.messageLogged(listenersText, error);
			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void log(int log_type, String data) {
		notifyListeners(log_type, addTimeStamp(data));

		if (isEnabled() && !no_output) {
			data = "[" + name + "] " + data;

			org.gudy.azureus2.core3.logging.Logger.log(new LogEvent(LOGID,
					LogTypePluginToCore(log_type), data));
		}
	}
	
	public void
	log(
		String	data )
	{
		log( LT_INFORMATION, data );
	}
	
	public void log(Object[] relatedTo, int log_type, String data) {

		String listenerData;
		if (relatedTo != null) {
			StringBuffer text = new StringBuffer();
			for (int i = 0; i < relatedTo.length; i++) {
				Object obj = relatedTo[i];

				if (obj == null)
					continue;

				if (i > 0)
					text.append("; ");

				if (obj instanceof LogRelation) {
					text.append(((LogRelation) obj).getRelationText());
				} else {
					text.append("RelatedTo[")
					    .append(obj.toString())
					    .append("]");
				}
			}
			
			listenerData = text.toString() + "] " + data;
		}else{
			listenerData = data;
		}
		
		notifyListeners(log_type, addTimeStamp(listenerData));
		
		if (isEnabled() && !no_output) {
			data = "[" + name + "] " + data;
			org.gudy.azureus2.core3.logging.Logger.log(new LogEvent(relatedTo, LOGID,
					LogTypePluginToCore(log_type), data));
		}
	}

	public void log(Object relatedTo, int log_type, String data) {
		log(new Object[] { relatedTo }, log_type, data);
	}
	
	public void log(Throwable error)
  {
    log("", error);
  }

  public void log(String str, Throwable error) {
		notifyListeners(str.equals("") ? "" : addTimeStamp(str), error);

		if (!no_output) {
			LogEvent event = new LogEvent(LOGID, "[" + name + "] " + str, error);
			org.gudy.azureus2.core3.logging.Logger.log(event);
		}
	}

	public void log(Object[] relatedTo, String str, Throwable error) {
		notifyListeners(str.equals("") ? "" : addTimeStamp(str), error);

		if (isEnabled() && !no_output) {
			str = "[" + name + "] " + str;
			
			org.gudy.azureus2.core3.logging.Logger.log(new LogEvent(relatedTo, LOGID,
					str, error));
		}
	}

	public void log(Object relatedTo, String str, Throwable error) {
		log(new Object[] { relatedTo }, str, error);
	}
	
	public void log(Object[] relatedTo, String data) {
		log(relatedTo, LT_INFORMATION, data);
	}

	public void log(Object relatedTo, String data) {
		log(relatedTo, LT_INFORMATION, data);
	}


	// Alert Functions
	// ===============
  
	protected void logAlert(int alert_type, String message, boolean repeatable) {
		// output as log message to any listeners
		for (int i = 0; i < listeners.size(); i++) {
			try {
				((LoggerChannelListener) listeners.get(i)).messageLogged(alert_type,
						addTimeStamp(message));
			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}

		if (!no_output) {
			int at;

			switch (alert_type) {
				case LoggerChannel.LT_INFORMATION: {
					at = LogAlert.AT_INFORMATION;
					break;
				}
				case LoggerChannel.LT_WARNING: {
					at = LogAlert.AT_WARNING;
					break;
				}
				default: {
					at = LogAlert.AT_ERROR;
					break;
				}
			}

			org.gudy.azureus2.core3.logging.Logger.log(new LogAlert(repeatable, at,
					message));
		}
	}
	
	public void
	logAlert(
		int			alert_type,
		String		message )
	{
		logAlert( alert_type, message, false );
	}
	
	public void
	logAlertRepeatable(
		int			alert_type,
		String		message )
	{
		logAlert( alert_type, message, true );
	}
	
	public void
	logAlert(
		String		message,
		Throwable 	e )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((LoggerChannelListener)listeners.get(i)).messageLogged( addTimeStamp( message ), e );
	
			}catch( Throwable f ){
				
				Debug.printStackTrace( f );
			}
		}	

		if ( !no_output ){
			org.gudy.azureus2.core3.logging.Logger.log(new LogAlert(
					LogAlert.UNREPEATABLE, message, e));
		}
	}	
	
	public void
	logAlertRepeatable(
		String		message,
		Throwable 	e )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((LoggerChannelListener)listeners.get(i)).messageLogged( addTimeStamp( message ), e );
	
			}catch( Throwable f ){
				
				Debug.printStackTrace( f );
			}
		}	
		
		if ( !no_output ){
			org.gudy.azureus2.core3.logging.Logger.log(new LogAlert(
					LogAlert.REPEATABLE, message, e));
		}
	}
	
	// Misc. Functions
	// ===============
	
	public void
	addListener(
		LoggerChannelListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		LoggerChannelListener	l )
	{
		listeners.remove(l);
	}
	
	protected String
	addTimeStamp(
		String	data )
	{
		if ( timestamp  ){
			
			return( getTimeStamp() + data );
			
		}else{
			
			return( data );
		}
	}
	
	protected String
	getTimeStamp()
	{
		Calendar now = GregorianCalendar.getInstance();
    
		String timeStamp =
			"[" + now.get(Calendar.HOUR_OF_DAY)+ ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "] ";        

		return( timeStamp );
	}
	
	private static String 
	format(
		int 	n ) 
	{
		if (n < 10){
	   	
			return( "0" + n );
	   }
		
	   return( String.valueOf(n));
	}
}
