/*  Class for a Unix tar archive structure
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2017-05-29: javadoc 1.8
    2014-12-04, Georg Fischer: copied from Cpm
*/
/*
 * Copyright 2014 Dr. Georg Fischer <punctum at punctum dot kom>
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
import  java.io.File;
import  java.io.FileOutputStream;
import  java.util.Iterator;
import  org.apache.log4j.Logger;

/** Common methods for derivatives of the CP/M system (Digital Research)
 *  @author Dr. Georg Fischer
 */
public class Tar extends BaseSystem {
    public final static String CVSID = "@(#) $Id: Tar.java 852 2012-01-06 08:07:08Z gfis $";

    /** whether to write debugging output (iff &gt; 0) */
    protected final static int debug = 0;

    /** log4j logger (category) */
    private Logger log;

    //--------------------------------
    // Construction
    //--------------------------------
    /** Constructor with no arguments, no heavy-weight operations.
     */
    public Tar() {
        log = Logger.getLogger(Tar.class.getName());
        setCode("tar");
        setDescription("Unix tar archive");
    } // Constructor(0)

    /** Initializes the file system structure
     */
    public void initialize() {
        super.initialize();
        setBlockSize(512);
        setDirEntrySize(getBlockSize());
    } // initialize

    //--------------------------
    // Access methods
    //--------------------------

    /** Retrieves the next directory entry
     *  @param withDeleted whether deleted entries should be returned
     *  @return a filled {@link DirectoryEntry}, or null if there
     *  are no more directory entries
     */
    public DirectoryEntry nextDirectoryEntry(boolean withDeleted) {
        DirectoryEntry result = new DirectoryEntry();
        try {
            BaseBuffer container = getContainer();
            int ofs = getDirOffset();
            if (debug > 0) {
                System.err.println("nextDirectoryEntry.ofs = " + String.format("0x%x", ofs)
                        + ", block " + String.format("0x%x", ofs / getBlockSize())
                        );
            }
            if (ofs < container.size()) {
                String fileName = container.getAscii(ofs + 0, 100).trim();
                if (fileName.length() > 0) {
                    result.setBaseFileName(fileName);
                    result.addBlock(getDirOffset() / getBlockSize() + 1); // file contents start at next block
                    // 100,  8 = File mode
                    // 108,  8 = Owner's numeric user ID
                    // 116,  8 = Group's numeric user ID
                    // 124, 12 = File size in bytes (octal)
                    // 136, 12 = Last modification time in numeric Unix time format (octal)
                    // 148,  8 = Checksum for header record
                    // 156,  1 = Link indicator (0 = normal, 1 = hard, 2 = symbolic link; pre-POSIX-1-1988)
                    // 157,100 = nume of linked file
                    result.setFileSize(Integer.parseInt(container.getAscii(ofs + 124, 12).trim(), 8)); // octal
                    result.setDeleted(false);
                    ofs += (1 + (result.getFileSize() + getBlockSize() - 1) / getBlockSize()) * getBlockSize();
                    setDirOffset(ofs);
                } else {
                    result = null; // empty directory filename resp. block
                }
            } else {
                result = null; // not found - behind buffer end
            }
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
            System.exit(1);
        }
        return result;
    } // nextDirectoryEntry

    /** Copy one file into a target directory.
     *  Starting at the first block behind the directory entry,
     *  all bytes of the source file are copied to the target.
     *  @param diren directory entry for the file to be copied
     *  @param path target directory, for example "."
     *  @return target filename
     */
    public String copyFile(DirectoryEntry diren, String path) {
        String sourceFileName = diren.getBaseFileName();
        if (sourceFileName.startsWith("/")) { // remove leading slash
            sourceFileName = sourceFileName.substring(1);
        }
        String targetFileName = path + "/" + sourceFileName;
        int lastSlash = targetFileName.lastIndexOf("/");
        try {
            (new File(targetFileName.substring(0, lastSlash))).mkdirs(); // intermediate directories are also created
            if (! sourceFileName.endsWith("/")) { // not a directory
                int remainingSize = diren.getFileSize();
                BufferedOutputStream byteWriter = new BufferedOutputStream(new FileOutputStream(targetFileName, false));
                Iterator<Integer> blockIterator = diren.getBlockIterator();
                int blockNo = blockIterator.next();
                while (remainingSize > 0) {
                    remainingSize = writeBlock(byteWriter, blockNo, remainingSize);
                    blockNo ++;
                } // while blocks
                byteWriter.close();
            } // not a directory
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
        return targetFileName;
    } // copyFile

} // Tar
