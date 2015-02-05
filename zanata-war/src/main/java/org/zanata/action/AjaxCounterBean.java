/*
 * Copyright 2014, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.action;

import lombok.extern.slf4j.Slf4j;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

/**
 * @author Sean Flanigan <a href="mailto:sflaniga@redhat.com">sflaniga@redhat.com</a>
 */
@AutoCreate
@Name("ajaxCounter")
@Slf4j
public class AjaxCounterBean {
    // http://stackoverflow.com/questions/4410218/trying-to-keep-track-of-number-of-outstanding-ajax-requests-in-firefox
    private static final String AJAX_COUNTER_SCRIPT = "<script type=\"application/javascript\">" +
            "(function(xhr) {\n" +
            "  if (xhr.active === undefined) {\n" +
            "    xhr.active = 0;\n" +
            "    var pt = xhr.prototype;\n" +
            "    pt._send = pt.send;\n" +
            "    pt.send = function() {\n" +
            "        XMLHttpRequest.active++;\n" +
            "        this._onreadystatechange = this.onreadystatechange;\n" +
            "        this.onreadystatechange = function(e) {\n" +
            "            if ( this._onreadystatechange ) {\n" +
            "                var fn = this._onreadystatechange.handleEvent || this._onreadystatechange;\n" +
            "                fn.apply(this, arguments);\n" +
            "            }\n" +
            "            if ( this.readyState == 4 ) {\n" +
            "                XMLHttpRequest.active--;\n" +
            "            }\n" +
            "        };\n" +
            "        this._send.apply(this, arguments);\n" +
            "    }\n" +
            "  }\n" +
            "})(XMLHttpRequest)" +
            "</script>\n";
    public String getAjaxCounterScript() {
        String propName = "zanata.countAjax";
        if (Boolean.getBoolean(propName)) {
            return AJAX_COUNTER_SCRIPT;
        }
        return "";
    }
}
