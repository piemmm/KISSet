package org.prowl.kisset.util.compression.dictblock;


/**
 * Output stream that compresses data. A compressed block
 * is generated and transmitted once a given number of bytes
 * have been written, or when the flush method is invoked.
 *
 * Copyright 2005 - Philip Isenhour - http://javatechniques.com/
 *
 * This software is provided 'as-is', without any express or
 * implied warranty. In no event will the authors be held liable
 * for any damages arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any
 * purpose, including commercial applications, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you
 *     must not claim that you wrote the original software. If you
 *     use this software in a product, an acknowledgment in the
 *     product documentation would be appreciated but is not required.
 *
 *  2. Altered source versions must be plainly marked as such, and
 *     must not be misrepresented as being the original software.
 *
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 *
 * $Id: CompressedBlockOutputStream.java,v 1.1 2015/06/16 20:23:42 ihawkins Exp $
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

public class CompressedBlockOutputStream extends FilterOutputStream {

   private static final Log LOG = LogFactory.getLog("CompressedBlockOutputStream");
   RollingDictionary dict = new RollingDictionary();
   
   /**
    * Buffer for input data
    */
   private byte[] inBuf = null;

   /**
    * Buffer for compressed data to be written
    */
   private byte[] outBuf = null;

   /**
    * Number of bytes in the buffer
    */
   private volatile int len = 0;

   /**
    * Deflater for compressing data
    */
   private Deflater deflater = null;

   private static final int MAXCACHE = 200;
   private static final int MAXCACHESMALL = 700;
   
   /**
    * Constructs a CompressedBlockOutputStream that writes to
    * the given underlying output stream 'os' and sends a compressed
    * block once 'size' byte have been written. The default
    * compression strategy and level are used.
    */
   public CompressedBlockOutputStream(OutputStream os, int size)
         throws IOException {
      this(os, size,
            Deflater.BEST_COMPRESSION, Deflater.DEFAULT_STRATEGY);
   }

   /**
    * Constructs a CompressedBlockOutputStream that writes to the
    * given underlying output stream 'os' and sends a compressed
    * block once 'size' byte have been written. The compression
    * level and strategy should be specified using the constants
    * defined in java.util.zip.Deflator.
    */
   public CompressedBlockOutputStream(OutputStream os, int size,
         int level, int strategy) throws IOException {
      super(os);
      //rollingDictionary.addwrite("WinterSun".getBytes());
      this.inBuf = new byte[size];
      this.outBuf = new byte[size + 64];
      this.deflater = new Deflater(level);
      this.deflater.setStrategy(strategy);
      //  this.deflater.setStrategy(Deflater.DEFLATED);
      this.deflater.setLevel(Deflater.BEST_COMPRESSION);
     // this.deflater.setDictionary(ClientHandler.DICTIONARY.getBytes());
      this.deflater.setDictionary(dict.getDictionary());
      this.deflater.reset();

   }


   protected void compressAndSend(int leng) throws IOException {
      //new RuntimeException(new String(inBuf,0,len)).printStackTrace();
      if (len > 0) {

         deflater.setInput(inBuf, 0, len);
         deflater.finish();
         
         deflater.setDictionary(dict.getDictionary());
         int size = deflater.deflate(outBuf);

         if (size == 0 || len < 10 || size+4 > len) {
            // Uncompressed
            out.write((len >>  8) & 0xFF);
            out.write((len >>  0) & 0xFF);
            out.write(0);
            out.write(0);
        //       System.out.print("u");

            out.write(inBuf, 0, len);
            //  System.out.println("USize:"+len+"   for:'"+new String(inBuf,0,len)+"'");
         } else{
            // Write the size of the compressed data, followed
            // by the size of the uncompressed data
            //compressed
            out.write((size >>  8) & 0xFF);
            out.write((size >>  0) & 0xFF);

            out.write((len >>  8) & 0xFF);
            out.write((len >>  0) & 0xFF);
            //System.out.print("c");


            //System.out.println("Csize:" + size + "  oSize:"+len+"   for:'"+new String(inBuf,0,len)+"'");
            out.write(outBuf, 0, size);

         }
         out.flush();

         deflater.reset();
 
      }
      dict.addToDictionary(inBuf, 0, len);
      len = 0;

   }

   public void write(int b) throws IOException {

      LOG.debug("Write:" + b);
      if (len > inBuf.length) {
         len = 0; // Comms error, reset.
         //  java.lang.ArrayIndexOutOfBoundsException: 279466
         //          at org.prowl.wintersun.server.communications.CompressedBlockOutputStream.write(CompressedBlockOutputStream.java:248)
         //          at java.io.DataOutputStream.writeShort(DataOutputStream.java:167)
         //          at org.prowl.wintersun.server.communications.ClientHandler.sendQueue(ClientHandler.java:5081)
         //          at org.prowl.wintersun.server.communications.ClientHandler$3.run(ClientHandler.java:657)
         //          at java.lang.Thread.run(Thread.java:745)
      }
      inBuf[len++] = (byte) b;
      if (len == inBuf.length) {
         compressAndSend(inBuf.length);
      }
   }

   public void write(byte[] b, int boff, int blen)
         throws IOException {
      while ((len + blen) > inBuf.length) {
         int toCopy = inBuf.length - len;
         System.arraycopy(b, boff, inBuf, len, toCopy);
         len += toCopy;
         compressAndSend(len);
         boff += toCopy;
         blen -= toCopy;
      }
      System.arraycopy(b, boff, inBuf, len, blen);
      len += blen;
   }

   public void flush() throws IOException {
      compressAndSend(len);
      out.flush();
   }

   public void close() throws IOException {
      compressAndSend(len);
      out.close();
   }
}