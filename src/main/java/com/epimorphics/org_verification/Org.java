/******************************************************************
 * File:        Org.java
 * Created by:  Dave Reynolds
 * Created on:  27 May 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.util.FileManager;

/**
 * Supporting utility for computing the ORG closure.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Org {
    static final String VOCAB_FILE = "org.ttl";
    static final String closure = FileManager.get().readWholeFileAsUTF8("queries/closure.ru");

    static Org org = new Org();
    
    public static Org get() {
        return org;
    }
    
    protected Model vocab;
    protected Map<String, String> rawOrgQueries = new HashMap<String, String>();
    
    public Org() {
        vocab = FileManager.get().loadModel(VOCAB_FILE);
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
    
    public String getQuery(String name) {
        return rawOrgQueries.get(name);
    }
    
    /**
     * Return or compute a cached closure of the indicated upload
     */
    public Model getUpload(String path) {
        ModelCache mcache = ModelCache.get();
        if (mcache.containsKey(path)) {
            return (Model)mcache.get(path);
        } else {
            String filename = Config.get().getUploadFilename(path);
            if (filename == null) {
                return null;
            }
            try {
                Model model = FileManager.get().loadModel(filename);
                model.add( vocab );
                model = computeClosure( model);
                model.setNsPrefixes( vocab );
                mcache.put(path, model);
                return model;
            } catch (Throwable t) {
                return null;
            }
        }
    }

    // Would make more sense to put closure computation somewhere else but for now ...
    protected Model computeClosure(Model model) {
        Dataset ds = DatasetFactory.create(model);
        GraphStore graphStore = GraphStoreFactory.create(ds) ;
        UpdateAction.parseExecute(closure, graphStore);
        Model result = ModelFactory.createModelForGraph( graphStore.getDefaultGraph() );
        result.setNsPrefixes(model);
        return result;
    }
    
}
