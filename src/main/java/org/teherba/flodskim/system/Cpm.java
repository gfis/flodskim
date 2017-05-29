/*  Class for a CP/M file system structure
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
import  org.apache.log4j.Logger;

/** Common methods for derivatives of the CP/M system (Digital Research)
 *  @author Dr. Georg Fischer
 */
public class Cpm extends BaseSystem {
    public final static String CVSID = "@(#) $Id: Cpm.java 852 2012-01-06 08:07:08Z gfis $";

    /** whether to write debugging output (iff &gt; 0) */
    protected final static int debug = 0;

    /** log4j logger (category) */
    private Logger log;

    //--------------------------------
    // Construction
    //--------------------------------
    /** Constructor with no arguments, no heavy-weight operations.
     */
    public Cpm() {
        log = Logger.getLogger(Cpm.class.getName());
        setCode("cpm");
        setDescription("CP/M (Digital Research)");
    } // Constructor(0)

    /** Initializes the file system structure
     */
    public void initialize() {
        super.initialize();
        setDirEntrySize(32);
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
        boolean found = false;
        try {
            int dirOffset = getDirOffset();
            while (! found && dirOffset < maxDirEntries * getDirEntrySize()) {
                byte[] entry = new byte[getDirEntrySize()];
                System.arraycopy(directory, dirOffset, entry, 0, getDirEntrySize());
                dirOffset += getDirEntrySize();
                setDirOffset(dirOffset);
                if (entry[1] != (byte) 0xe5) { // there is some filename
                    if (entry[0] != ((byte) 0xe5) || withDeleted) { // to be shown
                        found = true;
                        result.setBaseFileName((new String(entry, 1, 8, "UTF-8")).trim());
                        result.setExtension   ((new String(entry, 9, 3, "UTF-8")).trim());
                        result.setDeleted     (entry[0] == (byte) 0xe5);
                        result.setExtentNumber(entry[0xc] | (entry[0xe] << 5));
                        int ientry = 16;
                        while (ientry < getDirEntrySize() && entry[ientry] != 0) {
                            result.addBlock(entry[ientry] & 0xff);
                            ientry ++;
                        } // while ientry
                        result.setFileSize(result.getBlockCount() * getBlockSize());
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

} // Cpm
