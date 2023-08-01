package org.prowl.kisset.comms.host.parser.commands;


import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.io.Stream;
import org.prowl.kisset.io.StreamState;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;

/**
 * Change streams on an interface to a different stream
 */
@TNCCommand
public class ChangeStream extends Command {


        @Override
        public boolean doCommand(String[] data) throws IOException {

            // No parameter? Just list the streams then
            if (data.length == 1) {
                writeToTerminal(CR+ANSI.BOLD+ANSI.UNDERLINE+"Stream  Status                                      "+ANSI.NORMAL + CR);
                int i = 0;
                for (Stream stream : commandParser.getCurrentInterface().getStreams()) {
                    writeToTerminal(StringUtils.rightPad(Integer.toString(i)+": ",8) + StringUtils.rightPad(stream.getStreamState().name(),30) + CR);
                    i++;
                }
                writeToTerminal(CR);
                return true;
            }


            // Prevent stream changes whilst in connecting state
            if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTING)) {
                writeToTerminal("*** Cannot change stream whilst connecting");
                return false;
            }


            return false;
        }


        @Override
        public String[] getCommandNames() {
            return new String[]{"streams", "|", "str","st"};
        }

}
