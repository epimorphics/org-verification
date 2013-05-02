/******************************************************************
 * File:        Verify.java
 * Created by:  Dave Reynolds
 * Created on:  2 May 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.epimorphics.rdfutil.RDFUtil;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;

public class Verify {
    static final String VOCAB_FILE = "org.ttl";
    static final String RESULT = "result";
    static final String ORG = "http://www.w3.org/ns/org#";
    static final Resource ORG_Organization = ResourceFactory.createResource(ORG + "Organization");
    static final String closure = FileManager.get().readWholeFileAsUTF8("queries/closure.ru");

    static final VQuery[] verificationQueries = {
        new VQuery("listorg.sparql", false),
        new VQuery("suborgs.sparql", true),
        new VQuery("sites.sparql", true),
        new VQuery("members.sparql", true),
        new VQuery("posts.sparql", true),
        new VQuery("change.sparql", true),
        new VQuery("collaboration.sparql", true),
    };

    Model model = ModelFactory.createDefaultModel();

    public Verify(Model source) {
        model.add( source );
        model.setNsPrefixes( source );
        model.add( FileManager.get().loadModel(VOCAB_FILE) );
        computeClosure();
    }

    protected void computeClosure() {
        Dataset ds = DatasetFactory.create(model);
        GraphStore graphStore = GraphStoreFactory.create(ds) ;
        UpdateAction.parseExecute(closure, graphStore);
        Model result = ModelFactory.createModelForGraph( graphStore.getDefaultGraph() );
        result.setNsPrefixes(model);
        model = result;
    }

    protected void displayQueryResult(ResultSet results) {
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            List<String> vars = varNames(soln);
            if (vars.contains(RESULT)) {
                displayResultLine(soln, RESULT);
            }
            for (String var : vars) {
                if ( ! var.equals(RESULT) ) {
                    displayResultLine(soln, var);
                }
            }
            System.out.println("");
        }
    }

    protected void displayResultLine(QuerySolution soln, String var) {
        RDFNode value = soln.get(var);
        Object display = value;
        if (value != null) {
            display = RDFUtil.getLabel(value);
            if (value.isResource()) {
                if (value.isAnon()) {
                    display = String.format("[]");
                } else {
                    display = String.format("%s (%s)", value.asResource().getURI(), display);
                }
            }
        }
        System.out.println( String.format("  %18s = %s", var, display ) );
    }

    protected List<String> varNames(QuerySolution soln) {
        Iterator<String> i = soln.varNames();
        List<String> names = new ArrayList<String>();
        while (i.hasNext()) {
            String name = i.next();
            if ( ! name.startsWith("?")) {  // Suppress internal bnode variables
                names.add( name );
            }
        }
//        Collections.sort(names);
        return names;
    }

    public ResultSet runQuery(String query) {
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            return ResultSetFactory.copyResults( qexec.execSelect() );
        } finally {
            qexec.close();
        }
    }

    public ResultSet runQuery(String query, Resource org) {
        return runQuery( query.replaceAll("\\$org", "<" + org.getURI() + ">") );
    }


    public void runQuery(VQuery vq) {
        System.out.println("***" + vq.label + "\n");
        if (vq.isOrgQuery) {
            ResIterator ri = model.listSubjectsWithProperty(RDF.type, ORG_Organization);
            while (ri.hasNext()) {
                Resource org = ri.next();
                ResultSet results = runQuery(vq.query, org);
                if (results.hasNext()) {
                    System.out.println("    Result for " + model.shortForm(org.getURI()));
                    displayQueryResult(results);
                }
            }
        } else {
            displayQueryResult( runQuery(vq.query) );
        }
    }

    public void runQueries() {
        for (VQuery vq : verificationQueries) {
            runQuery(vq);
        }
    }

    public static void main(String[] args) {
        String test = "data/sample1.ttl";
        Verify verify = new Verify( FileManager.get().loadModel(test) );
        verify.runQueries();
    }

    public static class VQuery {
        String query;
        String label;
        boolean isOrgQuery;

        public VQuery(String queryfile, boolean isOrgQuery) {
            this.query = FileManager.get().readWholeFileAsUTF8("queries/" + queryfile);
            int split = query.indexOf("\n");
            this.label = query.substring(1, split);
            this.isOrgQuery = isOrgQuery;
        }
    }
}
