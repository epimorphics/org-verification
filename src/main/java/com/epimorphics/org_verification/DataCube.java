/******************************************************************
 * File:        DataCube.java
 * Created by:  Dave Reynolds
 * Created on:  27 May 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.vocabs.Cube;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.util.FileManager;

/**
 * Support for data cube closure and checking.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class DataCube {
    static final Logger log = LoggerFactory.getLogger( DataCube.class );

    static DataCube dataCube = new DataCube();
    
    public static DataCube get() {
        return dataCube;
    }
    
    protected static final String SDMX_FILE = "sdmx-dimension.ttl";
    final String closure = FileManager.get().readWholeFileAsUTF8("qb-queries/closure.ru");
    final String flatten = FileManager.get().readWholeFileAsUTF8("qb-queries/flatten.ru");
    protected Map<String, String> rawQueries = new HashMap<String, String>();
    protected Map<String, String> queryDescription = new HashMap<String, String>();
    protected Model sdmx;
    
    final String prefixes = 
            "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
    		"PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n" +
    		"PREFIX skos:    <http://www.w3.org/2004/02/skos/core#>\n" +
    		"PREFIX qb:      <http://purl.org/linked-data/cube#>\n" +
    		"PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>\n" +
    		"PREFIX owl:     <http://www.w3.org/2002/07/owl#>\n";

    public DataCube() {
        initQuery("integrity-10.sparql");
        initQuery("integrity-11.sparql");
        initQuery("integrity-12.sparql");
        initQuery("integrity-13.sparql");
        initQuery("integrity-14.sparql");
        initQuery("integrity-15.sparql");
        initQuery("integrity-16.sparql");
        initQuery("integrity-17.sparql");
        initQuery("integrity-18.sparql");
        initQuery("integrity-19a.sparql");
        initQuery("integrity-19b.sparql");
        initQuery("integrity-1.sparql");
        initQuery("integrity-20.sparql");
        initQuery("integrity-21.sparql");
        initQuery("integrity-2.sparql");
        initQuery("integrity-3.sparql");
        initQuery("integrity-4.sparql");
        initQuery("integrity-5.sparql");
        initQuery("integrity-6.sparql");
        initQuery("integrity-7.sparql");
        initQuery("integrity-8.sparql");
        initQuery("integrity-9.sparql");
        initQuery("list-hierarchies.sparql");
        initQuery("list-inverse-hierarchies.sparql");
        
        sdmx = FileManager.get().loadModel(SDMX_FILE);
    }
    
    private void initQuery(String query) {
        String q = FileManager.get().readWholeFileAsUTF8("qb-queries/" + query);
        String description = q.split("\n")[0].replace("# ", "");
        rawQueries.put(query, prefixes + q);
        queryDescription.put(query, description);
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
                model = expand(model);
                model.add(sdmx);
                model.setNsPrefixes( model );
                mcache.put(path, model);
                return model;
            } catch (Throwable t) {
                return null;
            }
        }
    }

    public  Model expand(Model m) {
        Dataset ds = DatasetFactory.create(m);
        GraphStore graphStore = GraphStoreFactory.create(ds) ;
        UpdateAction.parseExecute(closure, graphStore);
        UpdateAction.parseExecute(flatten, graphStore);
        Model result = ModelFactory.createModelForGraph( graphStore.getDefaultGraph() );
        result.setNsPrefixes(m);
        return result;
    }
    
    /**
     * Runs a single validation check, returns the result as a JsonValue.
     */
    public JsonValue validate(String ic, String upload) {
        Model model = getUpload(upload);
        if (ic.equals("12")) {
            JsonArray results = new JsonArray();
            results.add( new Result(ic, doIC12(model)).asJSON() );
            return results;
            
        } else if (ic.equals("20")) {
            return runHierarchyQuery(ic, "list-hierarchies.sparql", model);
            
        } else if (ic.equals("21")) {
            return runHierarchyQuery(ic, "list-inverse-hierarchies.sparql", model);
            
        } else {
            String query = rawQueries.get("integrity-" + ic + ".sparql");
            if (query == null) {
                log.error("Failed to retrieve query for IC-" + ic);
                return null;
            }
            return validateOne(ic, model, query);
        }
    }

    protected JsonValue validateOne(String ic, Model model, String query) {
        JsonArray results = new JsonArray();
        results.add( doValidate(ic, model, query).asJSON() );
        return results;
    }
    
    protected Result doValidate(String ic, Model model, String query) {
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            boolean result = qexec.execAsk();
            return new Result(ic, !result);
        } finally {
            qexec.close();
        }
    }

    private JsonValue runHierarchyQuery(String ic, String searchQuery, Model model) {
        String search = rawQueries.get(searchQuery);
        String validation = rawQueries.get("integrity-" + ic + ".sparql");
        JsonArray results = new JsonArray();
        for (RDFNode p : QueryUtil.selectAllVar("p", model, search, null)) {
            log.debug("Running hierarchy query on " + p);
            String vq = validation.replace("${0}", p.asResource().getURI());
            Result r = doValidate(ic, model, vq);
            r.description += " Run on " + p;
            results.add( r.asJSON() );
        }
        return results;
    }
    
    // More scalable verison of "No duplicate observations"
    private boolean doIC12(Model model) {
        NodeIterator ni = model.listObjectsOfProperty(Cube.dataSet);
        while(ni.hasNext()) {
            Resource dataset = ni.next().asResource();
            Set<String> locations = new HashSet<>();
            String query = "PREFIX qb: <http://purl.org/linked-data/cube#> SELECT DISTINCT ?i WHERE { <" + dataset.getURI() + "> qb:structure/qb:component/qb:componentProperty ?i. ?i a qb:DimensionProperty. }";
            List<Property> dims = new ArrayList<>();
            for (RDFNode dim:  QueryUtil.selectAllVar("i", model, query, null)) {
                dims.add( dim.as(Property.class) );
            }
            ResIterator ri = model.listSubjectsWithProperty(Cube.dataSet, dataset);
            while (ri.hasNext()) {
                String location = "";
                Resource obs = ri.next();
                for (Property dim : dims) {
                    RDFNode value = obs.getProperty(dim).getObject();
                    location += dim.getURI() + "=" + value +";";
                }
//                log.info("Checking obs: " + location);
                if ( !locations.add(location) ) {
                    log.info("Found duplicate on " + obs + " at " + location);
                    return false;
                }
            }
        }
        return true;
    }
    
    
    public final class Result {
        String constraint;
        String description;
        String url;
        boolean passed;
        
        public Result(String ic, boolean ok) {
            constraint = ic;
            description = queryDescription.get("integrity-" + ic + ".sparql");
            url = "http://www.w3.org/TR/vocab-data-cube/#ic-" + ic.replaceAll("a$", "");
            passed = ok;
        }
        
        public String toString() {
            return String.format("IC-%s '%s' ", constraint, description) + (passed ? "passed" : "FAILED");
        }
        
        public JsonObject asJSON() {
            JsonObject jo = new JsonObject();
            jo.put("ic", constraint);
            jo.put("url", url);
            jo.put("description", description);
            jo.put("passed", passed);
            return jo;
        }
    }
    
    
    
}
