/*  Class for a buffer for the (e)DSK disk image container format
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2013-11-05, Georg Fischer

    c.f. http://web.archive.org/web/20090107021455/http://www.kjthacker.f2s.com/docs/dsk.html
    and http://simonowen.com/samdisk/formats/
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
import  org.teherba.flodskim.buffer.BaseBuffer;
import  org.apache.log4j.Logger;

/** Base class for a byte buffer for the DSK disk image container
 *  defining common properties and methods. The container format
 *  is described in an
 *  {@link <a href="http://web.archive.org/web/20090107021455/http://www.kjthacker.f2s.com/docs/dsk.html">outdated
 *  document for CPCEMU</a>}.
 *  @author Dr. Georg Fischer
 */
public class DskBuffer extends BaseBuffer {
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
    public DskBuffer() {
        super();
        log = Logger.getLogger(DskBuffer.class.getName());
        setCode("dsk");
        setDescription("(Extendend) Disk Image");
    } // Constructor(0)

    /** Initializes the buffer
     */
    public void initialize() {
        super.initialize();
    } // initialize

    /** Fills the buffer from a disk image container file.
     *  @param informLevel amount if diagnostic output: 0 = none, 1 = minimal, 2 = medium, 3 = full
     */
    public void readContainer(int informLevel) {
        int blockSize = 0x100;
        // read disk information block
        setPosition(0);
        readChunk(blockSize);
        setPosition(0x30);
        int trackNo = get1();
        setMaxCylinder(trackNo);
        int headNo  = get1();
        setMaxHead(headNo);
        int trackLen = getLsb2(); // not filled by SAMdisk?
        if (informLevel >= 1) { // minimal: Disc Information Block
            charWriter.println(getAscii(0, 0x30)); // format descriptor and creator
            charWriter.println(trackNo + " tracks, " + headNo + " heads");
        } // informLevel >= 0
        setPosition(0); // overwrite disk information block

        // read all tracks
        int itrack = 0;
        while (itrack < trackNo) {
            int ihead = 0;
            while (ihead < headNo) {
                // first read the track's information block (tib)
                int tib0 = getPosition();
                readChunk(blockSize);
                setPosition(tib0 + 0x10);
                int tibTrack = get1();
                if (tibTrack == itrack) {
                    int tibHead  = get1();
                    if (tibHead  != ihead ) {
                        log.error("wrong head# "  + tibHead  + " for track " + itrack + ", head " + ihead);
                    }
                    int dummy = getLsb2();
                    int sectSize = 128 << get1(); // 0 = 128, 1= 256, 2 = 512 ...
                    setSectorSize(sectSize);
                    int sectNo   = get1();
                    setMaxSector(sectNo);
                    if (informLevel >= 2) { // medium
                        charWriter.println("track " + tibTrack
                                + ", head " + tibHead + ": "
                                + sectNo + " sectors of " + sectSize + " bytes"
                                );
                    } // informLevel >= 2
                    setPosition(tib0); // overwrite track information block
                    int isect = 0;
                    while (isect < sectNo) { // read 1 sector
                        readChunk(sectSize);
                        isect ++;
                    } // while isect
                } else if (tibTrack > itrack) {
                    log.error("wrong track# " + tibTrack + " for track " + itrack + ", head " + ihead);
                	// ignore
                } else { // tibTrack < itrack - ignore any strange tracks at the end of the disk
                    // log.error("wrong track# " + tibTrack + " for track " + itrack + ", head " + ihead);
                    setMaxCylinder(itrack - 1);
                    itrack = trackNo; // will stop loop
                    ihead  = headNo;
                }
                ihead ++;
            } // while ihead
            itrack ++;
        } // while itrack
        bufferLength = bufferPos;
    } // readContainer

} // DskBuffer
