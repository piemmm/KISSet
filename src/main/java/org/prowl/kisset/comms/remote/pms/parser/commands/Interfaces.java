//package org.prowl.kisset.comms.remote.pms.parser.commands;
//
//import org.apache.commons.lang.StringUtils;
//import org.prowl.kisset.KISSet;
//import org.prowl.kisset.annotations.PMSCommand;
//import org.prowl.kisset.comms.remote.pms.parser.Mode;
//import org.prowl.kisset.io.Interface;
//import org.prowl.kisset.util.ANSI;
//
//import java.io.IOException;
//import java.text.NumberFormat;
//
//@PMSCommand
//public class Interfaces extends Command {
//
//    @Override
//    public boolean doCommand(String[] data) throws IOException {
//
//        // We're only interesteed in comamnd moed.
//        if (!getMode().equals(Mode.CMD)) {
//            return false;
//        }
//
//        write(CR);
//
//        NumberFormat nf = NumberFormat.getInstance();
//        nf.setMaximumFractionDigits(4);
//        nf.setMinimumFractionDigits(3);
//
//        NumberFormat nfb = NumberFormat.getInstance();
//        nfb.setMaximumFractionDigits(1);
//        nfb.setMinimumFractionDigits(1);
//
//        // No parameter? Just list the interfaces then
//        if (data.length == 1) {
//            write(CR + ANSI.BOLD + ANSI.UNDERLINE + "No. Interface                                      " + ANSI.NORMAL + CR);
//            int i = 0;
//            for (Interface anInterface : KISSet.INSTANCE.getInterfaceHandler().getInterfaces()) {
//                String status = anInterface.getInterfaceStatus()
//                if (status == null) {
//                    status = "OK";
//                }
//                write(StringUtils.rightPad(Integer.toString(i) + ": ", 4) + StringUtils.rightPad(anInterface.toString(), 25) + StringUtils.rightPad(status, 30) + CR);
//                i++;
//            }
//            write(CR);
//            return true;
//        }
//        return true;
//    }
//
//
//    @Override
//    public String[] getCommandNames() {
//        return new String[]{"int", "ports", "i", "interfaces"};
//    }
//}
