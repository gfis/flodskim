/*  Read and Process (Floppy) Disk Image Formats
    @(#) $Id: Servlet.java 820 2011-11-07 21:59:07Z gfis $
    2017-05-29: javadoc 1.8
    2013-11-05, Dr. Georg Fischer: copied from numword

    Caution, only a stub, not tested!
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
import  org.teherba.flodskim.Main;
import  java.io.IOException;
import  javax.servlet.RequestDispatcher;
import  javax.servlet.ServletConfig;
import  javax.servlet.ServletContext;
import  javax.servlet.ServletException;
import  javax.servlet.http.HttpServlet;
import  javax.servlet.http.HttpServletRequest;
import  javax.servlet.http.HttpServletResponse;
import  javax.servlet.http.HttpSession;
import  org.apache.logging.log4j.Logger;
import  org.apache.logging.log4j.LogManager;

/** This class is the servlet interface to {@link Main},
 *  and ressembles the functionality of the commandline interface
 *  @author Dr. Georg Fischer
 */
public class Servlet extends HttpServlet {
    public final static String CVSID = "@(#) $Id: Servlet.java 820 2011-11-07 21:59:07Z gfis $";
    // public final static long serialVersionUID = 19470629004L;

    /** log4j logger (category) */
    private Logger log;
    /** instance of the number converter */
    private Main command;

    /** Called by the servlet container to indicate to a servlet
     *  that the servlet is being placed into service.
     *  @param config object containing the servlet's configuration and initialization parameters
     *  @throws ServletException for Servlet errors
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config); // ???
        log = LogManager.getLogger(Servlet.class.getName());
        command = new Main();
    } // init

    /** Creates the response for a HTTP GET request.
     *  @param request fields from the client input form
     *  @param response data to be sent back the user's browser
     *  @throws IOException for IO errors
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        generateResponse(request, response);
    } // doGet

    /** Creates the response for a HTTP POST request.
     *  @param request fields from the client input form
     *  @param response data to be sent back the user's browser
     *  @throws IOException for IO errors
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        generateResponse(request, response);
    } // doPost

    /** Gets the value of an HTML input field, maybe as empty string
     *  @param request request for the HTML form
     *  @param name name of the input field
     *  @return non-null (but possibly empty) string value of the input field
     */
    private String getInputField(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null) {
            value = "";
        }
        return value;
    } // getInputField

    /** Creates the response for a HTTP GET or POST request.
     *  @param request fields from the client input form
     *  @param response data to be sent back the user's browser
     *  @throws IOException for IO errors
     */
    public void generateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            HttpSession session = request.getSession();
            // NumwordCommand command = new NumwordCommand();
            String view     = getInputField(request, "view");
            String language = getInputField(request, "language");
            if (language.equals("")) {
                language = "de";
            }
            String function = getInputField(request, "function");
            String digits   = getInputField(request, "digits"  );
            session.setAttribute("command"  , command);
            session.setAttribute("language" , language);
            session.setAttribute("digits"   , digits  );
            String newPage = "index";
            if (false) {
            } else if (false) {
                newPage = "message";
                session.setAttribute("messno", "003"); // invalid language code
            } else if (function.equals("c")    ) {
                session.setAttribute("function", function);
            } else if (function.equals("C")    ) {
                // check for alphabetic "digits" input field
                session.setAttribute("function", function);
            } else if (function.equals("d")    ) {
                session.setAttribute("function", function);
            } else if (function.equals("g")    ) {
                session.setAttribute("function", function);
            } else if (function.startsWith("h")) {
                session.setAttribute("function", function);
            } else if (function.equals("m")    ) {
                session.setAttribute("function", function);
            } else if (function.equals("m3")   ) {
                session.setAttribute("function", function);
            } else if (function.equals("p")    ) {
                session.setAttribute("function", function);
            } else if (function.equals("s")    ) {
                session.setAttribute("function", function);
            } else if (function.equals("w")    ) {
                session.setAttribute("function", function);
            } else if (function.equals("w2")   ) {
                session.setAttribute("function", function);
            } else { // invalid function
                newPage = "message";
                session.setAttribute("messno"  , "001");
            }
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/" + newPage + ".jsp");
            dispatcher.forward(request, response);
        } catch (Exception exc) {
            response.getWriter().write(exc.getMessage());
            System.out.println(exc.getMessage());
            throw new IOException(exc.getMessage());
        }
    } // generateResponse

} // Servlet
