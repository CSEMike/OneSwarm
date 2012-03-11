package com.aelitis.net.upnp.impl.device;

import com.aelitis.net.upnp.UPnPDeviceImage;

public class UPnPDeviceImageImpl
	implements UPnPDeviceImage
{
	public int width;

	public int height;

	public String location;
	
	public String mime;

	public UPnPDeviceImageImpl(int width, int height, String location, String mime) {
		this.width = width;
		this.height = height;
		this.location = location;
		this.mime = mime;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getLocation() {
		return location;
	}
	
	public String getMime() {
		return mime;
	}
}
