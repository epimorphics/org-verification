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

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Cache loaded closure models.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ModelCache {
    // This could be configurable as a service
    static final int CACHE_SIZE = 20;

    static ModelCache cache = new ModelCache();
    
    public static ModelCache get() {
        return cache;
    }

    LRUMap mcache = new LRUMap(CACHE_SIZE);

    public void put(String key, Model model) {
        mcache.put(key, model);
    }
    
    public Model get(String key) {
        return (Model)mcache.get(key);
    }
    
    public boolean containsKey(String key) {
        return mcache.containsKey(key);
    }
    
    public void flush(String path) {
        mcache.remove(path);
    }

    public void clear() {
        mcache.clear();
    }

}
