package org.prowl.util.compression.deflate;

import org.checkerframework.checker.units.qual.A;
import org.junit.Assert;
import org.junit.Test;
import org.prowl.kisset.util.compression.deflate.DeflateOutputStream;
import org.prowl.kisset.util.compression.deflate.InflateInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CompressionTest {

    // Test strings, used to benchmark the compression/dictionary updates
    public static final String TEST_STRING1 = "This is too short";
    public static final String TEST_STRING2 = "This somewhat longer string that aims to see if the compression part of the code works, and how well. this will be the first part of the compression test, and will be followed by a second part that will be the decompression test.";
    public static final String TEST_STRING3 = "This is also too short";
    public static final String TEST_STRING4 = "This somewhat longer string that aims to see if the compression part of the code works, and how well. this will be the first part of the compression test, and will be followed by a second part that will be the decompression test.";
    public static final String TEST_STRING5 = "So, after the first test, this is now the last test which should have a decently populated dictionary which should now provide some insight into the compression ability";
    public static final String TEST_STRING6 = "This is well short";
    public static final String TEST_STRING7 = "This somewhat longer string that aims to see if the compression part of the code works, and how well. this will be the first part of the compression test, and will be followed by a second part that will be the decompression test.";

    public static final String ALL_DATA = TEST_STRING1+TEST_STRING2+TEST_STRING3+TEST_STRING4+TEST_STRING5+TEST_STRING6+TEST_STRING7;

    @Test
    public void testCompression() {

        try {

            // Compress a string
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DeflateOutputStream dos = new DeflateOutputStream(bos);
            dos.write(TEST_STRING1.getBytes());
            dos.flush();
            dos.write(TEST_STRING2.getBytes());
            dos.flush();
            dos.write(TEST_STRING3.getBytes());
            dos.flush();
            dos.write(TEST_STRING4.getBytes());
            dos.flush();
            dos.write(TEST_STRING5.getBytes());
            dos.flush();
            dos.write(TEST_STRING6.getBytes());
            dos.flush();
            dos.write(TEST_STRING7.getBytes()); // This should reduce to only 16 bytes as it is repeated 3 times
            dos.flush();


            // Now decompress it and check
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            InflateInputStream iis = new InflateInputStream(bis);
            byte[] data = iis.readAllBytes();
            Assert.assertEquals(ALL_DATA, new String(data));

            // Stats for junkies and improving the compressor/dictionary test
            double compressedSize = bos.size();
            double uncompressedSize = ALL_DATA.length();
            System.out.println("Compressed size (488 current lowest): "+compressedSize);
            System.out.println("Uncompressed size(should be 912): "+uncompressedSize);
            System.out.println("Compression(46.4912%): "+(100-(compressedSize*100/uncompressedSize))+"%");

            // Ensure minimum compression size is met
            Assert.assertTrue( compressedSize <= 488);
            Assert.assertEquals(912, uncompressedSize, 0.0);



        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
