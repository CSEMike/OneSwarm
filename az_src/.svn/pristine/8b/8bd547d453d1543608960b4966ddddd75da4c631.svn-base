package com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;

public class LTHandshake implements LTMessage {
	
	private Map data_dict;
	private byte[] bencoded_data;
	private String bencoded_string;
	private String description;
    private byte version;
    private DirectByteBuffer[] buffer_array;
    
    public LTHandshake(Map data_dict, byte version) {
    	this.data_dict = (data_dict == null) ? Collections.EMPTY_MAP : data_dict;
    	this.version = version;
    }

	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		if (data == null) {
			throw new MessageException( "[" +getID() + "] decode error: data == null");
		}
		if (data.remaining(DirectByteBuffer.SS_MSG ) < 1) {
			throw new MessageException( "[" +getID() + "] decode error: less than 1 byte in payload");
		}
		
		// Try decoding the data now.
		Map res_data_dict = MessagingUtil.convertBencodedByteStreamToPayload(data, 1, getID());
		
		LTHandshake result = new LTHandshake(res_data_dict, this.version);
		return result;
	}

	public DirectByteBuffer[] getData() {
		if (buffer_array == null) {
			buffer_array = new DirectByteBuffer[1];
			DirectByteBuffer buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG_LT_HANDSHAKE, getBencodedData().length);
			buffer_array[0] = buffer;
			
			buffer.put(DirectByteBuffer.SS_MSG, getBencodedData());
			buffer.flip(DirectByteBuffer.SS_MSG);
		}
		return buffer_array;
	}

	public void destroy() {
		this.data_dict = null;
		this.bencoded_data = null;
		this.description = null;
		if (buffer_array != null) {
			buffer_array[0].returnToPool();
		}
		this.buffer_array = null;
	}

	public String getDescription() {
		if (description == null) {
			description = LTMessage.ID_LT_HANDSHAKE.toUpperCase() + ": " + this.getBencodedString();
		}
		return description;
	}
	
	public String getBencodedString() {
		if (this.bencoded_string == null) {
			try {
				this.bencoded_string = new String(this.getBencodedData(), Constants.BYTE_ENCODING);
			}
			catch (java.io.UnsupportedEncodingException uee) {
				this.bencoded_string = "";
				Debug.printStackTrace(uee);
			}
		}
		return this.bencoded_string;
	}
	
	public byte[] getBencodedData() {
		if (this.bencoded_data == null) {
			try {this.bencoded_data = BEncoder.encode(this.data_dict);}
			catch (java.io.IOException ioe) {
				this.bencoded_data = new byte[0];
				Debug.printStackTrace(ioe);
			}
		}
		return this.bencoded_data;
	}
	
	public Map getDataMap() {
		return this.data_dict;
	}
	
	public String getClientName() {
		byte[] client_name = (byte[])data_dict.get("v");
		if (client_name == null) {return null;}
		try {return new String(client_name, Constants.DEFAULT_ENCODING);}
		catch (java.io.IOException ioe) {return null;}
	}
	
	public boolean isUploadOnly() {
		Long ulOnly = (Long)data_dict.get("upload_only");
		return ulOnly != null && ulOnly.longValue() > 0L;
	}
	
	public InetAddress getIPv6() {
		byte[] addr = (byte[])data_dict.get("ipv6");
		if(addr != null && addr.length == 16)
		{
			try
			{
				return InetAddress.getByAddress(addr);
			} catch (UnknownHostException e)
			{
				// should not happen
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public int getTCPListeningPort()
	{
		Long port = (Long)data_dict.get("p");
		if(port == null)
			return 0;
		int val = port.intValue();
		if(val <= 65535 && val > 0)
			return val;
		return 0;
	}
	
	public Boolean isCryptoRequested()
	{
		Long crypto = (Long)data_dict.get("e");
		if(crypto == null)
			return null;
		return Boolean.valueOf(crypto.longValue() == 1);
	}
	
	public Map getExtensionMapping() {
		Map result = (Map)data_dict.get("m");
		return (result == null) ? Collections.EMPTY_MAP : result;
	}

	public void
	addDefaultExtensionMappings(
		boolean		enable_pex,
		boolean		enable_md )
	{
		if ( enable_pex | enable_md ){
			Map ext = (Map)data_dict.get("m");
			
			if ( ext == null ){
				ext = new HashMap();
				data_dict.put( "m", ext );
			}
	
			if ( enable_pex ){
				
				ext.put( ID_UT_PEX, new Long( SUBID_UT_PEX ));
			}
			
			if ( enable_md ){
				
				ext.put( ID_UT_METADATA, new Long( SUBID_UT_METADATA ));
			}
		}
	}
	
	public String getFeatureID() {return LTMessage.LT_FEATURE_ID;}
	public int getFeatureSubID() {return LTMessage.SUBID_LT_HANDSHAKE;}
	public String getID() {return LTHandshake.ID_LT_HANDSHAKE;}
	public byte[] getIDBytes() {return LTHandshake.ID_LT_HANDSHAKE_BYTES;}
    public int getType() {return Message.TYPE_PROTOCOL_PAYLOAD;}
	public byte getVersion() {return this.version;}


}
