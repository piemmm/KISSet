package org.prowl.kisset.objects.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.objects.InvalidMessageException;
import org.prowl.kisset.util.Tools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents a user.
 * <p>
 * A user has a name, priviliges, and a callsign.
 */
public class User {

    private static final Log LOG = LogFactory.getLog("User");

    private String name;
    private String baseCallsign;
    private String privFlags;
    private String location;
    private String passwordHash; // This is used for things like telnet remote access (not over radio) or sysop acces

    /**
     * Create an empty user
     */
    public User() {

    }

    /**
     * Create a user with a name, callsign, priviliges and password
     *
     * @param name
     * @param baseCallsign
     * @param privFlags
     * @param hashPassword
     */
    public User(String name, String baseCallsign, String privFlags, String hashPassword) {

        this.name = name;
        this.baseCallsign = baseCallsign;
        this.privFlags = privFlags;
        this.passwordHash = hashPassword;
    }

    /**
     * Take a password and return a hash seeded with the baseCallsign
     *
     * @param password     The password to hash
     * @param baseCallsign the callsign we will use as a seeed
     * @return
     */
    public static String hashPassword(String password, String baseCallsign) {
        return Tools.hashString(password + baseCallsign);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseCallsign() {
        return baseCallsign;
    }

    public void setBaseCallsign(String baseCallsign) {
        this.baseCallsign = baseCallsign;
    }

    public boolean hasPriv(String priv) {
        return privFlags.contains(priv);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Serialise into a byte array. Keeping the size to a minimum is important.
     * Length, data, length, data format for all the fields.
     *
     * @return A byte array representing the serialised message
     */
    public byte[] toPacket() {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(30);
             DataOutputStream dout = new DataOutputStream(bos)) {

            // String.length measures UTF units, which is no good to use, so we will use the
            // byte array size.
            byte[] bname = name.getBytes();
            byte[] bbaseCallsign = baseCallsign.getBytes();
            byte[] bprivFlags = privFlags.getBytes();
            byte[] blocation = location.getBytes();
            byte[] bPasswordHash = passwordHash.getBytes();
            dout.writeInt(bname.length);
            dout.write(bname);

            dout.writeInt(bbaseCallsign.length);
            dout.write(bbaseCallsign);

            dout.writeInt(bprivFlags.length);
            dout.write(bprivFlags);

            dout.writeInt(blocation.length);
            dout.write(blocation);

            dout.writeInt(bPasswordHash.length);
            dout.write(bPasswordHash);

            dout.flush();
            dout.close();
            return bos.toByteArray();

        } catch (Throwable e) {
            LOG.error("Unable to serialise message", e);
        }
        return null;
    }

    public String getPrivFlags() {
        return privFlags;
    }

    public void setPrivFlags(String privFlags) {
        this.privFlags = privFlags;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Deserialise from a byte array
     **/
    public User fromPacket(DataInputStream din) throws InvalidMessageException {

        try {

            String name = Tools.readString(din, din.readInt());
            String baseCallsign = Tools.readString(din, din.readInt());
            String privFlags = Tools.readString(din, din.readInt());
            String location = Tools.readString(din, din.readInt());
            String passwordHash = Tools.readString(din, din.readInt());

            setName(name);
            setBaseCallsign(baseCallsign);
            setPrivFlags(privFlags);
            setLocation(location);
            setPasswordHash(passwordHash);

        } catch (Throwable e) {
            LOG.error("Unable to build message from packet", e);
            throw new InvalidMessageException(e.getMessage(), e);
        }
        return this;
    }

}
