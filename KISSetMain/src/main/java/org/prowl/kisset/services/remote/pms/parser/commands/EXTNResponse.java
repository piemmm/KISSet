package org.prowl.kisset.services.remote.pms.parser.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.annotations.PMSCommand;
import org.prowl.kisset.services.remote.pms.parser.Mode;
import org.prowl.kisset.util.Tools;
import org.prowl.kisset.util.compression.deflate.DeflateOutputStream;
import org.prowl.kisset.util.compression.deflate.InflateInputStream;

import java.io.IOException;

@PMSCommand
public class EXTNResponse extends Command {
    private static final Log LOG = LogFactory.getLog("EXTNResponse");

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (getMode().equals(Mode.CMD)) {
            StringBuffer acceptedExtensions = new StringBuffer();
            String extensions = data[1].substring(0, data[1].length() - 1);
            LOG.debug("Client has requested EXTN extensions to be enabled: " + extensions);

            // We will accept compression
            if (extensions.contains("C")) {
                acceptedExtensions.append("C");
            }

            // Now send the response once we have our accepted list.
            write(CR + "[EXTN " + acceptedExtensions + "]" + CR);
            client.flush();


            // Now we can activate compression
            if (extensions.contains("C")) {


                // Compression requires is to wrap the input and output streams in a GZIP stream
                LOG.debug("Compression enabled");
                client.setOutputStream(new DeflateOutputStream(client.getOutputStream()));
                client.useNewInputStream(new InflateInputStream(client.getInputStream()));

                Tools.delay(200);

            }
            return true;
        }
        return false;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"[EXTN"};
    }
}
