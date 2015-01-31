/*  Class for a buffer for some disk image container
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2013-11-05, Georg Fischer: copied from basdetok.BaseBuffer

    Data can currently only be read from a file into a buffer,
    but not written back to a file.
*/
/*
 * Copyright 2013 Dr. Georg Fischer <punctum at punctum dot kom>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teherba.flodskim.buffer;
import  java.io.BufferedInputStream;
import  java.io.File;
import  java.io.FileInputStream;
import  java.io.FileOutputStream;
import  java.io.InputStream;
import  java.io.InputStreamReader;
import  java.io.PrintWriter;
import  java.nio.channels.Channels;
import  java.nio.channels.ReadableByteChannel;
import  java.nio.channels.WritableByteChannel;
import  org.apache.log4j.Logger;

/** Base class for a byte buffer for some disk image container,
 *  defining common properties and methods.
 *  The {@link <a href="http://simonowen.com/samdisk">SAMdisk documentation</a>}
 *  lists a series of possible container formats.
 *  @author Dr. Georg Fischer
 */
public class BaseBuffer {
    public final static String CVSID = "@(#) $Id: BaseBuffer.java 852 2012-01-06 08:07:08Z gfis $";

    /** whether to write debugging output (iff > 0) */
    protected final static int debug = 0;

    /** log4j logger (category) */
    private Logger log;

    //--------------------------------
    // Constructor
    //--------------------------------
    /** Constructor with no arguments, no heavy-weight operations.
     */
    public BaseBuffer() {
        log = Logger.getLogger(BaseBuffer.class.getName());
        setCode("base");
        setDescription("Raw Container File");
    } // Constructor(0)

    /** Initializes the buffer
     */
    public void initialize() {
        allocatedSize = 512; // just some guess, filled by file's size in openFile
        buffer        = new byte[allocatedSize];
        bufferLength  = 0; // will force an immediate read
        bufferPos     = 0;
        filePos       = 0;
        setResultEncoding("UTF-8");
        setMaxCylinder(0); // unknown
        setMaxHead    (0);
        setMaxSector  (0);
        setSectorSize (0);
    } // initialize

    //--------------------------------------------------
    // Buffer for the binary input, and related methods
    //--------------------------------------------------
    /** Internal buffer for the container, will have {@link #allocatedSize} */
    private byte[] buffer;

    /** Allocated length of the buffer (in bytes) */
    protected int allocatedSize;

    /** Filled length of the container buffer as read from a file */
    protected int bufferLength;

    /** current position relative to file start */
    protected int filePos;

    /** Gets the filled container length read from a file
     *  @return number of bytes / chararcters read into the reocrd
     */
    public int size() {
        return bufferLength;
    }  // size

   /** Current read/write pointer (position in the internal {@link #buffer}).
    *  Is incremented by almost all access methods, and is sometimes
    *  returned by such methods.
    */
    protected int bufferPos;

    //-----------------------
    // Other bean properties
    //-----------------------
    /** description of the disk image container */
    private String description;

    /** Sets the description of the disk image container
     *  @param description some short text
     */
    public void setDescription(String description) {
        this.description = description;
    } // setDescription
    /** Gets the description of the disk image container
     *  @return some short text
     */
    public String getDescription() {
        return description;
    } // getDescription

    /** list of codes for the disk image container */
    private String code;

    /** Sets the codes for the disk image container
     *  @param codes list of codes separated by commas
     */
    public void setCode(String codes) {
        this.code = codes;
    } // setCode
    /** Gets the file codes
     *  @return list of codes separated by commas
     */
    public String getCode() {
        return code;
    } // getCode

    /** output file encoding, empty for binary (byte) data */
    private String resultEncoding;

    /** Sets the output file encoding
     *  @param encoding name of the encoding (UTF-8, ISO-8859-1),
     *  or empty for binary data
     */
    public void setResultEncoding(String encoding) {
        resultEncoding = encoding;
    } // setResultEncoding

    /** Gets the output file encoding
     *  @return encoding name of the result encoding (UTF-8, ISO-8859-1),
     *  or empty for binary data
     */
    public String getResultEncoding() {
        return resultEncoding;
    } // getResultEncoding

    //------------------------------
    // Diskette geometry properties
    //------------------------------
    /** maximum cylinder number*/
    private int maxCylinder;

    /** Gets the maximum cylinder number
     *  @return highest cylinder number read into the container
     */
    public int getMaxCylinder() {
        return maxCylinder;
    } // getMaxCylinder

    /** Sets the maximum cylinder number
     *  @param maxCylinder highest cylinder number read into the container, or 0 if unknown
     */
    public void setMaxCylinder(int maxCylinder) {
        this.maxCylinder = maxCylinder;
    } // setMaxCylinder

    /** maximum head     number*/
    private int maxHead    ;

    /** Gets the maximum head number
     *  @return highest head number read into the container
     */
    public int getMaxHead() {
        return maxHead;
    } // getMaxHead

    /** Sets the maximum head number
     *  @param maxHead highest head number read into the container, or 0 if unknown
     */
    public void setMaxHead(int maxHead) {
        this.maxHead = maxHead;
    } // setMaxHead

    /** maximum sector   number*/
    private int maxSector  ;

    /** Gets the maximum sector number
     *  @return highest sector number read into the container
     */
    public int getMaxSector() {
        return maxSector;
    } // getMaxSector

    /** Sets the maximum sector number
     *  @param maxSector highest sector number read into the container, or 0 if unknown
     */
    public void setMaxSector(int maxSector) {
        this.maxSector = maxSector;
    } // setMaxSector

    /** sector size: 128, 256, 512, 1024 ...*/
    private int sectorSize;

    /** Gets the length of (most) sectors
     *  @return sector size, or 0 if unknown
     */
    public int getSectorSize() {
        return sectorSize;
    } // getSectorSize

    /** Sets the length of (most) sectors
     *  @param sectorSize size of a sector, or 0 if unknown
     */
    public void setSectorSize(int sectorSize) {
        this.sectorSize = sectorSize;
    } // setSectorSize

    //-----------------
    // File Processing
    //-----------------
    /** reader for binary files */
    protected InputStream   byteReader;
    /** writer for text   files */
    protected PrintWriter   charWriter;

    /** Opens some named (ordinary) input or output file
     *  @param ifile 0 for source file, 1 for result file
     *  @param fileName name of the (ordinary) file to be opened, or null for STDOUT
     *  @return whether the operation was successful
     */
    public boolean openFile(int ifile, String fileName) {
        boolean result = true;
        try {
            switch (ifile) {
                case 0: // open input  from file
                    { // byte mode
                        if (byteReader != null) {
                            byteReader.close();
                        }
                    /* will not read disk images from STDIN
                        if (fileName == null) {
                            byteReader = new BufferedInputStream(System.in);
                        } else
                    */
                        {
                            File containerFile = new File(fileName);
                            byteReader = new BufferedInputStream(new FileInputStream(containerFile));
                            allocatedSize = (int) containerFile.length();
                            // System.err.println("allocated " + allocatedSize + " for file " + fileName);
                            // allocatedSize += 16384;
                            buffer = new byte[allocatedSize];
                        }
                    } // byte input file
                    break;
                case 1:
                default: // open output into file
                    { // character mode
                        if (fileName == null) { // stdout
                            if (charWriter == null) {
                                charWriter = new PrintWriter(Channels.newWriter(Channels.newChannel(System.out), resultEncoding));
                            } // else leave stdout open, close it with main program
                        } else { // not stdout
                            if (charWriter != null) {
                                charWriter.close();
                            }
                            WritableByteChannel channel = (new FileOutputStream (fileName, false)).getChannel();
                            charWriter = new PrintWriter(Channels.newWriter(channel, resultEncoding));
                        } // not stdout
                    } // character output
                    break;
            } // switch ifile
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
            result = false;
        }
        return result;
    } // openFile

    /** Closes any open input and output files
     */
    public void closeAll() {
        try {
            if (charWriter != null) {
                charWriter.flush();
                charWriter.close();
            }
            if (byteReader != null) {
                byteReader.close();
            }
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
    } // closeAll

    /** Reads a chunk of bytes from an open stream into the container buffer
     *  @param offset buffer position where first byte read should be stored
     *  @param length number of bytes to be read
     *  @return actual number of bytes which were read, or -1 for error
     */
    protected int readChunk(int offset, int length) {
        bufferPos = offset;
        return readChunk(length);
    } // readChunk(2)

    /** Reads a chunk of bytes from an open stream into the container buffer,
     *  starting at the current position
     *  @param length number of bytes to be read
     *  @return actual number of bytes which were read, or -1 for error
     */
    protected int readChunk(int length) {
        int result = 0;
        try {
            result = byteReader.read(buffer, bufferPos, length); // -1 at EOF
            if (result >= 0) {
                filePos   += result;
                bufferPos += result;
            }
            if (bufferPos > bufferLength) {
                bufferLength = bufferPos;
            }
        } catch (Exception exc) {
            System.err.println("bufferPos=" + bufferPos + ", length=" + length);
            log.error(exc.getMessage(), exc);
        }
        return result;
    } // readChunk(1)

    /** Fills the buffer from a disk image container file.
     *  The method should be overwritten by subclasses to implement the
     *  specific container structure.
     *  This is a very simple implementation which reads all bytes
     *  without obeying any structural information ("raw" container).
     *  @param informLevel amount if diagnostic output: 0 = none, 1 = minimal, 2 = medium, 3 = full
     */
    public void readContainer(int informLevel) {
        int readLength = 1;
        int blockSize = 0x4000; // reasonable - 16 kB
        bufferPos = 0;
        filePos   = 0;
        while (readLength > 0 && bufferPos + readLength < allocatedSize) { // -1 = EOF
            readLength = readChunk(blockSize);
        } // while not EOF
        bufferLength = bufferPos;
    } // readContainer

    //---------------------------------
    //  Access to fields in the buffer
    //---------------------------------
    /** Sets the current position in {@link #buffer}
     *  @param newPos the new position
     *  @return previous position
     */
    public int setPosition(int newPos) {
        int result = bufferPos;
        bufferPos = newPos;
        return result;
    } // setPosition

    /** Gets the current position in {@link #buffer}
     *  @return current position
     */
    public int getPosition() {
        return bufferPos;
    } // getPosition

    /** Gets the next byte from the internal {@link #buffer},
     *  starting at the current position, and incrementing the latter
     *  @return that byte
     */
    public byte get1() {
        return buffer[bufferPos ++];
    } // get1

    /** Gets some integer value from the current buffer position, in little endian mode,
     *  starting at the current position, and incrementing the latter
     *  @return a Java integer
     */
    public int getLsb2() {
        bufferPos += 2;
        return (((buffer[bufferPos - 1] & 0xff) << 8) | (buffer[bufferPos - 2] & 0xff)) & 0xffff;
    } // getLsb2

    /** Gets some integer value from a buffer position, in little endian mode,
     *  starting at some position.
     *  @return a Java integer
     */
    public static int getLsb2(byte[] buffer, int bufferPos) {
        bufferPos += 2;
        return (((buffer[bufferPos - 1] & 0xff) << 8) | (buffer[bufferPos - 2] & 0xff)) & 0xffff;
    } // getLsb2

    /** Gets some integer value from the current buffer position, in big endian mode,
     *  starting at the current position, and incrementing the latter
     *  @return a Java integer
     */
    public int getMsb2() {
        bufferPos += 2;
        return (((buffer[bufferPos - 2] & 0xff) << 8) | (buffer[bufferPos - 1] & 0xff)) & 0xffff;
    } // getMsb2

    /** Gets some integer value from a buffer position, in big endian mode,
     *  starting at some position.
     *  @return a Java integer
     */
    public static int getMsb2(byte[] buffer, int bufferPos) {
        bufferPos += 2;
        return (((buffer[bufferPos - 2] & 0xff) << 8) | (buffer[bufferPos - 1] & 0xff)) & 0xffff;
    } // getMsb2

    /** Gets the internal buffer
     *  @return a byte array
     */
    public byte[] getBuffer() {
        return buffer;
    } // getBuffer

    /** Gets some subsegment of the buffer,
     *  starting at the specified position.
     *  The current position is set behind the subsegment.
     *  @param position starting position of the subsegment
     *  @param length number of bytes to be extracted
     *  @return a byte array
     */
    public byte[] getBytes(int position, int length) {
        byte[] result = new byte[length];
        System.arraycopy(buffer, position, result, 0, length);
        bufferPos = position + length;
        return result;
    } // getBytes(2)

    /** Gets some subsegment of the buffer,
     *  starting at the current position, and incrementing the latter
     *  @param length number of bytes to be extracted
     *  @return a byte array
     */
    public byte[] getBytes(int length) {
        return getBytes(bufferPos, length);
    } // getBytes(1)

    /** Gets some subsegment of the buffer,
     *  starting at the specified position.
     *  The current position is set behind the subsegment.
     *  @param position starting position of the subsegment
     *  @param length number of bytes to be extracted
     *  @return a string which is truncated at the first null byte
     */
    public String getAscii(int position, int length) {
        byte[] result = new byte[length];
        System.arraycopy(buffer, position, result, 0, length);
        bufferPos = position + length;
        String sresult = "";
        try {
            sresult = new String(result, "US-ASCII");
        } catch (Exception exc) {
            // ignore
        }
        int nullPos = sresult.indexOf('\0');
        if (nullPos >= 0) { // cut from null byte on
            sresult = sresult.substring(0, nullPos);
        } // cut
        return sresult;
    } // getAscii

    /** Gets some subsegment of the buffer,
     *  starting at the current position, and incrementing the latter
     *  @param length number of bytes to be extracted
     *  @return a string which is truncated at the first null byte
     */
    public String getAscii(int length) {
        return getAscii(bufferPos, length);
    } // getAscii(1)

    /** Dumps a portion of a source array as hexadecimal and ASCII characters.
     *  16 bytes are shown per line, for example:
     <pre>
 69200: 16  6 19 56    cf  2 47   5 bf  7 38  a b2  c 2c  ...V.O.G.?.8.2.,
 69210:  f a4 11 1c 14 94 16  1  19 5c    d1  2 4a  5 c1  .$.......\.Q.J.A
 69220:  7 39  a b4  c 2d  f a7  11 1e 14 96 16  6 19 57  .9.4.-.'.......W
 69230:    cf  2 49  5 c3  7 3b   a b3  c 2d  f a7 11 1f  .O.I.C.;.3.-.'..
     </pre>
     *  In the hex dump part, leading zeroes are suppressed, and zero bytes
     *  are shown as spaces. In the ASCII part, non-printable characters
     *  are replaced by dots. A blank line is inserted before any
     *  offset which is divisible by 0x100.
     *  The output is to the internal Writer which must have been opened.
     *  @param srcBuffer array containing the bytes to be displayed
     *  @param offset buffer position of first byte to be dumped;
     *  it should be a multiple of 0x10, or even of 0x100.
     *  @param length number of bytes to be dumped
     */
    public void dump(byte[] srcBuffer, int offset, int length) {
        StringBuffer line = new StringBuffer(128);
        int CHUNK_LEN = 16;
        byte by1 = 0;
        int last = offset + length;
        boolean first = true;
        try {
            while (offset < last) {
                if (first) {
                    first = false;
                } else {
                    if (offset % 0x100 == 0) {
                        charWriter.println();
                    }
                } // ! first
                line.setLength(0);
                line.append(String.format("%6x:", offset));
                int
                ipos = 0;
                while (ipos < CHUNK_LEN) { // hex bytes
                    by1 = srcBuffer[offset + ipos];
                    if (by1 == 0) {
                        line.append("   ");
                    } else {
                        line.append(String.format(" %2x", by1));
                    }
                    ipos ++;
                } // while ipos hex
                line.append("  "); // separate ASCII part a little bit
                ipos = 0;
                while (ipos < CHUNK_LEN) { // ASCII bytes
                    by1 = srcBuffer[offset + ipos];
                    if (by1 < 0x20 || by1 > 0x7e) {
                        line.append('.');
                    } else {
                        line.append((char) by1);
                    }
                    ipos ++;
                } // while ipos ASCII
                // System.out.println(line.toString());
                charWriter.println(line.toString());
                offset += CHUNK_LEN;
            } // while offset
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
    } // dump(3)

    /** Dumps a portion of the internal buffer as hexadecimal and ASCII characters.
     *  @param offset buffer position of first byte to be dumped;
     *  it should be a multiple of 0x10, or even of 0x100.
     *  @param length number of bytes to be dumped
     */
    public void dump(int offset, int length) {
        dump(buffer, offset, length);
    } // dump(2)

} // BaseBuffer
