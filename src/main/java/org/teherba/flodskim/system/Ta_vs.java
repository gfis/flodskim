/*  Class for the file system structure of Triumph-Adler VS20 and BSM100
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2017-05-29: javadoc 1.8
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
import  org.teherba.flodskim.buffer.BaseBuffer;
import  org.teherba.flodskim.system.BaseSystem;
import  org.teherba.flodskim.system.DirectoryEntry;
import  java.io.BufferedOutputStream;
import  org.apache.logging.log4j.Logger;
import  org.apache.logging.log4j.LogManager;

/** Common methods for file system structure of Triumph-Adler VS20 and BSM100,
 *  see <a href="https://de.wikipedia.org/wiki/Triumph-Adler#Bildschirmschreibsysteme">Wikipedia (de)</a>
 *  @author Dr. Georg Fischer
 */
public class Ta_vs extends BaseSystem {
    public final static String CVSID = "@(#) $Id: Ta_vs.java 852 2012-01-06 08:07:08Z gfis $";

    /** whether to write debugging output (iff &gt; 0) */
    protected final static int debug = 0;

    /** log4j logger (category) */
    private Logger log;

    //--------------------------------
    // Construction
    //--------------------------------
    /** Constructor with no arguments, no heavy-weight operations.
     */
    public Ta_vs() {
        log = LogManager.getLogger(Ta_vs.class.getName());
        setCode("ta-vs");
        setDescription("TA VS20, BSM100");
    } // Constructor(0)

    /** Initializes the file system structure
     */
    public void initialize() {
        super.initialize();
    } // initialize

    /** Initializes the character table
     */
    protected void initCharTable() {
        super.initCharTable();
        charTable[0x80] = 'Ä'; // Ae
        charTable[0x81] = 'Ö'; // Oe
        charTable[0x82] = 'Ü'; // Ue
        charTable[0x85] = '-';
        charTable[0x90] = '\''; // apostrophe
        charTable[0x92] = '°'; // degree
        charTable[0x93] = '\u0015'; // German paragraph
        charTable[0x94] = 'ß'; // sz
        charTable[0x96] = '²'; // upper 2
        charTable[0x97] = 'µ'; // my
        charTable[0x98] = '£'; // pound sterling
        charTable[0x9a] = 'á'; // a acute
        charTable[0x9b] = 'à'; // a grave
        charTable[0x9d] = 'é'; // e acute
        charTable[0x9e] = 'è'; // e grave
        charTable[0xa0] = 'ä'; // ae
        charTable[0xa1] = 'ö'; // oe
        charTable[0xa2] = 'ü'; // ue
        charTable[0xa3] = 'Ý'; // Y acute
        charTable[0xa9] = 'ç'; // c cedille
        charTable[0xbf] = '½'; // 1/2
        charTable[0xc1] = '¼'; // 1/4
        charTable[0xc8] = '@'; // diamond, VS20: not filled, also (R) sign
        /* BSM100 */
        charTable[0x01] = 'Ü'; // Ue
        charTable[0x04] = 'ß'; // sz
        charTable[0x12] = 'ä'; // ae
        charTable[0x13] = 'ö'; // oe
        charTable[0x14] = 'ü'; // ue
        charTable[0x7e] = '\u0015'; // German paragraph
        charTable[0x03] = 'Ä'; // Ae
        charTable[0x80] = 'Ä'; // Ae
        charTable[0x81] = 'Ö'; // Oe
        charTable[0x90] = '\'';
    } // initCharTable

    /** Initializes the disk geometry properties
     *  Blocks consist of 4 sectors = 2 kB, with canonical numbering
     *  (block 0 is at the start of the disk).
     */
    protected void setDiskGeometry() {
        minCylinder  = 0;
        maxCylinder  = 79;
        minHead      = 0;
        maxHead      = 1;
        minSector    = 0;
        maxSector    = 8;
        sectorSize   = 512;
        setBlockSize(8 * sectorSize);
        setDirEntrySize(32);
    } // setDiskGeometry

    //--------------------------
    // Access methods
    //--------------------------

    /** Get a logical block from the file system.
     *  @param blockNo2 number of the block, always even, must
     *  be divided by 2 to yield a canonical blockNo fitted for the blocksize
     *  @return array of bytes with the content of the block
     */
    public byte[] getBlock(int blockNo2) {
        int blockNo = blockNo2 / 2;
        int sectCount = 1; // blockSize / sectSize; // per block; sectors still to be processed
        byte[] result = new byte[getBlockSize()];
        int destPos = 0; // destination position in result
        while (sectCount > 0) {
            int srcPos   = blockNo * getBlockSize();
            if (debug > 0) {
                System.err.println(""
                        + ", blockNo2=" + String.format("0x%x", blockNo2)
                        + ", srcPos="   + String.format("0x%x", srcPos)
                        + ", destPos="  + String.format("0x%x", destPos)
                        );
            }
            System.arraycopy(getContainer().getBuffer(), srcPos, result, destPos, getBlockSize());
            destPos += sectorSize;
            sectCount --;
        } // while sectCount
        return result;
    } // getBlock

    /** Offset of file allocation table in {@link #directory} */
    protected int fatOffset;

    /** Tests whether the "signature" is found at some offset.
     *  @param offset offset in {@link #directory}
     *  @return true if a "signature" of 4 bytes (02 00 01 20) is found
     *  at this offset, false otherwise
     */
    private boolean testSig(int offset) {
        return directory[offset + 0] == 0x02
            && directory[offset + 1] == 0x00
            && directory[offset + 2] == 0x01
            && directory[offset + 3] == 0x20
            ;
    } // testSig

    /** Fills the directory by reading blocks from the container buffer.
     *  The FAT starts with a "signature" of 4 bytes: 02 00 01 20.
     *  There are 3 FAT layout variants:
     *  <ol>
     *  <li>signature at +0x202 and +0x01e: start at +0x202</li>
     *  <li>signature at +0x402 and +0x01e: exchange 200-3ff with 400-5ff, then start at +0x202</li>
     *  <li>signature only at +0x002: start at +0x002</li>
     *  </ol>
     */
    public void fillDirectory() {
        byte[] block = null;
        int dirBlockNo = 2;
        directory = new byte[getBlockSize() * dirBlockNo]; // blocks 0(1), 2(3)
        int blockNo = 0;
        while (blockNo < dirBlockNo) {
            block = getBlock(blockNo * 2);
            System.arraycopy(block, 0, directory, blockNo * getBlockSize(), getBlockSize());
            blockNo ++;
        } // while blockNo
        maxDirEntries = directory.length / getDirEntrySize();
        fatOffset = 0x202; // 2nd sector, 2nd word
        setDirOffset(0x400); // 2 * sectorSize;
        if (false) {
        } else if (testSig(0x202) && testSig(0x01e)) {
            fatOffset = 0x202; // 2nd sector, 2nd word
        } else if (testSig(0x202) && testSig(0x01e)) {
            block = getBlock(0);
            System.arraycopy(block, 0x400, directory, 0x200, 0x200);
            System.arraycopy(block, 0x200, directory, 0x400, 0x200);
            fatOffset = 0x202; // 2nd sector, 2nd word
        } else if (testSig(0x002)                  ) {
            fatOffset = 0x002;
        } else {
            log.error("cannot find signature");
            System.exit(1);
        }
    } // fillDirectory

    /** Retrieves the next directory entry which has the following fields:
     *  <ul>
     *  <li>+0x00: Filename (16 bytes in TA VS character set)</li>
     *  <li>+0x10: "A" for text, "I", "C" for database, "Z" with BSM100</li>
     *  <li>+0x11: Starting blockNo2 (LSB2)</li>
     *  <li>+0x13: 0 for deleted file, valid file otherwise</li>
     *  <li>+0x14: 3 unknown bytes</li>
     *  </ul>
     *  For example:
     <pre>
   400:                                                  ................
   410:           3  7  8 89                             ................
   420: 41 64 72 2e 47 65 62 69 65 74 73 67 65 2e 20 20  Adr.Gebietsge.
   430: 5a 1c     1  7  8 89                             Z...............
   440: 44 6f 6c 6d 65 74 73 63 68 65 72 20 20 20 20 20  Dolmetscher
   450: 5a 26     1  7  8 89                             Z&amp;..............
   460: 4c 69 73 74 65 20 57 65 63 68 73 65 6c 20 47 65  Liste Wechsel Ge
   470: 5a 2c        7  8 89                             Z,..............
   480:                                                  ................
   490:                                                  ................
     </pre>
     *  @param withDeleted whether deleted entries should be returned
     *  @return a filled {@link DirectoryEntry}, or null if there
     *  are no more directory entries.
     */
    public DirectoryEntry nextDirectoryEntry(boolean withDeleted) {
        DirectoryEntry result = new DirectoryEntry();
        boolean found = false;
        try {
            int dirOffset = getDirOffset();
            while (! found && dirOffset < maxDirEntries * getDirEntrySize()) {
                byte[] entry = new byte[getDirEntrySize()];
                System.arraycopy(directory, dirOffset, entry, 0, getDirEntrySize());
                dirOffset += getDirEntrySize();
                setDirOffset(dirOffset);
                if (entry[0] != 0) { // there is some filename
                    if (entry[0x13] != 0 || withDeleted) { // to be shown
                        found = true;
                        result.setBaseFileName(translate(entry, 0, 15).trim().replaceAll("[^A-Za-z0-9ÄÖÜäöüß.]", "_"));
                        result.setExtension   (translate(entry, 16, 1).trim());
                        result.setDeleted     (entry[0x13] == 0 );
                        result.setExtentNumber(0);
                        int blockNo2 = BaseBuffer.getLsb2(entry, 17); // starting block
                        result.addBlock(blockNo2);
                        // blockNo /= 2;
                        int fileSize = 0;
                        if (true || ! result.isDeleted()) {
                            fileSize += followFat(blockNo2,  result  ); // now consult the FAT
                        } // not deleted
                        result.setFileSize(fileSize);
                    } // if to be shown
                } // some filename
            } // while ! found
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
        if (! found) {
            result = null;
        }
        return result;
    } // nextDirectoryEntry

    /** Retrieves the following block numbers and the byte length in the last
     *  block from the file allocation table starting at offset 0x202.
     *  For each blockNo2 the FAT contains a value which is:
     *  <ol>
     *  <li>even - the following blockNo2 for the file</li>
     *  <li>odd - the number of bytes remaining in the last block, minus 1 div 2</li>
     *  </ol>
     *  For example:
     <pre>
   200: ca  7  2     1 20 33 ff  8    83  c d7 ff a5 ff  ..... 3.........
   210: 71 ff 12 ff ab ff 16 ff 18 ff 1a ff  f ff 1e     q...............
   220: 20    22    d5 1d 63  9 8b  3 83 ff df  f 2e ff   ."...c.........
   230: 30 ff bd ff 34 ff 36 ff 81 ff 6b ff 3c ff 3e ff  0...4.6...k.&lt;.&gt;.
   240: fb ff 42 ff 44 ff  f ff 48 ff 49 ff 37 ff 41 ff  ..B.D...H.I.7.A.
   250: 7f ff 7d ff 7f 17 ff ff ff ff ff ff ff ff ff ff  ..}.............
   260: ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff  ................
     </pre>
     *  If the high byte of the value is 0xff, the file is deleted (BSM100).
     *  @param blockNo2 logical block number of first block
     *  @param result the directory entry to be filled (its block list is modified!)
     *  @return number of total bytes in the file
     */
    private int followFat(int blockNo2,  DirectoryEntry result  ) {
        boolean busy = true;
        int loopLimit = 32;
        int fileSize = 0;
        while (busy && loopLimit > 0) {
            loopLimit --;
            int offset = fatOffset + blockNo2;
            if (offset < 0x400) {
                int fatValue = BaseBuffer.getLsb2(directory, offset);
                if (debug > 0) {
                    System.out.println("\t\t\t"
                            + "blockNo2="   + String.format("%02x", blockNo2)
                            + ", offset="   + String.format("%04x", offset)
                            + ", fatValue=" + String.format("%04x", fatValue)
                            );
                }
                if (fatValue % 2 == 0) { // even, next block number
                    fileSize += getBlockSize();
                    blockNo2 = fatValue;
                    if (blockNo2 >= 0xff00) { // deleted block, for BSM100
                        blockNo2 &= 0x00ff;
                    }
                    result.addBlock(blockNo2);
                } else { // odd, number of bytes in last block
                    busy = false;
                    fileSize += (fatValue - 1) / 2;
                }
            } else {
                busy = false;
                System.err.println("\t\t\tinvalid offset for "
                            + "blockNo2="   + String.format("%02x", blockNo2)
                            + ", offset="   + String.format("%04x", offset)
                            );
            }
        } // while busy
        return fileSize;
    } // followFat

} // Ta_vs
