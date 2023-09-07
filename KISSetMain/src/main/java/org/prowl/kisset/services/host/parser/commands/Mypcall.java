package org.prowl.kisset.services.host.parser.commands;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangeCompleteEvent;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.PacketTools;

import java.io.IOException;

/**
 * Display or set the station mailbox callsign
 */
@TNCCommand
public class Mypcall extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        Config config = KISSet.INSTANCE.getConfig();
        String currentPMSSSID = config.getConfig(Conf.pmsSSID, Conf.pmsSSID.stringDefault());
        String mypcall = KISSet.INSTANCE.getMyCallNoSSID() + currentPMSSSID;
        if (data.length == 1) {
            writeToTerminal("*** MYPcall is " +mypcall + CR);
        } else {

            String baseNewCall = PacketTools.getBaseCall(data[1]);
            String ssid = PacketTools.getSSID(data[1]);

            // base call must be the same as mypcall base
            if (!baseNewCall.equalsIgnoreCase(KISSet.INSTANCE.getMyCallNoSSID())) {
                writeToTerminal("*** MYPcall base callsign must be same as mycall" + KISSet.INSTANCE.getMyCallNoSSID() + CR);
                return true;
            }

            // The SSID if present on the basecall must not be the same as the main callsign (if it has an ssid)
            if (ssid.equalsIgnoreCase(PacketTools.getSSID(KISSet.INSTANCE.getMyCall()))) {
                writeToTerminal("*** MYPcall SSID must be different to MYcall SSID" + CR);
                return true;
            }

            // Now we passed the above, we can change the config.
            String call = data[1].toUpperCase();
            writeToTerminal("*** MYPcall set to " + baseNewCall+ssid + CR);
            KISSet.INSTANCE.getConfig().setProperty(Conf.pmsSSID, ssid).saveConfig();

            // Update the station config (which also restarts services)
            SingleThreadBus.INSTANCE.post(new ConfigurationChangedEvent());
            SingleThreadBus.INSTANCE.post(new ConfigurationChangeCompleteEvent(false));

        }

        return true;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"myp", "mypcall"};
    }

}
