/*  Class for a Sinix tar file system structure on MX-2 floppy disks
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2017-05-29: javadoc 1.8
    2014-12-04, Georg Fischer: copied from Dex_rx50
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
import  org.teherba.flodskim.system.Tar;
import  org.apache.log4j.Logger;

/** Sinix tar archive structure on MX-2 floppy disks DS DD 80 tracks
 *  @author Dr. Georg Fischer
 */
public class Sinix_mx2 extends Tar {
    public final static String CVSID = "@(#) $Id: Sinix_mx2.java 852 2012-01-06 08:07:08Z gfis $";

    /** whether to write debugging output (iff &gt; 0) */
    protected final static int debug = 0;

    /** log4j logger (category) */
    private Logger log;

    //--------------------------------
    // Construction
    //--------------------------------
    /** Constructor with no arguments, no heavy-weight operations.
     */
    public Sinix_mx2() {
        log = Logger.getLogger(Sinix_mx2.class.getName());
        setCode("sinix-mx2");
        setDescription("Sinix tar MX-2");
    } // Constructor(0)

    /** Initializes the file system structure
     */
    public void initialize() {
        super.initialize();
        maxDirEntries = 4096;
    } // initialize

    /** Initializes the disk geometry properties
     *  Blocks are not really relevant for tar.
     */
    protected void setDiskGeometry() {
        minCylinder  = 0;
        maxCylinder  = 79;
        minHead      = 0;
        maxHead      = 1;
        minSector    = 1;
        maxSector    = 16;
        sectorSize   = 256;
        setBlockSize    (512);
        setDirEntrySize (512);
        setDirStartBlock(0x38);
        if (debug > 1) {
            System.err.println("Sinix_mx2.setDiskGeometry, dirStartBlock = " + String.format("0x%x", getDirStartBlock()));
        }
    } // setDiskGeometry

    //--------------------------
    // Access methods
    //--------------------------

    /** Fills the directory by reading blocks from the container buffer
     */
    public void fillDirectory() {
        setDirOffset(getDirStartBlock() * getBlockSize());
        if (debug > 0) {
            System.err.println("Sinix_mx2.fillDirectory, dirOffset = " + String.format("0x%x", getDirOffset()));
        }
    } // fillDirectory

} // Sinix_mx2
