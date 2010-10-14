/*
 * File    : ViewUtils.java
 * Created : 24-Oct-2003
 * By      : parg
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.InputShell;

/**
 * @author parg
 */

public class 
ViewUtils 
{
	public static void
	addSpeedMenu(
		final Shell 		shell,
		Menu				menuAdvanced,
		boolean				isTorrentContext,
		boolean				hasSelection,
		boolean				downSpeedDisabled,
		boolean				downSpeedUnlimited,
		long				totalDownSpeed,
		long				downSpeedSetMax,
		long				maxDownload,
		boolean				upSpeedDisabled,
		boolean				upSpeedUnlimited,
		long				totalUpSpeed,
		long				upSpeedSetMax,
		long				maxUpload,
		final int			num_entries,
		final SpeedAdapter	adapter )
	{
		// advanced > Download Speed Menu //
		final MenuItem itemDownSpeed = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemDownSpeed, "MyTorrentsView.menu.setDownSpeed"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemDownSpeed, "speed");

		final Menu menuDownSpeed = new Menu(shell, SWT.DROP_DOWN);
		itemDownSpeed.setMenu(menuDownSpeed);

		final MenuItem itemCurrentDownSpeed = new MenuItem(menuDownSpeed, SWT.PUSH);
		itemCurrentDownSpeed.setEnabled(false);
		StringBuffer speedText = new StringBuffer();
		String separator = "";
		//itemDownSpeed.                   
		if (downSpeedDisabled) {
			speedText.append(MessageText
					.getString("MyTorrentsView.menu.setSpeed.disabled"));
			separator = " / ";
		}
		if (downSpeedUnlimited) {
			speedText.append(separator);
			speedText.append(MessageText
					.getString("MyTorrentsView.menu.setSpeed.unlimited"));
			separator = " / ";
		}
		if (totalDownSpeed > 0) {
			speedText.append(separator);
			speedText.append(DisplayFormatters
					.formatByteCountToKiBEtcPerSec(totalDownSpeed));
		}
		itemCurrentDownSpeed.setText(speedText.toString());

		new MenuItem(menuDownSpeed, SWT.SEPARATOR);

		final MenuItem itemsDownSpeed[] = new MenuItem[12];
		Listener itemsDownSpeedListener = new Listener() {
			public void handleEvent(Event e) {
				if (e.widget != null && e.widget instanceof MenuItem) {
					MenuItem item = (MenuItem) e.widget;
					int speed = item.getData("maxdl") == null ? 0 : ((Integer) item
							.getData("maxdl")).intValue();
					adapter.setDownSpeed(speed);
				}
			}
		};

		itemsDownSpeed[1] = new MenuItem(menuDownSpeed, SWT.PUSH);
		Messages.setLanguageText(itemsDownSpeed[1],
				"MyTorrentsView.menu.setSpeed.unlimit");
		itemsDownSpeed[1].setData("maxdl", new Integer(0));
		itemsDownSpeed[1].addListener(SWT.Selection, itemsDownSpeedListener);

		if (hasSelection) {

			//using 200KiB/s as the default limit when no limit set.
			if (maxDownload == 0){		
				if ( downSpeedSetMax == 0 ){
					maxDownload = 200 * 1024;
				}else{
					maxDownload	= 4 * ( downSpeedSetMax/1024 ) * 1024;
				}
			}

			for (int i = 2; i < 12; i++) {
				itemsDownSpeed[i] = new MenuItem(menuDownSpeed, SWT.PUSH);
				itemsDownSpeed[i].addListener(SWT.Selection, itemsDownSpeedListener);
	
				// dms.length has to be > 0 when hasSelection
				int limit = (int)(maxDownload / (10 * num_entries) * (12 - i));
				StringBuffer speed = new StringBuffer();
				speed.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit
						* num_entries));
				if (num_entries > 1) {
					speed.append(" ");
					speed.append(MessageText
							.getString("MyTorrentsView.menu.setSpeed.in"));
					speed.append(" ");
					speed.append(num_entries);
					speed.append(" ");
					speed.append(MessageText
							.getString("MyTorrentsView.menu.setSpeed.slots"));
					speed.append(" ");
					speed
							.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit));
				}
				itemsDownSpeed[i].setText(speed.toString());
				itemsDownSpeed[i].setData("maxdl", new Integer(limit));
			}
		}

		// ---
		new MenuItem(menuDownSpeed, SWT.SEPARATOR);
		
		String menu_key = "MyTorrentsView.menu.manual";
		if (num_entries > 1) {menu_key += (isTorrentContext?".per_torrent":".per_peer" );}

		final MenuItem itemDownSpeedManualSingle = new MenuItem(menuDownSpeed, SWT.PUSH);
		Messages.setLanguageText(itemDownSpeedManualSingle, menu_key);
		itemDownSpeedManualSingle.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int speed_value = getManualSpeedValue(shell, true);
				if (speed_value > 0) {adapter.setDownSpeed(speed_value);}
			}
		});
		
		if (num_entries > 1) {
			final MenuItem itemDownSpeedManualShared = new MenuItem(menuDownSpeed, SWT.PUSH);
			Messages.setLanguageText(itemDownSpeedManualShared, isTorrentContext?"MyTorrentsView.menu.manual.shared_torrents":"MyTorrentsView.menu.manual.shared_peers");
			itemDownSpeedManualShared.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					int speed_value = getManualSharedSpeedValue(shell, true, num_entries);
					if (speed_value > 0) {adapter.setDownSpeed(speed_value);}
				}
			});
		}
		
		// advanced >Upload Speed Menu //
		final MenuItem itemUpSpeed = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemUpSpeed, "MyTorrentsView.menu.setUpSpeed"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemUpSpeed, "speed");

		final Menu menuUpSpeed = new Menu(shell, SWT.DROP_DOWN);
		itemUpSpeed.setMenu(menuUpSpeed);

		final MenuItem itemCurrentUpSpeed = new MenuItem(menuUpSpeed, SWT.PUSH);
		itemCurrentUpSpeed.setEnabled(false);
		separator = "";
		speedText = new StringBuffer();
		//itemUpSpeed.                   
		if (upSpeedDisabled) {
			speedText.append(MessageText
					.getString("MyTorrentsView.menu.setSpeed.disabled"));
			separator = " / ";
		}
		if (upSpeedUnlimited) {
			speedText.append(separator);
			speedText.append(MessageText
					.getString("MyTorrentsView.menu.setSpeed.unlimited"));
			separator = " / ";
		}
		if (totalUpSpeed > 0) {
			speedText.append(separator);
			speedText.append(DisplayFormatters
					.formatByteCountToKiBEtcPerSec(totalUpSpeed));
		}
		itemCurrentUpSpeed.setText(speedText.toString());

		// ---
		new MenuItem(menuUpSpeed, SWT.SEPARATOR);

		final MenuItem itemsUpSpeed[] = new MenuItem[12];
		Listener itemsUpSpeedListener = new Listener() {
			public void handleEvent(Event e) {
				if (e.widget != null && e.widget instanceof MenuItem) {
					MenuItem item = (MenuItem) e.widget;
					int speed = item.getData("maxul") == null ? 0 : ((Integer) item
							.getData("maxul")).intValue();
					adapter.setUpSpeed(speed);
				}
			}
		};

		itemsUpSpeed[1] = new MenuItem(menuUpSpeed, SWT.PUSH);
		Messages.setLanguageText(itemsUpSpeed[1],
				"MyTorrentsView.menu.setSpeed.unlimit");
		itemsUpSpeed[1].setData("maxul", new Integer(0));
		itemsUpSpeed[1].addListener(SWT.Selection, itemsUpSpeedListener);

		if (hasSelection) {
			//using 75KiB/s as the default limit when no limit set.
			if (maxUpload == 0){
				maxUpload = 75 * 1024;
			}else{
				if ( upSpeedSetMax == 0 ){
					maxUpload = 200 * 1024;
				}else{
					maxUpload = 4 * ( upSpeedSetMax/1024 ) * 1024;
				}
			}
			for (int i = 2; i < 12; i++) {
				itemsUpSpeed[i] = new MenuItem(menuUpSpeed, SWT.PUSH);
				itemsUpSpeed[i].addListener(SWT.Selection, itemsUpSpeedListener);

				int limit = (int)( maxUpload / (10 * num_entries) * (12 - i));
				StringBuffer speed = new StringBuffer();
				speed.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit
						* num_entries));
				if (num_entries > 1) {
					speed.append(" ");
					speed.append(MessageText
							.getString("MyTorrentsView.menu.setSpeed.in"));
					speed.append(" ");
					speed.append(num_entries);
					speed.append(" ");
					speed.append(MessageText
							.getString("MyTorrentsView.menu.setSpeed.slots"));
					speed.append(" ");
					speed
							.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit));
				}

				itemsUpSpeed[i].setText(speed.toString());
				itemsUpSpeed[i].setData("maxul", new Integer(limit));
			}
		}

		new MenuItem(menuUpSpeed, SWT.SEPARATOR);

		final MenuItem itemUpSpeedManualSingle = new MenuItem(menuUpSpeed, SWT.PUSH);
		Messages.setLanguageText(itemUpSpeedManualSingle, menu_key);
		itemUpSpeedManualSingle.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int speed_value = getManualSpeedValue(shell, false);
				if (speed_value > 0) {adapter.setUpSpeed(speed_value);}
			}
		});
		
		if (num_entries > 1) {
			final MenuItem itemUpSpeedManualShared = new MenuItem(menuUpSpeed, SWT.PUSH);
			Messages.setLanguageText(itemUpSpeedManualShared, isTorrentContext?"MyTorrentsView.menu.manual.shared_torrents":"MyTorrentsView.menu.manual.shared_peers" );
			itemUpSpeedManualShared.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					int speed_value = getManualSharedSpeedValue(shell, false, num_entries);
					if (speed_value > 0) {adapter.setUpSpeed(speed_value);}
				}
			});
		}
		
	}
	
	public static int getManualSpeedValue(Shell shell, boolean for_download) {
		String kbps_str = MessageText.getString("MyTorrentsView.dialog.setNumber.inKbps",
				new String[]{ DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB ) });
		
		String set_num_str = MessageText.getString("MyTorrentsView.dialog.setNumber." +
				((for_download) ? "download" : "upload"));
		
		InputShell is = new InputShell(
				"MyTorrentsView.dialog.setSpeed.title",
				new String[] {set_num_str},
				"MyTorrentsView.dialog.setNumber.text",
				new String[] {
						kbps_str,
						set_num_str
				});

		String sReturn = is.open();
		if (sReturn == null)
			return -1;

		try {
			int result = (int) (Double.valueOf(sReturn).doubleValue() * 1024);
			if (result <= 0) {throw new NumberFormatException("non-positive number entered");}
			return result;
		} catch (NumberFormatException er) {
			MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			mb.setText(MessageText
					.getString("MyTorrentsView.dialog.NumberError.title"));
			mb.setMessage(MessageText
					.getString("MyTorrentsView.dialog.NumberError.text"));

			mb.open();
			return -1;
		}
	}
	
	public static int getManualSharedSpeedValue(Shell shell, boolean for_download, int num_entries) {
		int result = getManualSpeedValue(shell, for_download);
		if (result == -1) {return -1;}
		result = result / num_entries;
		if (result == 0) {result = 1;}
		return result;
	}
	
	public interface
	SpeedAdapter
	{
		public void
		setUpSpeed(
			int		val );
		
		public void
		setDownSpeed(
			int		val );
	}
}
