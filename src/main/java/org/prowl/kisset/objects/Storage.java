package org.prowl.kisset.objects;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.netrom.RoutingTable;
import org.prowl.kisset.objects.messages.Message;
import org.prowl.kisset.objects.netrom.NetROMNode;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Storage class allows objects to be stored on disk to keep things simple.
 * <p>
 * Directory based storage, where parts of the object are split up into pieces
 * and recombined when needed as we have plenty of CPU horsepower.
 */
public class Storage {

    private static final Log LOG = LogFactory.getLog("Storage");
    private static final String STORAGE_LOCATION = "storage";
    private static final String NEWS = "news";
    private static final String NETROM = "netrom";
    private static final String USER = "user";
    // Cache of messages
    private static final Cache<String, Message> BIDMIDToMsg = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(7, TimeUnit.DAYS).build();
    private static final Cache<Long, Message> messageIdToMsg = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(7, TimeUnit.DAYS).build();
    private static final Cache<File, Message> messageFileToMsg = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(7, TimeUnit.DAYS).build();
    private static long highestMessageIdSeen = -1;
    private File locationDir = getStorageDir();


    public Storage() {
    }


    /**
     * Store a news message
     * <p>
     * Paths are: module:date:group:messageFile
     *
     * @param message
     */
    public void storeNewsMessage(Message message) throws IOException {
        // Write it to disk
        storeData(getNewsMessageFile(message), message.toPacket());
    }

    public File getStorageDir() {
        String userHome = System.getProperty("user.home");
        File appDir = new File(userHome, ".kisset");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        File storageDir = new File(appDir, STORAGE_LOCATION);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir;
    }

    public File getNewsMessageFile(Message message) {
        String filename = message.getBID_MID();
        File itemDir = new File(locationDir.getAbsolutePath() + File.separator + NEWS + File.separator + message.getGroup());
        if (!itemDir.exists()) {
            itemDir.mkdirs();
        }
        return new File(itemDir, filename);
    }

    /**
     * Convenience method
     *
     * @param BID_MID
     * @return
     */
    public File getNewsMessageFile(String BID_MID) {
        Message empty = new Message();
        empty.setBID_MID(BID_MID);
        return getNewsMessageFile(empty);
    }


    /**
     * Convenience method for if a message exists already
     * <p>
     * MIDBID and Group must be populated.
     * <p>
     * Checks local storage to see if a news message already exists
     */
    public boolean doesNewsMessageExist(Message message) {
        return getNewsMessageFile(message).exists();
    }

    /**
     * Convert a time in milliseconds to a directory slot.
     *
     * @param timeMillis
     * @return
     */
    private final String timeToSlot(long timeMillis) {
        // Split this down into directories about 1 day (86400000millis ish) apart
        String dateStr = Long.toString((int) (timeMillis / 86400000d));
        return dateStr;
    }

    private void storeData(File file, byte[] data) throws IOException {
        // Ensure directory tree exists
        file.getParentFile().mkdirs();
        if (!file.getParentFile().exists()) {
            throw new IOException("Unable to create directory: " + file.getParentFile().getAbsolutePath());
        }

        // Actually try to save the file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (Throwable e) {
            throw new IOException("Unable to persist file: " + file.getAbsolutePath());
        }
    }


    /**
     * Get a list of news group messages going back as far as date X.
     */
    public File[] listMessages(String group) {
        List<File> files = new ArrayList<>();
        if (group != null) {
            // Get for a group
            File groupFile = new File(locationDir.getAbsolutePath() + File.separator + NEWS, group);
            if (groupFile != null) {
                getMessageList(files, groupFile);
            }
        } else {
            // Get all messages
            File[] groups = new File(locationDir.getAbsolutePath() + File.separator + NEWS + File.separator).listFiles();
            if (groups != null) {
                for (File fgroup : groups) {
                    getMessageList(files, fgroup);
                }
            }
        }

        return files.toArray(new File[files.size()]);
    }

    /**
     * Get a message based on it's messageId
     *
     * @param messageId
     * @return
     */
    public Message getMessage(long messageId) {

        // Check cache first to avoid disk access
        Message message = messageIdToMsg.getIfPresent(messageId);
        if (message != null) {
            return message;
        }

        // If not in cache then retrieve from disk
        List<Message> messages = getMessagesInOrder(null);

        // We could check the cache, but it might have been evicted, so we will check iterate the list anyway
        for (Message msg : messages) {
            if (msg.getMessageNumber() == messageId) {
                message = msg;
                break;
            }
        }

        // Probably re-insert into the cache, just to be sure it wasn't evicted already.
        if (message != null) {
            messageIdToMsg.put(message.getMessageNumber(), message);
        }
        return message;
    }

    /**
     * Delete a message from the local storage
     *
     * @param messagId the message id to delete
     * @return true if the message was deleted
     */
    public boolean deleteMessage(long messageId) {
        Message message = getMessage(messageId);
        if (message != null) {
            File file = getNewsMessageFile(message);
            if (file.exists()) {
                return file.delete();
            }
        }
        return false;
    }

    /**
     * Get a list of messages for a group and add to the supplied list
     *
     * @param files  the list we will append files found to
     * @param fgroup the group to list files for
     */
    private void getMessageList(List<File> files, File fgroup) {
        try {
            File[] messages = fgroup.listFiles();
            if (messages != null) {
                files.addAll(Arrays.asList(messages));
            }
        } catch (Throwable e) {
            // Ignore the 'not a date' file
            LOG.debug("Ignoring file path:" + fgroup, e);
        }
    }

    /**
     * Get a list of all news group messages going back as far as date X.
     */
    public File[] listMessages() {
        return listMessages(null);
    }


    /**
     * Retrieve a news message
     * <p>
     * Message IDs are immutable, so we can cache the references.
     *
     * @param f
     * @return
     * @throws IOException
     */
    public Message loadNewsMessage(File f) throws IOException {

        Message message = messageFileToMsg.getIfPresent(f);
        if (message != null) {
            return message;
        }

        // Otherwise load the message from disk.
        message = new Message();
        try {
            message.fromPacket(loadData(f));
        } catch (InvalidMessageException e) {
            throw new IOException(e);
        }

        // Store in cache for quick lookup.
        BIDMIDToMsg.put(message.getBID_MID(), message);
        messageIdToMsg.put(message.getMessageNumber(), message);
        messageFileToMsg.put(f, message);

        return message;
    }


    /**
     * Load a data file
     *
     * @param file
     * @return
     * @throws IOException
     */
    private DataInputStream loadData(File file) throws IOException {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    }

    /**
     * Get the node file given its callsign.
     *
     * @return
     */
    private File getNodePropertiesFile(String callsign) {
        File file = new File(locationDir.getAbsolutePath() + File.separator + "syncstate" + File.separator + callsign + ".properties");
        file.getParentFile().mkdirs();
        return file;

    }


    /**
     * Get a list of messages in message id order.
     *
     * @param groupName the group to list messages for, or null for all groups
     * @return a list of messages in message id order.
     */
    public List<Message> getMessagesInOrder(String groupName) {
        File[] files = listMessages();
        List<Message> messages = new ArrayList<>();
        for (File file : files) {
            try {
                Message message = loadNewsMessage(file);
                messages.add(message);
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }

        // Sort them in id order
        Collections.sort(messages, new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                if (o1.getMessageNumber() < o2.getMessageNumber()) {
                    return 1;
                } else if (o1.getMessageNumber() > o2.getMessageNumber()) {
                    return -1;
                }
                return 0;
            }
        });

        return messages;
    }

    /**
     * Scan the news AND mail folder looking for the highest message ID and store in memory and returns
     * the next one.
     */
    public synchronized long getNextMessageID() {
        if (highestMessageIdSeen != -1) {
            return ++highestMessageIdSeen;
        }
        long highest = -1;
        File[] files = listMessages();
        for (File f : files) {
            try {
                Message message = loadNewsMessage(f);
                if (highest < message.getMessageNumber()) {
                    highest = message.getMessageNumber();
                }
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }

        if (highest <= 0) {
            highest = 10000; // Starting id
        }

        highestMessageIdSeen = ++highest;
        return highest;
    }

    public static void write(DataOutputStream dout, long i) throws IOException {
        dout.writeLong(i);
    }

    public static void write(DataOutputStream dout, int i) throws IOException {
        dout.writeInt(i);
    }

    public static void write(DataOutputStream dout, String s) throws IOException {
        // String.length measures UTF units, which is no good to use, so we will use the
        // byte array size
        byte[] b = s.getBytes();
        dout.writeInt(b.length);
        dout.write(b);
    }


    public File getRouteFile() {
        File routeFile = new File(locationDir.getAbsolutePath() + File.separator + NETROM + File.separator + "routes.dat");
        routeFile.getParentFile().mkdirs();
        return routeFile;
    }

    /**
     * Save the NetROM routing table to disk in a single file
     */
    public void saveNetROMRoutingTable() {
        List<NetROMNode> nodes = new ArrayList<>(RoutingTable.INSTANCE.getNodes());

        File routeFile = getRouteFile();
        routeFile.delete();

        try (FileOutputStream fos = new FileOutputStream(routeFile);
             DataOutputStream dout = new DataOutputStream(fos)) {

            dout.writeInt(nodes.size());
            for (NetROMNode node : nodes) {
                dout.write(node.toPacket());
            }
            dout.flush();

        } catch (Throwable e) {
            LOG.error("Error saving NetROM routing table", e);
        }
    }

    /**
     * Load the NetROM routing table from disk, removing any that have 'expired'.
     */
    public void loadNetROMRoutingTable() {

        File routeFile = getRouteFile();
        if (!routeFile.exists()) {
            LOG.info("No existing NetROM routing table found to load");
            return;
        }

        try (FileInputStream fin = new FileInputStream(routeFile);
             DataInputStream din = new DataInputStream(fin)) {

            int count = din.readInt();
            for (int i = 0; i < count; i++) {
                NetROMNode node = new NetROMNode();
                node.fromPacket(din);
                if (!node.isExipred()) {
                    RoutingTable.INSTANCE.addNode(node);
                }
            }

        } catch (Throwable e) {
            LOG.error("Error loading NetROM routing table", e);
        }

    }

}