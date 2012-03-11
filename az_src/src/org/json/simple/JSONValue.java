/*
 * $Id: JSONValue.java,v 1.1 2007-06-05 00:43:56 tuxpaper Exp $
 * Created on 2006-4-15
 */
package org.json.simple;

import java.io.Reader;
import java.io.StringReader;

import org.json.simple.parser.JSONParser;


/**
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public class JSONValue {
	/**
	 * parse into java object from input source.
	 * @param in
	 * @return instance of : JSONObject,JSONArray,String,Boolean,Long,Double or null
	 */
	public static Object parse(Reader in){
		try{
			JSONParser parser=new JSONParser();
			return parser.parse(in);
		}
		catch(Exception e){
			return null;
		}
	}
	
	public static Object parse(String s){
		StringReader in=new StringReader(s);
		return parse(in);
	}
}
