package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class LocaleLite implements IsSerializable {
	private String country;
	private String language;
	private String code;

	public LocaleLite(String country, String language, String code) {
		super();
		this.country = country;
		this.language = language;
		this.code = code;
	}

	public LocaleLite() {

	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
