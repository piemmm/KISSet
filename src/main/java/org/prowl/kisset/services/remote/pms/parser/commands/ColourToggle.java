package org.prowl.kisset.services.remote.pms.parser.commands;

import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.PMSCommand;
import org.prowl.kisset.services.remote.pms.parser.Mode;

import java.io.IOException;

@PMSCommand
public class ColourToggle extends Command {

    /**
     * Colour toggle is a special case command and is accessible in any mode.
     *
     * @param data
     * @return
     * @throws IOException
     */
    @Override
    public boolean doCommand(String[] data) throws IOException {

        if (!getMode().equals(Mode.CMD)) {
            return false;
        }


        client.setColourEnabled(!client.getColourEnabled());
        if (client.getColourEnabled()) {
            write(Messages.get("colourEnabled") + CR);
        } else {
            write(Messages.get("colourDisabled") + CR);
        }

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"CC"};
    }
}
