/******************************************************************
 * File:        DataFetch.java
 * Created by:  Dave Reynolds
 * Created on:  22 May 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import static com.epimorphics.webapi.marshalling.RDFXMLMarshaller.FULL_MIME_RDFXML;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.epimorphics.server.webapi.BaseEndpoint;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.NotFoundException;

@Path("/uploads")
public class DataFetch extends BaseEndpoint {
    public static final String FULL_MIME_TURTLE = "text/turtle; charset=UTF-8";

    @Path("{path: .*}")
    @GET
    @Produces({FULL_MIME_TURTLE, FULL_MIME_RDFXML})
    public Model getUpload(@PathParam("path") String path) {
        // Could make this streaming but no particular need to
        Model model = ModelCache.get().getUpload(path);
        if (model == null) {
            throw new NotFoundException(path);
        } else {
            return model;
        }

    }

}
