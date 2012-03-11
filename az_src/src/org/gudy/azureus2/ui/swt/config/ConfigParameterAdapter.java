/*
 * Created on 16-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.config;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.config.generic.GenericParameterAdapter;

public class ConfigParameterAdapter extends GenericParameterAdapter
{
	private static final int CHANGINGCOUNT_BREAKER = 5;

	private Parameter owner;

	private int changingCount = 0;

	private boolean changedExternally = false;

	protected ConfigParameterAdapter(Parameter _owner, final String configID) {
		owner = _owner;

		COConfigurationManager.addParameterListener(configID,
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						try {
						if (!owner.isInitialised()){
							return;
						}
  						if (owner.isDisposed()) {
  							COConfigurationManager.removeParameterListener(parameterName, this);
  							return;
  						}
  
  						informChanged(true);
  						
  						Object valueObject = owner.getValueObject();
  
  						if (valueObject instanceof Boolean) {
  							boolean b = COConfigurationManager.getBooleanParameter(parameterName);
  							owner.setValue(new Boolean(b));
  						} else if (valueObject instanceof Integer) {
  							int i = COConfigurationManager.getIntParameter(parameterName);
  							owner.setValue(new Integer(i));
  						} else if (valueObject instanceof String) {
  							String s = COConfigurationManager.getStringParameter(parameterName);
  							owner.setValue(s);
  						}
						} catch (Exception e) {
							Debug.out("parameterChanged trigger from ConfigParamAdapter "
									+ configID, e);
						}
					}
				});
	}

	public int getIntValue(String key) {
		return (COConfigurationManager.getIntParameter(key));
	}

	public int getIntValue(String key, int def) {
		return (COConfigurationManager.getIntParameter(key, def));
	}

	public void setIntValue(String key, int value) {
		if (changingCount == 0) {
			changedExternally = false;
		}
		changingCount++;
		try {
			if (getIntValue(key) == value) {
				changedExternally = true;
				return;
			}

			if (changingCount > CHANGINGCOUNT_BREAKER) {
				Debug.out("Preventing StackOverflow on setting " + key + " to " + value
						+ " (was " + getIntValue(key) + ") via "
						+ Debug.getCompressedStackTrace());
				changingCount = 1;
			} else {
				informChanging(value);

				if (!changedExternally) {
					COConfigurationManager.setParameter(key, value);
					changedExternally = true;
				}
			}

		} finally {
			changingCount--;
		}
	}

	public boolean resetIntDefault(String key) {
		if (COConfigurationManager.doesParameterDefaultExist(key)) {
			COConfigurationManager.removeParameter(key);
			return (true);
		}

		return (false);
	}

	public boolean getBooleanValue(String key) {
		return (COConfigurationManager.getBooleanParameter(key));
	}

	public boolean getBooleanValue(String key, boolean def) {
		return (COConfigurationManager.getBooleanParameter(key, def));
	}

	public void setBooleanValue(String key, boolean value) {
		if (changingCount == 0) {
			changedExternally = false;
		}

		changingCount++;
		try {
			if (getBooleanValue(key) == value) {
				changedExternally = true;
				return;
			}

			if (changingCount > CHANGINGCOUNT_BREAKER) {
				Debug.out("Preventing StackOverflow on setting " + key + " to " + value
						+ " (was " + getBooleanValue(key) + ") via "
						+ Debug.getCompressedStackTrace());
				changingCount = 1;
			} else {
				informChanging(value);

				if (!changedExternally) {
					COConfigurationManager.setParameter(key, value);
					changedExternally = true;
				}
			}

		} finally {
			changingCount--;
		}
	}

	public void informChanged(boolean internally) {
		if (owner.change_listeners != null) {
			for (int i = 0; i < owner.change_listeners.size(); i++) {
				try {
					((ParameterChangeListener) owner.change_listeners.get(i)).parameterChanged(
							owner, internally);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void informChanging(int toValue) {
		if (owner.change_listeners != null) {
			for (int i = 0; i < owner.change_listeners.size(); i++) {
				try {
					ParameterChangeListener l = (ParameterChangeListener) owner.change_listeners.get(i);
					l.intParameterChanging(owner, toValue);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void informChanging(boolean toValue) {
		if (owner.change_listeners != null) {
			for (int i = 0; i < owner.change_listeners.size(); i++) {
				try {
					ParameterChangeListener l = (ParameterChangeListener) owner.change_listeners.get(i);
					l.booleanParameterChanging(owner, toValue);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void informChanging(String toValue) {
		if (owner.change_listeners != null) {
			for (int i = 0; i < owner.change_listeners.size(); i++) {
				try {
					ParameterChangeListener l = (ParameterChangeListener) owner.change_listeners.get(i);
					l.stringParameterChanging(owner, toValue);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void informChanging(double toValue) {
		if (owner.change_listeners != null) {
			for (int i = 0; i < owner.change_listeners.size(); i++) {
				try {
					ParameterChangeListener l = (ParameterChangeListener) owner.change_listeners.get(i);
					l.floatParameterChanging(owner, toValue);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}
}
