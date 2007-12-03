package net.benhui.btgallery.bluelet;

import javax.bluetooth.RemoteDevice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.List;

/**
 * This version of BLUElet was modified for bbTracker by Joachim Sauer.
 * 
 * <p>
 * Title: Remote Device List Component
 * </p>
 * <p>
 * Description: This is a List screen to display a list of discovered Bluetooth devices. This is a class used by
 * BLUEletUI.
 * 
 * </p>
 * 
 * @author Ben Hui (www.benhui.net)
 * @version 1.0
 * 
 * LICENSE: This code is licensed under GPL. (See http://www.gnu.org/copyleft/gpl.html)
 */
class RemoteDeviceUI extends List {

	public RemoteDeviceUI() {
		super("Bluetooth devices", List.IMPLICIT);

		addCommand(new Command("Select", Command.SCREEN, 1));
		addCommand(new Command("Search", Command.SCREEN, 2));
		addCommand(BLUElet.BACK);

		setCommandListener(BLUElet.instance);
	}

	/**
	 * Set a one-line message to screen.
	 * 
	 * @param str
	 *            String
	 */
	public void setMsg(final String str) {
		// *** super.deleteAll();
		while (super.size() > 0) {
			super.delete(0);
		}
		append(str, null);

	}

	/**
	 * refresh the list with blutooth devices
	 */
	public void showui() {
		// *** super.deleteAll();
		while (super.size() > 0) {
			super.delete(0);
		}

		if (BLUElet.devices.size() > 0) {
			for (int i = 0; i < BLUElet.devices.size(); i++) {
				try {
					final RemoteDevice device = (RemoteDevice) BLUElet.devices.elementAt(i);
					final String name = device.getFriendlyName(false);
					append(name, null);

				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			append("[No Device Found]", null);
		}
	}

}
