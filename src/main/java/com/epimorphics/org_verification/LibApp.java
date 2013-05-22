/******************************************************************
 * File:        LibReg.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import java.util.HashMap;
import java.util.Map;

import com.epimorphics.rdfutil.ModelWrapper;
import com.epimorphics.rdfutil.RDFNodeWrapper;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.templates.LibPlugin;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

/**
 * Some supporting methods to help Velocity UI access
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LibApp extends ServiceBase implements LibPlugin, Service {
    protected Map<String, String> rawOrgQueries = new HashMap<String, String>();

    public LibApp() {
        initQuery("listorg.sparql");
        initQuery("suborgs.sparql");
        initQuery("sites.sparql");
        initQuery("members.sparql");
        initQuery("posts.sparql");
        initQuery("change.sparql");
        initQuery("collaboration.sparql");
    }

    private void initQuery(String query) {
        rawOrgQueries.put(query, FileManager.get().readWholeFileAsUTF8("queries/" + query));
    }

    public ModelWrapper getUpload(String path) {
        Model model = ModelCache.get().getUpload(path);
        if (model != null) {
            return new ModelWrapper(model);
        } else {
            return null;
        }
    }

    public String getOrgQuery(String name, RDFNodeWrapper org) {
        String rawQuery = rawOrgQueries.get(name);
        if (rawQuery != null) {
            return rawQuery.replaceAll("\\$org", "<" + org.getURI() + ">");
        }
        return null;
    }
}
