/*  Class for a DEC CP/M file system structure on RX50 floppy disks SS DD 80 tracks
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2013-11-07, Georg Fischer: copied from BaseSystem
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
import  org.teherba.flodskim.system.Cpm;
import  org.apache.log4j.Logger;

/** DEC CP/M file system structure on RX50 floppy disks SS DD 80 tracks,
 *  for the DEC Rainbow 100
 *  @author Dr. Georg Fischer
 */
public class Dec_rx50 extends Cpm {
    public final static String CVSID = "@(#) $Id: Dec_rx50.java 852 2012-01-06 08:07:08Z gfis $";

    /** whether to write debugging output (iff > 0) */
    protected final static int debug = 0;

    /** log4j logger (category) */
    private Logger log;

    //--------------------------------
    // Construction
    //--------------------------------
    /** Constructor with no arguments, no heavy-weight operations.
     */
    public Dec_rx50() {
        log = Logger.getLogger(Dec_rx50.class.getName());
        setCode("dec-rx50");
        setDescription("DEC CP/M RX50 (Rainbow 100)");
    } // Constructor(0)

    /** Initializes the file system structure
     */
    public void initialize() {
        super.initialize();
        maxDirEntries = 128;
    } // initialize

    /** Initializes the disk geometry properties
     *  Blocks consist of 4 sectors = 2 kB, with canonical numbering
     *  (block 0 is at the start of the disk).
     */
    protected void setDiskGeometry() {
        minCylinder  = 0;
        maxCylinder  = 79;
        minHead      = 0;
        maxHead      = 0;
        minSector    = 1;
        maxSector    = 10;
        sectorSize   = 512;
        setBlockSize(4 * sectorSize);
        setDirEntrySize(32);
    } // setDiskGeometry

    //--------------------------
    // Access methods
    //--------------------------

    /** Fills the directory by reading blocks from the container buffer
     */
    public void fillDirectory() {
        int dirBlockNo = 2;
        byte[]
        block = getBlock(0);
        directory = new byte[block.length * dirBlockNo]; // blocks 0, 1
        System.arraycopy(block, 0, directory, 0, block.length);
        block = getBlock(1);
        System.arraycopy(block, 0, directory, block.length, block.length);
        maxDirEntries = directory.length / getDirEntrySize();
        setDirOffset(0);
    } // fillDirectory

    /** Mapping of the soft sector interleave
        from /var/pt/disk/PTDECCOM.PAS, for DEC RX50 SS and DS drives
    */
    private static final byte[] SKEW_TAB_50 = new byte[] // rotating interleave over 5 tracks
    //  0, 1, 2, 3, 4, 5, 6, 7, 8, 9 // logical sectors
      { 1, 3, 5, 7, 9, 2, 4, 6, 8,10 // starting in track 2
    //  ##block0##  ##block1##  ##block2 (block 0,1 = directory)
    /*  the following was for RSX-11M, RT11 etc.
      , 3, 5, 7, 9, 1, 4, 6, 8,10, 2
      , 5, 7, 9, 1, 3, 6, 8,10, 2, 4
      , 7, 9, 1, 3, 5, 8,10, 2, 4, 6
      , 9, 1, 3, 5, 7,10, 2, 4, 6, 8
    */
      };

    /** Get a logical block from the file system.
     *  Block 0 is at the start of the disk.
     *  @param blockNo number of the block
     *  @return array of bytes with the content of the block
     */
    public byte[] getBlock(int blockNo) {
        int headNo      = maxHead   - minHead   + 1;
        int sectNo      = maxSector - minSector + 1; // per track
        int trackSize   = sectorSize * sectNo * headNo;
        int sectCount   = getBlockSize() / sectorSize; // per block; sectors still to be processed
        int logSect     = blockNo * sectCount; // logical sector number, start of disk = 0
        byte[] result   = new byte[getBlockSize()];
        int destPos     = 0; // destination position in result
        while (sectCount > 0) {
            int track    = 2 + logSect / sectNo;
            int physSect = SKEW_TAB_50[logSect % SKEW_TAB_50.length];
            int srcPos   = track * trackSize + (physSect - 1) * sectorSize;
            if (debug > 0) {
                System.err.println(""
                        + ", blockNo="  + blockNo
                        + ", logSect="  + logSect
                        + ", track="    + track
                        + ", physSect=" + physSect
                        + ", srcPos="   + String.format("0x%x", srcPos)
                        + ", destPos="  + String.format("0x%x", destPos)
                        );
            }
            System.arraycopy(getContainer().getBuffer(), srcPos, result, destPos, sectorSize);
            destPos += sectorSize;
            logSect ++;
            sectCount --;
        } // while sectCount
        return result;
    } // getBlock

} // Dec_rx50
