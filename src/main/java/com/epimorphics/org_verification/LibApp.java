/******************************************************************
 * File:        LibReg.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import org.apache.jena.atlas.json.JsonValue;

import com.epimorphics.rdfutil.ModelWrapper;
import com.epimorphics.rdfutil.RDFNodeWrapper;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.templates.LibPlugin;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Some supporting methods to help Velocity UI access
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LibApp extends ServiceBase implements LibPlugin, Service {

    public LibApp() {
    }

    public ModelWrapper getUpload(String path) {
        Model model = Org.get().getUpload(path);
        if (model != null) {
            return new ModelWrapper(model);
        } else {
            return null;
        }
    }

    public String getQuery(String name) {
        return Org.get().getQuery(name);
    }

    public String getOrgQuery(String name, RDFNodeWrapper org) {
        String rawQuery = getQuery(name);
        if (rawQuery != null) {
            return rawQuery.replaceAll("\\$org", "<" + org.getURI() + ">");
        }
        return null;
    }
    
    public JsonValue qbTest(String ic, String upload) {
        return DataCube.get().validate(ic, upload);
    }
}
