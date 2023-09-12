package org.prowl.kisset.services.remote.netrom.user.parser.commands;

import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.NodeCommand;
import org.prowl.kisset.services.remote.netrom.user.parser.Mode;

import java.io.IOException;

@NodeCommand
public class Bye extends Command {


    @Override
    public boolean doCommand(String[] data) throws IOException {
        // We're only interesteed in comamnd moed - other modes may need use these command words to exit their mode
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        // Possibly save user at this point?


        // Now say goodbye and close the connection
        write(CR);
        write(Messages.get(client.getUser(), "userDisconnecting") + CR);
        client.flush();
        // Disconnect the client.
        client.close();


        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"b", "q", "bye", "end", "logoff", "logout", "exit", "quit"};
    }
}
