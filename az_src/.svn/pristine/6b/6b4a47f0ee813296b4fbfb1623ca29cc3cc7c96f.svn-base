/*
 * File    : LocaleUtilitiesImpl.java
 * Created : 30-Mar-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.util.Debug;

import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.*;


public class 
LocaleUtilitiesImpl
	implements LocaleUtilities
{
	private PluginInterface	pi;
	
	private List			listeners;
	
	public
	LocaleUtilitiesImpl(
		PluginInterface		_pi )
	{
		pi	 = _pi;
	}
	
	public void
	integrateLocalisedMessageBundle(
		String		resource_bundle_prefix )
	{
		MessageText.integratePluginMessages(resource_bundle_prefix,pi.getPluginClassLoader());
	}
	
	public void integrateLocalisedMessageBundle(ResourceBundle rb) {
		MessageText.integratePluginMessages(rb);
	}
	
	public void integrateLocalisedMessageBundle(Properties p) {
		// Surely there's a more convenient way of doing this?
		ResourceBundle rb = null;
		try {
			PipedInputStream in_stream = new PipedInputStream();
			PipedOutputStream out_stream = new PipedOutputStream(in_stream);
			p.store(out_stream, "");
			out_stream.close();
			rb = new PropertyResourceBundle(in_stream);
			in_stream.close();
		}
		catch (IOException ioe) {return;}
		integrateLocalisedMessageBundle(rb);
	}
	
	public String
	getLocalisedMessageText(
		String		key )
	{
		return( MessageText.getString( key ));
	}
	
	public String
	getLocalisedMessageText(
		String		key,
		String[]	params )
	{
		return( MessageText.getString( key, params ));
	}
	
	public boolean hasLocalisedMessageText(String key) {
		return MessageText.keyExists(key);
	}
	
	public String localise(String key) {
		String res = MessageText.getString(key);
		if (res.charAt(0) == '!' && !MessageText.keyExists(key)) {
			return null;
		}
		return res;
	}
	
	public Locale getCurrentLocale() {
		return MessageText.getCurrentLocale();
	}
	
	public LocaleDecoder[]
	getDecoders()
	{
		LocaleUtilDecoder[]	decs = LocaleUtil.getSingleton().getDecoders();
		
		LocaleDecoder[]	res = new LocaleDecoder[decs.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = new LocaleDecoderImpl( decs[i] );
		}
		
		return( res );
	}
	
	public void
	addListener(
		LocaleListener		l )
	{
		if ( listeners == null ){
			
			listeners	= new ArrayList();
			
			COConfigurationManager.addParameterListener(
				"locale.set.complete.count", 
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String parameterName )
					{
						for (int i=0;i<listeners.size();i++){
							
							try{
								((LocaleListener)listeners.get(i)).localeChanged( MessageText.getCurrentLocale());
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
						}
					}
				});
		}
		
		listeners.add( l );
	}
	
	public void
	removeListener(
		LocaleListener		l )
	{
		if ( listeners == null ){
			
			return;
		}
		
		listeners.remove(l);
	}
}
