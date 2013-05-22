/******************************************************************
 * File:        RequestProcessor.java
 * Created by:  Dave Reynolds
 * Created on:  22 May 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.util.FileUtil;
import com.epimorphics.util.NameUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

@Path("/request")
public class RequestProcessor {
    static final Logger log = LoggerFactory.getLogger( RequestProcessor.class );

    public static final String UPLOAD_DIR = "uploads";

    protected static long count = 0;

    @Path("/upload")
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    public Response fileForm(@Context HttpHeaders hh, FormDataMultiPart multiPart) {
        List<FormDataBodyPart> fields = multiPart.getFields("file");
        boolean success = true;
        boolean started = false;
        StringBuffer errorMessages = new StringBuffer();
        String uploadDir = uploadDirName();
        String base = "http://gld01.w3.org/";
        Model merge = ModelFactory.createDefaultModel();

        for(FormDataBodyPart field : fields){
            InputStream uploadedInputStream       = field.getValueAs(InputStream.class);
            String filename = field.getContentDisposition().getFileName();
            try {
                String localFile = save(uploadDir, filename, uploadedInputStream);
                String syntax = FileUtils.langXML;
                if (filename.endsWith(".ttl")) {
                    syntax = FileUtils.langTurtle;
                }
                FileManager.get().readModel(merge, localFile, base, syntax);
                log.info("Saved upload " + filename + " to " + uploadDir);
                started = true;
            } catch (Throwable t) {
                success = false;
                errorMessages.append("<p>" + filename + " - " + t.getMessage() + "</p>");
            }
        }
        // Save merge for actual processing
        try {
            FileOutputStream out = new FileOutputStream(uploadDir + File.separator + "merge.ttl");
            merge.write(out, FileUtils.langTurtle);
            out.close();
        } catch (IOException e) {
            success = false;
            errorMessages.append("<p>Internal error saving merge - " + e.getMessage() + "</p>");
        }
        if (!success) {
            throw new WebApiException(Response.Status.BAD_REQUEST, errorMessages.toString());
        }
        if (!started) {
            throw new WebApiException(Response.Status.BAD_REQUEST, "No file uploaded");
        }

        return Response.ok().type("text/plain").entity( NameUtils.splitAfterLast(uploadDir, "/") ).build();
    }

    private String uploadDirName() {
        String time = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format( new Date().getTime() );
        return String.format("%s/uploads/upload-%s-%s", Config.get().getDirbase(), time, count++);
    }

    /**
     * Preserve the uploaded file in its source form.
     * @return the file created
     */
    private String save(String dirname, String filename, InputStream is) throws IOException {
        FileUtil.ensureDir(dirname);
        String fname = dirname + File.separator + filename;

        OutputStream os = new FileOutputStream(fname);
//        int len = 0;
        byte[] buf = new byte[1024];
        int n;

        while ((n = is.read(buf, 0, buf.length)) >= 0) {
            os.write( buf, 0, n );
//            len += n;
        }
        is.close();
        return fname;
    }

}
