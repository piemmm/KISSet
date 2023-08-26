package org.prowl.kisset.util.compression.dictblock;

public class RollingDictionary {

    // 2 dictionaries of 32k
    private final byte[] dictionary;

    private int destPos = 0;   // The current position in the writing dictionary

    public RollingDictionary() {
        dictionary = new byte[32768];
    }

    public byte[] getDictionary() {
        return dictionary;
    }

    public void addToDictionary(byte[] array, int offset, int length) {

        // Add to the dictionary, byte walking and loop around when we get to the end
        for (int i = 0; i < length; i++) {
            dictionary[destPos] = array[offset + i];
            destPos++;
            if (destPos >= dictionary.length) {
                destPos = 0;
            }
        }

    }

}
