/******************************************************************
 * File:        Cache.java
 * Created by:  Dave Reynolds
 * Created on:  22 May 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import org.apache.commons.collections.map.LRUMap;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.util.FileManager;

/**
 * Cache loaded closure models.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ModelCache {
    static final String VOCAB_FILE = "org.ttl";
    static final String closure = FileManager.get().readWholeFileAsUTF8("queries/closure.ru");
    static ModelCache cache = new ModelCache();

    // This could be configurable as a service
    static final int CACHE_SIZE = 100;

    public static ModelCache get() {
        return cache;
    }

    LRUMap mcache = new LRUMap(CACHE_SIZE);

    /**
     * Return or compute a cached closure of the indicated upload
     */
    public Model getUpload(String path) {
        if (mcache.containsKey(path)) {
            return (Model)mcache.get(path);
        } else {
            String filename = String.format("%s/uploads/%s/merge.ttl", Config.get().getDirbase(), path);
            try {
                Model model = FileManager.get().loadModel(filename);
                model.add( FileManager.get().loadModel(VOCAB_FILE) );
                model = computeClosure( model);
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

    public void flush(String path) {
        mcache.remove(path);
    }

    public void clear() {
        mcache.clear();
    }

}
