package org.prowl.kisset.util.compression.deflate;

public class Dictionary {

    private static final int DICTIONARY_SIZE = 32768;

    private final byte[] dictionary;
    private int position;

    public Dictionary() {
        dictionary = new byte[DICTIONARY_SIZE];
    }

    public void addToDictionary(byte[] array, int offset, int length) {
        // Add to the dictionary, byte walking and loop around when we get to the end
        for (int i = 0; i < length; i++) {
            dictionary[position] = array[offset + i];
            position++;
            if (position >= dictionary.length) {
                position = 0;
            }
        }

    }

    public byte[] getDictionary() {
        return dictionary;
    }

}
