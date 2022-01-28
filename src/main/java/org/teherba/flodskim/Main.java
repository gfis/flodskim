/*  Read and Process (Floppy) Disk Image Formats
    @(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $
    2013-11-05, Georg Fischer: copied from Main
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

package org.teherba.flodskim;
import  org.teherba.flodskim.buffer.BaseBuffer;
import  org.teherba.flodskim.buffer.BufferFactory;
import  org.teherba.flodskim.system.BaseSystem;
import  org.teherba.flodskim.system.DirectoryEntry;
import  org.teherba.flodskim.system.SystemFactory;
import  java.io.StringWriter;
import  java.io.PrintWriter;
import  java.util.Iterator;
import  java.util.regex.Matcher;
import  java.util.regex.Pattern;
import  org.apache.logging.log4j.Logger;
import  org.apache.logging.log4j.LogManager;

/** Spells a number (or some other enumerable word) in some language.
 *  This class is the commandline interface to <em>BaseSpeller</em>.
 *  @author Dr. Georg Fischer
 */
final public class Main {
    public final static String CVSID = "@(#) $Id: Main.java 820 2011-11-07 21:59:07Z gfis $";

    /** log4j logger (category) */
    public Logger log;
    /** Newline string (CR/LF or LF only) */
    private String nl;

    /** code for output format */
    private int mode;
    /** code for HTML output format */
    public static final int MODE_HTML     = 0;
    /** code for emphasized HTML output format */
    public static final int MODE_HTML_EM  = 1;
    /** code for plain text output format */
    public static final int MODE_PLAIN    = 2;
    /** code for tab separated values, for Excel and other spreadsheet processors */
    public static final int MODE_TSV      = 3;
    /** code for XML output format */
    public static final int MODE_XML      = 4;

    /** Sets the output format
     *  @param format code for the output format, corresponding to the value of commandline option "-m"
     */
    public void setMode(int format) {
        mode = format;
    } // setMode

    /** Gets the output format
     *  @return format code for the output format, corresponding to the value of commandline option "-m"
     */
    public int getMode() {
        return mode;
    } // getMode

    /** No-args Constructor
     */
    public Main() {
        log = LogManager.getLogger(Main.class.getName());
        nl = System.getProperty("line.separator");
        setMode(MODE_PLAIN);
    } // Constructor()

    /** Convenience overlay method with a single string argument instead
     *  of an array of strings.
     *  @param commandLine all parameters of the commandline in one string
     */
    public void process(String commandLine) {
        process(commandLine.split("\\s+"));
    } // process(String)

    /** Evaluates the arguments of the command line, and processes them.
     *  @param args Arguments; if missing, print the usage string
     */
    public void process(String args[]) {
        BufferFactory bufferFactory  = new BufferFactory();
        BaseBuffer    container = null;
        SystemFactory systemFactory  = new SystemFactory();
        BaseSystem    fileSystem = null;
        try {
            int iarg = 0; // index for command line arguments
            if (iarg >= args.length) { // usage
                System.out.println("Usage:\tjava org.teherba.flodskim.Main parameters actions");
                System.out.println("Parameters are:");
                System.out.println("  -buffer code        container format is code (default: dsk)");
                System.out.println("  -system code        filesystem is code (default: base)");
                System.out.println("  -inform num         amount of diagnostic output");
                System.out.println("Actions on buffers are:");
                System.out.println("  -block xnum         dump block xnum");
                System.out.println("  -dump xoffs xlen    hexadecimal dump");
                System.out.println("  -read filename      read a disk image file");
                System.out.println("Actions on file systems are:");
                System.out.println("  -dir                print a directory listing");
                System.out.println("  -copy path          copy all files into path");
            } else { // >= 1 argument
                String bufferCode = "dsk";
                String systemCode = "base";
                String fileName   = null;
                String targetPath = ".";
                int informLevel   = 0; // amount of diagnostic information

                // get all option codes
                while (iarg < args.length && args[iarg].startsWith("-")) {
                    String option = args[iarg ++];
                    if (false) {

                    } else if (option.startsWith("-block"   )) {
                        String tblock = args[iarg ++];
                        int blockNo = 0;
                        try {
                            blockNo = Integer.parseInt(tblock, 16); // hex
                        } catch (Exception exc) {
                            log.error("Main.process: numeric exception, blockNo=" + tblock);
                        }
                        byte[] block = fileSystem.getBlock(blockNo);
                        fileSystem.dump(block, 0, block.length);

                    } else if (option.startsWith("-buffer"  )) {
                        bufferCode = args[iarg ++];
                        container = bufferFactory.getInstance(bufferCode);

                    } else if (option.startsWith("-copy"    )) {
                        targetPath = args[iarg ++];
                        fileSystem.copyFiles(targetPath);

                    } else if (option.startsWith("-dir"     )) {
                        fileSystem.printDirectory();

                    } else if (option.startsWith("-dump"    )) {
                        String toffs = args[iarg ++];
                        String tlen  = args[iarg ++];
                        int offset = 0;
                        int length = 0x100;
                        try {
                            offset = Integer.parseInt(toffs, 16);
                            length = Integer.parseInt(tlen , 16);
                        } catch (Exception exc) {
                            log.error("Main.process: numeric exception, offset=" + toffs + ", length=" + tlen);
                        }
                        if (container == null) {
                            container = bufferFactory.getInstance(bufferCode);
                        }
                        container.dump(offset, length);

                    } else if (option.startsWith("-inform"  )) {
                        String tlevel = args[iarg ++];
                        try {
                            informLevel = Integer.parseInt(tlevel, 10);
                        } catch (Exception exc) {
                            log.error("Main.process: numeric exception, level=" + tlevel);
                        }

                    } else if (option.startsWith("-read"    )) {
                        fileName = args[iarg ++];
                        if (container == null) {
                            container = bufferFactory.getInstance(bufferCode);
                        }
                        container.openFile(0, fileName);
                        container.openFile(1, null);
                        container.readContainer(informLevel);

                    } else if (option.startsWith("-system"  )) {
                        systemCode = args[iarg ++];
                        fileSystem = systemFactory.getInstance(systemCode);
                        if (container == null) {
                            container = bufferFactory.getInstance(bufferCode);
                        }
                        fileSystem.setContainer(container);

                    } else {
                        System.err.println("invalid option \"" + option + "\"");
                    }
                } // while options
                container.closeAll();
            } // args.length >= 1
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        } // try
     } // process

    /** Commandline interface for number spelling.
     *  @param args elements of the commandline separated by whitespace
     */
    public static void main(String args[]) {
        Main command = new Main();
        command.process(args);
    } // main

} // Main
