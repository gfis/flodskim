/*  Selects the applicable subclass of BaseSystem
    @(#) $Id: SystemFactory.java 657 2011-03-17 07:56:38Z gfis $
    2013-11-05, Georg Fischer
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
 * See the License for the specific dialect language permissions and
 * limitations under the License.
 */
package org.teherba.flodskim.system;
import  org.teherba.flodskim.system.BaseSystem;
import  java.util.ArrayList;
import  java.util.Iterator;
import  java.util.StringTokenizer;
import  org.apache.log4j.Logger;

/** List of subclasses of {@link BaseSystem},
 *  with an iterator and access method.
 *  Initially, a list of the available {@link BaseSystem}s is built, and classes
 *  which cannot be instantiated are <em>silently</em> ignored.
 *  @author Dr. Georg Fischer
 */
public class SystemFactory {
    public  final static String CVSID = "@(#) $Id: SystemFactory.java 657 2011-03-17 07:56:38Z gfis $";
    private final static String PACKAGE_NAME = "org.teherba.flodskim.system.";

    /** log4j logger (category) */
    private Logger log;

    /** Array of instances for different disk image formats */
    private ArrayList<BaseSystem> instances;

    /** Attempts to instantiate an instance of buffer for some disk image format
     *  @param code a lowercase word identifying the instance
     *  @param baseName the name of the applicable Java class, without the package name
     */
    private void addInstance(String code, String baseName) {
        try {
            instances.add((BaseSystem) Class.forName(PACKAGE_NAME + baseName).newInstance());
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
            // ignore any error silently - this file extension will not be known
        }
    } // addInstance

    /** No-args Constructor.
     *  The order of the file extensions here defines the order in the user interfaces.
     */
    public SystemFactory() {
        log = Logger.getLogger(SystemFactory.class.getName());
        try {
            instances = new ArrayList<BaseSystem>(64);
            addInstance("base"		, "BaseSystem");
            addInstance("cpm"		, "Cpm");
            addInstance("dec-rx50"	, "Dec_rx50");
            addInstance("sinix-mx2"	, "Sinix_mx2");
            addInstance("ta-vs"		, "Ta_vs");
            addInstance("tar"		, "Tar");
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
    } // Constructor(0)

    /** Gets an iterator over all instances.
     *  @return iterator over {@link #instances}
     */
    public Iterator<BaseSystem> getIterator() {
        Iterator<BaseSystem> result = instances.iterator();
        return result;
    } // getIterator

    /** Gets the number of available instances
     *  @return size of {@link #instances}
     */
    public int getCount() {
        return instances.size();
    } // getCount

    /** Determines whether code denotes and instance
     *  @param instance the instance to be tested
     *  @param code code for the desired instance
     */
    public boolean isApplicable(BaseSystem instance, String code) {
        boolean result = false;
        StringTokenizer tokenizer = new StringTokenizer(instance.getCode(), ",");
        while (! result && tokenizer.hasMoreTokens()) { // try all tokens
            if (code.equals(tokenizer.nextToken())) {
                result = true;
            }
        } // while all tokens
        return result;
    } // isApplicable

    /** Gets the applicable instance for a specified code,
     *  and {@link BaseSystem#initialize} it
     *  @param code code for the instancce
     *  @return the instance for that code, or <em>null</em> if the
     *  code was not found
     */
    public BaseSystem getInstance(String code) {
        BaseSystem result = null;
        Iterator<BaseSystem> iter = getIterator();
        boolean notFound = true;
        while (notFound && iter.hasNext()) {
            BaseSystem instance = iter.next();
            if (isApplicable(instance, code)) {
                result = instance;
                result.initialize();
                notFound = false;
            } // applicable
        } // while not found
        if (notFound) {
        	System.err.println("System for code \"" + code + "\" was not found");
        }
        return result;
    } // getInstance

} // SystemFactory
