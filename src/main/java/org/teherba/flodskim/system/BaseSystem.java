/*  Class for a file system structure in a buffer for a disk image container
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2013-11-07, Georg Fischer: copied from BaseBuffer

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
package org.teherba.flodskim.system;
import  org.teherba.flodskim.buffer.BaseBuffer;
import  org.teherba.flodskim.system.DirectoryEntry;
import  java.io.BufferedOutputStream;
import  java.io.FileOutputStream;
import  java.util.Iterator;
import  org.apache.log4j.Logger;

/** Base class for a file system structure stored in a {@link org.teherba.flodskim.buffer.BaseBuffer BaseBuffer}
 *  defining common properties and methods.
 *  @author Dr. Georg Fischer
 */
public class BaseSystem {
    public final static String CVSID = "@(#) $Id: BaseSystem.java 852 2012-01-06 08:07:08Z gfis $";

    /** whether to write debugging output (iff > 0) */
    protected final static int debug = 0;

    /** log4j logger (category) */
    private Logger log;

    //--------------------------------
    // Construction
    //--------------------------------
    /** Constructor with no arguments, no heavy-weight operations.
     */
    public BaseSystem() {
        log = Logger.getLogger(BaseSystem.class.getName());
        setCode("base");
        setDescription("undefined filesystem");
    } // Constructor(0)

    /** Initializes the file system structure
     */
    public void initialize() {
        setDiskGeometry();
        initCharTable();
    } // initialize

    //--------------------------
    // General bean properties
    //--------------------------
    /** code for the file system */
    private String code;

    /** Sets the code for the file system
     *  @param code a short, lowercase word
     */
    public void setCode(String code) {
        this.code = code;
    } // setCode

    /** Gets the file system code
     *  @return short, lowercase word
     */
    public String getCode() {
        return code;
    } // getCode

    /** description of the file system */
    private String description;
    /** Sets the description of the file system
     *  @param description some short text
     */
    public void setDescription(String description) {
        this.description = description;
    } // setDescription

    /** Gets the description of the file system
     *  @return some short text
     */
    public String getDescription() {
        return description;
    } // getDescription

    //--------------------------
    // Specific bean properties
    //--------------------------
    /** buffer for the disk image container */
    private BaseBuffer container;

    /** Sets the instance for the disk image container
     *  @param container a subclass of {@link org.teherba.flodskim.buffer.BaseBuffer BaseBuffer}
     */
    public void setContainer(BaseBuffer container) {
        this.container = container;
    } // setContainer

    /** Gets the instance for the disk image container
     *  @return a subclass of {@link org.teherba.flodskim.buffer.BaseBuffer BaseBuffer}
     */
    public BaseBuffer getContainer() {
        return container;
    } // getContainer

    /** size of a directory entry (in bytes) */
    private int dirEntrySize;

    /** Sets the size of a directory entry
     *  @param dirEntrySize number of bytes per directory entry
     */
    public void setDirEntrySize(int dirEntrySize) {
        this.dirEntrySize = dirEntrySize;
    } // setDirEntrySize

    /** Gets the size of a directory entry
     *  @return number of bytes per directory entry
     */
    public int getDirEntrySize() {
        return dirEntrySize;
    } // getDirEntrySize

    /** number of possible entries in one directory */
    protected int maxDirEntries;

    /** offset of current directory entry relative to start of {@link #directory} */
    private int dirOffset;

    /** Sets the offset of the current directory entry
     *  @param dirOffset offset of directory entry relative to start of directory
     */
    public void setDirOffset(int dirOffset) {
        this.dirOffset = dirOffset;
    } // setDirOffset

    /** Gets the offset of the current directory entry
     *  @return offset of directory entry relative to start of directory
     */
    public int getDirOffset() {
        return dirOffset;
    } // getDirOffset

    /** block where the {@link #directory} starts */
    private int dirStartBlock;

    /** Sets the block where the directory starts
     *  @param dirStartBlock a (low) block number
     */
    public void setDirStartBlock(int dirStartBlock) {
        this.dirStartBlock = dirStartBlock;
    } // setDirStartBlock

    /** Gets the block where the directory starts
     *  @return a (low) block number
     */
    public int getDirStartBlock() {
        return dirStartBlock;
    } // getDirStartBlock

    /** buffer for a directory (linear byte array with raw directory entries) */
    protected byte[] directory;

    //------------------------------
    // Diskette geometry properties
    //------------------------------
    /** minimum cylinder number*/
    protected int minCylinder;
    /** maximum cylinder number*/
    protected int maxCylinder;
    /** minimum head     number*/
    protected int minHead    ;
    /** maximum head     number*/
    protected int maxHead    ;
    /** minimum sector   number*/
    protected int minSector  ;
    /** maximum sector   number*/
    protected int maxSector  ;
    /** sector size: 128, 256, 512, 1024 ...*/
    protected int sectorSize;

    /** logical block size, multiple of {@link #sectorSize}  */
    private int blockSize;

    /** Sets the size of a logical block
     *  @param blockSize 256, 512, 4096 or similiar
     */
    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    } // setBlockSize

    /** Gets the size of a logical block
     *  @return 256, 512, 4096 or similiar
     */
    public int getBlockSize() {
        return blockSize;
    } // getBlockSize

    /** Initializes the disk geometry properties
     */
    protected void setDiskGeometry() {
        minCylinder  = 0;
        maxCylinder  = 79;
        minHead      = 0;
        maxHead      = 1;
        minSector    = 1;
        maxSector    = 9;
        sectorSize   = 512;
        blockSize    = 4 * sectorSize;
        setDirEntrySize(32);
        setDirStartBlock(0);
    } // setDiskGeometry

    //--------------------------------------
    // Single byte to character translation
    //--------------------------------------
    /** Table which maps system bytes to Java characters */
    protected char[] charTable;

    /** Initializes the character table
     */
    protected void initCharTable() {
        charTable = new char[256];
        int ichar = 0;
        while (ichar < 256) {
            charTable[ichar] = (char) ichar;
            ichar ++;
        } // while ichar
    } // initCharTable

    /** Gets a translated string from a byte subarray
     *  @param buffer byte array
     *  @param start first byte position
     *  @param len number of bytes to be translated
     *  @return translated string
     */
    public String translate(byte[] buffer, int start, int len) {
        StringBuffer result = new StringBuffer(len);
        int pos = start;
        while (len > 0) {
            int ch = buffer[pos] & 0xff;
            result.append(charTable[ch]);
            len --;
            pos ++;
        } // while len
        return result.toString();
    } // translate

    // Directory Access
    //------------------------------
    /** Fills the directory by reading blocks from the container buffer
     */
    public void fillDirectory() {
    } // fillDirectory

    /** Retrieves the next directory entry
     *  @param withDeleted whether deleted entries should be returned
     *  @return a filled {@link DirectoryEntry}, or null if there
     *  are no more directory entries
     */
    public DirectoryEntry nextDirectoryEntry(boolean withDeleted) {
        return null;
    } // nextDirectoryEntry

    /** Prints a directory listing
     */
    public void printDirectory() {
        boolean busy = true;
        fillDirectory();
        while (busy) {
            DirectoryEntry diren = nextDirectoryEntry(true);
            if (diren != null) {
                System.out.println(diren.toString());
            } else {
                busy = false;
            }
        } // while busy
    } // printDirector

    /** Copy all files into a target directory
     *  @param path target directory, for example "."
     */
    public void copyFiles(String path) {
        boolean busy = true;
        fillDirectory();
        while (busy) {
            DirectoryEntry diren = nextDirectoryEntry(false);
            if (diren != null) {
                String targetFileName = copyFile(diren, path);
                System.out.println("\'" + diren.getBaseFileName() +  "\' -> \'" + targetFileName + "\'");
            } else {
                busy = false;
            }
        } // while busy
    } // copyFiles

    /** Copy one file into a target directory.
     *  This is used for {@link Cpm} and similiar file systems.
     *  @param diren directory entry for the file to be copied
     *  @param path target directory, for example "."
     *  @return target filename
     */
    public String copyFile(DirectoryEntry diren, String path) {
        String targetFileName = path + "/" + diren.getBaseFileName();
        if (diren.getExtension().length() > 0) {
            targetFileName += "." + diren.getExtension();
        }
        if (diren.getExtentNumber() > 1) { // kind of stupid: separate files for higher extent numbers
            targetFileName += "." + diren.getExtentNumber();
        }
        int remainingSize = diren.getFileSize();
        try {
            BufferedOutputStream byteWriter = new BufferedOutputStream(new FileOutputStream(targetFileName, false));
            Iterator<Integer> blockIterator = diren.getBlockIterator();
            while (blockIterator.hasNext()) {
                int blockNo = blockIterator.next();
                remainingSize = writeBlock(byteWriter, blockNo, remainingSize);
            } // while blocks
            byteWriter.close();
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
        return targetFileName;
    } // copyFile

    /** Write one block to the target file
     *  @param byteWriter open writer for the target file
     *  @param blockNo number of block to be written
     *  @return new remaining size to be written
     */
    public int writeBlock(BufferedOutputStream byteWriter, int blockNo, int remainingSize) {
        int result = remainingSize;
        byte[] block = getBlock(blockNo);
        try {
            int len = block.length;
            if (remainingSize > 0 && remainingSize < len) {
                len = remainingSize;
            }
            byteWriter.write(block, 0, len);
            result -= block.length;
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
        return result;
    } // writeBlock

   //--------------------------
    // Access methods
    //--------------------------

    /** Dumps a portion of a source array as hexadecimal and ASCII characters.
     *  Convenience method, see the description of {@link BaseBuffer#dump}.
     *  @param srcBuffer array containing the bytes to be displayed
     *  @param offset buffer position of first byte to be dumped;
     *  it should be a multiple of 0x10, or even of 0x100.
     *  @param length number of bytes to be dumped
     */
    public void dump(byte[] srcBuffer, int offset, int length) {
        getContainer().dump(srcBuffer, offset, length);
    } // dump

    /** Get a logical block from the file system.
     *  This implementation assumes a very simple, linear structure of blocks.
     *  Block 0 is at the start of the disk.
     *  @param blockNo number of the block
     *  @return array of bytes with the content of the block
     */
    public byte[] getBlock(int blockNo) {
        int sectSize  = blockSize;
        byte[] result = new byte[blockSize];
        int destPos   = 0; // destination position in result
        int sectCount = 1;
        while (sectCount > 0) {
            int srcPos   = blockNo * blockSize;
            System.arraycopy(getContainer().getBuffer(), srcPos, result, destPos, sectSize);
            destPos += sectSize;
            sectCount --;
        } // while sectCount
        return result;
    } // getBlock

} // BaseSystem
