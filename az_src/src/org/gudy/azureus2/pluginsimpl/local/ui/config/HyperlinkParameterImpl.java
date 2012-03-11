/**
 * 
 */
package org.gudy.azureus2.pluginsimpl.local.ui.config;

import org.gudy.azureus2.plugins.ui.config.HyperlinkParameter;
import org.gudy.azureus2.pluginsimpl.local.PluginConfigImpl;

/**
 * @author Allan Crooks
 *
 */
public class HyperlinkParameterImpl extends LabelParameterImpl implements
		HyperlinkParameter {
	
	private String hyperlink;

	public HyperlinkParameterImpl(PluginConfigImpl config, String key, String label, String hyperlink) {
		super(config, key, label);
		this.hyperlink = hyperlink;
	}

	public String getHyperlink() {
		return hyperlink;
	}

	public void setHyperlink(String url_location) {
		this.hyperlink = url_location;
		
		fireParameterChanged();
	}
}
