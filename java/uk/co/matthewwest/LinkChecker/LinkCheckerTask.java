/*
 *  $Id: LinkCheckerTask.java,v 1.3 2004/01/07 22:08:56 mfw Exp $	
 *  Copyright (C) Matthew West 2002-2004
 *
 * @author <a href="mailto:lc@matthewwest.co.uk">Matthew West</a>
 * @version RCS:$Revision: 1.3 $ $Date: 2004/01/07 22:08:56 $ Dist:0.1 12-July-2002 10:38
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * This is an Ant task for the LinkChecker, enabling sites to be checked from within an Ant script.
 */ 
public class LinkCheckerTask extends Task {
    private String url;
    private int del;
    private boolean loc;

    public void setDelay(int d) {
	del = d;
    }

    public void setLocal(boolean l) {
	loc = l;
    }

    public void setHref(String u) {
	url = u;
    }

    public void execute() throws BuildException {
	uk.co.matthewwest.LinkChecker.LinkChecker lc = new LinkChecker(url, loc, del);
	lc.run();
    }
}

