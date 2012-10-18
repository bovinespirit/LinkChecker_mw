/*
 *  $Id: LinkChecker.java,v 1.8 2005/02/06 01:34:48 mfw Exp $	
 *  Copyright (C) 2002-2005 Matthew West
 *
 * @author <a href="mailto:lc@matthewwest.co.uk">Matthew West</a>
 * @version RCS:$Revision: 1.8 $ $Date: 2005/02/06 01:34:48 $ Dist:0.1 12-July-2002 10:38
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package uk.co.matthewwest.LinkChecker;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import java.util.LinkedList;
import java.util.HashSet;

import java.io.IOException;

/**
 * <p>
 * Loads a XHTML document and checks all the links one by one.  The program uses a SAX parser to read the
 * document so only well formed documents need apply.  The constructor sets the variable, the run() function
 * does all the processing.  The SAX parser expects certain files to be in the jar file along with this code.
 * These files are in the data/ directory.  The files are the various DTDs defining XHTML.
 * </p><p>
 * The structure of the program allows it to be called by main() or as an Ant task.  All output goes straight
 * to System.out without passing through any redirection.  This would make it impossible to write a GUI
 * wrapper, but enabled it to be written quickly.
 * </p>
 */
public class LinkChecker extends DefaultHandler implements Runnable {
    /** If this is set to true then all sorts of debugging messages will be displayed */
    static private boolean debug = false;
    /** Level of indenting of debug messages */
    static private int debugindent = 0;
    /** Starting point for search */
    private String root;
    /** Name of host */
    private String host = "null";
    /** Only check URLs on this host */
    private boolean loc;
    /** Determines length of delay between each request */
    private int del;
    /** URLs that are known to be valid */
    private HashSet urls = new HashSet();
    /** URLs that are known to be not valid */
    private HashSet badurls = new HashSet();
    /** List of pages to parse */
    private LinkedList order = new LinkedList();
    /** Parser factory that creates document parser */
    private SAXParserFactory spf;
    /** Used by SAX parser to get the current position */
    private Locator locator = null;
    private URL currenturl = null;

    /**
     * Create LinkChecker
     * @param url Root URL to start check from
     * @param loc Only check local addresses
     * @param del 2 Second delay between each delay
     */
    public LinkChecker(final String url, final boolean loc, final int del) {
	this.root = url;
	this.loc = loc;
	this.del = del;
	spf = SAXParserFactory.newInstance();
	spf.setNamespaceAware(false);
	spf.setValidating(false);
    }

    /**
     * This does all the work
     */
    public void run() {
	String s="";
	int i;
	URL current;

	try {
	    current = new URL(root);
	    host = current.getHost();
	} catch (Exception e) {
	    s = "Bad URL supplied " + root + " " + e.getMessage();
	}
	if(s.equals(""))
	    s = checkUrl(root, null);
	if(!s.equals("")) {
	    System.out.println("Error finding source document - " + s);
	    return;
	}
	i = 0;
	while(i < order.size()) {
	    current = (java.net.URL)order.get(i);
	    i++;
	    debugPrint("Processing "+current.toExternalForm(), true);
	    try {
		if(current.getProtocol().equals("http")) {
		    HttpURLConnection huc = (HttpURLConnection)current.openConnection();
		    huc.setFollowRedirects(true);
		    huc.connect();
		    debugPrint("Content Type : " + huc.getContentType());
		    if(huc.getContentType().startsWith("text/html")) {
			parseUrl(current);
		    }
		}
	    } catch (IOException e) {
		System.out.println("Error loading document URL:" + current.toExternalForm() + "  " + e.getMessage());
	    }
	    debugEnd();
	}
    }

    /**
     * Checks that a resource pointed to by a URL exists.  If it does it is added to the <code>urls</code>
     * HashSet.  If it doesn't then it is added to the <code>badurls</code> HashSet and a message is returned.
     * If it exists, is local, and hasn't been checked then it is added to the <code>order</code> list of
     * pages waiting to be parsed.
     * @param url String representation of URL
     * @param base Context for URL
     * @return Empty string if URL is OK, message if not.  */
    public String checkUrl(final String url, final URL base) {
	debugPrint("Checking URL:"+url, true);
	URL u;
	try {
	    u = new URL(base, url);
	} catch (MalformedURLException e) {
	    debugEnd();
	    return e.getMessage() + "  " + url;
	}
	if(urls.contains(u)) {
	    debugEnd();
	    return "";
	}
	if(badurls.contains(u)) {
	    debugEnd();
	    return "Previous Error URL:"+u;
	}
	if(loc && !(host.equals(u.getHost()))) {
	    urls.add(u);
	    debugEnd();
	    return "";
	}
	try {
	    Thread.sleep(del);
	} catch (InterruptedException e) {
	    System.out.println(e.getMessage());
	}
	try {
	    if(u.getProtocol().equals("http")) {
		HttpURLConnection huc = (HttpURLConnection)u.openConnection();
		huc.setFollowRedirects(true);
		huc.connect();
		debugPrint("HTTP Response:"+huc.getResponseCode());
		if(huc.getResponseCode() != 200) {
		    debugEnd();
		    badurls.add(u);
		    return "HTTP Error:"+huc.getResponseMessage()+" URL:"+url;
		}
		huc.disconnect();
	    } else if (u.getProtocol().equals("mailto")) {
		urls.add(u);
		return "";
	    } else {
		URLConnection uc = u.openConnection();
		if(uc == null) {
		    debugEnd();
		    badurls.add(u);
		    return "Could not find URL:"+url;
		}
		if(uc.getInputStream() == null) {
		    debugEnd();
		    badurls.add(u);
		    return "Cannot read from document:"+url;
		}
	    }
	} catch (java.io.IOException e) {
	    debugEnd();
	    badurls.add(u);
	    return e.getMessage() + " URL: " + url;
	}
	urls.add(u);
	if(host.equals(u.getHost())) {
	    if(!order.contains(u)) {
		debugPrint("Adding "+u+" to order");
		order.add(u);
	    }	
	}
	debugEnd();
	return "";
    }

    /**
     * The command line interface.  This checks the command line parameters.  If there is a problem the help
     * message is displayed, otherwise the LinkChecker object is created and run.
     */
    public static void main(String[] args) {
	int i;
	boolean help = false;
	boolean loc = false;
	int del = 0;
	String url = null;
	for(i = 0; i<args.length; i++) {
	    if(args[i].startsWith("-")) {
		if(args[i].charAt(1) == 'h')
		    help = true;
		if(args[i].charAt(1) == 'd') {
		    del = Integer.parseInt(args[i+1]);
		    i++;
		}
		if(args[i].charAt(1) == 'l')
		    loc = true;
		if(args[i].charAt(1) == 'b')
		    LinkChecker.setDebug(true);
	    } else {
		url = args[i];
	    }
	}
	if(url == null)
	    help = true;
	if(help) {
	    System.out.println("LinkChecker");
	    System.out.println("Copyright (C) 2002-2005 Matthew West");
	    System.out.println("Usage: LinkChecker [-d][-l] url");
	    System.out.println("  url -> Url to start from");
	    System.out.println("   -d -> Delay each page");
	    System.out.println("   -l -> Only check local pages");
	} else {
	    LinkChecker lc = new LinkChecker(url, loc, del);
	    lc.run();
	}
    }

    /**
     * Sets the debugging flag, which causes lots of debug information
     * to be displayed
     * @param d Show debugging information
     */
    static public void setDebug(final boolean d) {
	debug = d;
    }

    /**
     * Prints debugging information if debug = true
     * @param s Message
     * @param i increase indent
     */
    static public void debugPrint(final String s, final boolean i) {
	if(i)
	    debugindent++;
	if(debug) {
	    for(int j = 0; j < debugindent; j++)
		System.err.print(" ");
	    System.err.println(s);
	}
    }

    /**
     * Print debugging information
     * @param s Message
     */
    static public void debugPrint(final String s) {
	debugPrint(s, false);
    }
    
    /**
     * Decrease indenting
     */
    static public void debugEnd() {
	debugindent--;
    }

    /*
     * SAX Methods and classes
     */

    /** Sets the locator reference */
    public void setDocumentLocator(Locator l) {
	locator = l;
    }

    /**
     * Checks if current element is a link element, i.e &lt;a&gt;,&lt;link&gt; or &lt;img&gt;.  If it is then
     * the link is passed to checkUrl() to be checked out.
     */
    public void startElement(String ns, String ln, String qName, Attributes atts) throws SAXException {
	String s = null;
	debugPrint(qName, true);
	if(qName.toLowerCase().equals("a") || qName.toLowerCase().equals("link"))
	    s = atts.getValue("href");
	if(qName.toLowerCase().equals("img"))
	    s = atts.getValue("src");
	if(s != null) {
	    String m = checkUrl(s, currenturl);
	    if(!m.equals(""))
		System.out.println("  File:"+locator.getSystemId()+" Line:"+locator.getLineNumber()+"  "+m);
	}
    }
    public void endElement(String ns, String localName, String qName) throws SAXException {
	debugEnd();
    }
    /**
     * Creates parser, then starts it
     * @param url URL of page to be checked
     */
    public void parseUrl(final URL url) {
	currenturl = url;
	XMLReader xr = null;
	
	try { Thread.sleep(del);
	} catch (InterruptedException e) {
	    System.out.println(e.getMessage()); }
        try {
            SAXParser sp = spf.newSAXParser();
            xr = sp.getXMLReader();
        } catch (Exception e) {
            System.out.println(e.getMessage());
	    return;
        }
	xr.setContentHandler(this);
	xr.setDTDHandler(this);
	xr.setEntityResolver(this);
	xr.setErrorHandler(new SaxErrorHandler());
        try {
            xr.parse(url.toExternalForm());
        } catch (Exception e) {
            System.out.println("Error parsing document: "+e.getMessage());
        }
    }

    /**
     * This is a fairly rough bodge that redirects requests for
     * documents to copies contained in the .jar file.
     * If null is returned then the parser is expected to figure it out for itself by finding the document on
     * the Internet.  If this fails the program exits.
     * The documents are currently DTDs and entity files
     * @param publicId PublicId from DTD
     * @param systemId SystemId in DTD
     * @return Either an inputsource from the appropriate file, or null
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
	InputSource is = null;
	URL url = null;
	debugPrint("Resolving Entity SystemId="+systemId+" PublicId="+publicId, true);
 	if(publicId.equals("-//W3C//DTD XHTML 1.0 Strict//EN")) {
	    url = this.getClass().getClassLoader().getResource("xhtml1-strict.dtd");
	}
	if(publicId.equals("-//W3C//DTD HTML 4.01//EN")) {
	    url = this.getClass().getClassLoader().getResource("html4-strict.dtd");
	}
	if(publicId.equals("-//W3C//DTD HTML 4.01 Transitional//EN")) {
	    url = this.getClass().getClassLoader().getResource("html4-loose.dtd");
	}
	if(url != null) {
	    is = new InputSource(url.toExternalForm());
	}
	debugEnd();
	return is;
    }	

    /**
     * Used to handle errors from the SAX parser
     */
    static private class SaxErrorHandler implements ErrorHandler {
	private String getParseExceptionInfo(SAXParseException se) {
	    String si = se.getSystemId();
	    if(si == null) {
		si = "null";
	    }
	    return "URL:"+si+" line:"+se.getLineNumber()+" : "+se.getMessage();
	}
	
	public void warning(SAXParseException se) throws SAXException {
	    debugPrint("Warning:" + getParseExceptionInfo(se));
	}
	public void error(SAXParseException se) throws SAXException {
	    throw new SAXException("Error:"+getParseExceptionInfo(se));
	}
	public void fatalError(SAXParseException se) throws SAXException {
	    throw new SAXException("Fatal Error:"+getParseExceptionInfo(se));
	}
    }
}	
