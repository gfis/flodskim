/*  Bean for a directory entry
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2013-11-07, Georg Fischer: copied from Cpm
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
import  java.util.ArrayList;
import  java.util.Iterator;
import  org.apache.log4j.Logger;

/** Bean properties and methods for a directory entry
 *  @author Dr. Georg Fischer
 */
public class DirectoryEntry {
    public final static String CVSID = "@(#) $Id: DirectoryEntry.java 852 2012-01-06 08:07:08Z gfis $";

    /** whether to write debugging output (iff > 0) */
    protected final static int debug = 0;

    /** log4j logger (category) */
    private Logger log;

    //--------------------------------
    // Construction
    //--------------------------------
    /** Constructor with no arguments
     */
    public DirectoryEntry() {
        log = Logger.getLogger(DirectoryEntry.class.getName());
        blockList = new ArrayList<Integer>(32);
        setBaseFileName("");
        setExtension("");
        setExtentNumber(0);
        setDeleted(true);
    } // Constructor(0)

    //--------------------------
    // Bean properties
    //--------------------------
    /** Base file name (without path and extension) */
    private String baseFileName;

    /** Sets the base file name
     *  @param baseFileName base file name
     */
    public void setBaseFileName(String baseFileName) {
        this.baseFileName = baseFileName;
    } // setBaseFileName

    /** Gets the base file name
     *  @return base file name
     */
    public String getBaseFileName() {
        return baseFileName;
    } // getBaseFileName

    /** List of allocated blocks */
    private ArrayList<Integer> blockList;

    /** Adds a block number
     *  @param blockNo add this block number
     */
    public void addBlock(int blockNo) {
        blockList.add(new Integer(blockNo));
    } // addBlock

    /** Gets an iterator over all block numbers
     *  @return iterator over block numbers
     */
    public Iterator<Integer> getBlockIterator() {
        Iterator<Integer> result = blockList.iterator();
        return result;
    } // getBlockIterator

    /** Gets the number of allocated blocks
     *  @return number of allocated blocks
     */
    public int getBlockCount() {
        return blockList.size();
    } // getBlockCount

    //----
    /** Whether the entry is deleted */
    private boolean deleted;

    /** Sets the deleted property
     *  @param deleted true if the entry is deleted
     */
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    } // setDeleted

    /** Determine whether the entry is deleted
     *  @return true if the entry is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deleted;
    } // is deleted

    /** Allocation extent number */
    private int extentNumber;

    /** Sets the allocation extent number
     *  @param extentNumber allocation extent number
     */
    public void setExtentNumber(int extentNumber) {
        this.extentNumber = extentNumber;
    } // setExtentNumber

    /** Gets the allocation extent number
     *  @return allocation extent number
     */
    public int getExtentNumber() {
        return extentNumber;
    } // getExtentNumber

    /** File name extension */
    private String extension;

    /** Sets the file name extension
     *  @param extension file name extension
     */
    public void setExtension(String extension) {
        this.extension = extension;
    } // setExtension

    /** Gets the file name extension
     *  @return file name extension
     */
    public String getExtension() {
        return extension;
    } // getExtension

    /** Number of bytes in the file */
    private int fileSize;

    /** Sets the file size
     *  @param fileSize file size
     */
    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    } // setFileSize

    /** Gets the file size
     *  @return file size
     */
    public int getFileSize() {
        return fileSize;
    } // getFileSize

    //--------------------------
    // Access methods
    //--------------------------

    /** Get the display representation of this directory entry
     *  @return human readable entry
     */
    public String toString() {
        StringBuffer result = new StringBuffer(128);
        String extension = getExtension();
        result.append(String.format("%-18s", getBaseFileName() + (extension.length() > 0 ? "." + extension : "")));
        result.append(String.format(" %2d" , getExtentNumber()));
        result.append(String.format(" %6d" , getFileSize()));
        if (isDeleted()) {
            result.append(" deleted");
        } else {
            // continue
        }
        // print all block numbers
        Iterator<Integer> blockIterator = getBlockIterator();
        while (blockIterator.hasNext()) {
           int blockNo = blockIterator.next();
           result.append(String.format(" %3x", blockNo));
        } // while blocks
        return result.toString();
    } // toString

} // DirectoryEntry
