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
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jena.atlas.json.JsonValue;
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
import com.sun.jersey.multipart.FormDataParam;

@Path("/request")
public class RequestProcessor {
    static final Logger log = LoggerFactory.getLogger( RequestProcessor.class );

    public static final String UPLOAD_DIR = "uploads";

    protected static long count = 0;

    @Path("/upload")
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    public Response fileForm(@Context HttpHeaders hh, FormDataMultiPart multiPart,
            @FormDataParam("limit") int limit) {
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
            log.info(String.format("Upload requested on %s", filename));
            try {
                String localFile = save(uploadDir, filename, uploadedInputStream, limit);
                if (localFile == null) {
                    log.warn("Upload aborted due to long file");
                    errorMessages.append(String.format("<p>File %s too long, uploads limited to %d bytes.</p>", filename, limit));
                    success = false;
                } else {
                    String syntax = FileUtils.langXML;
                    if (filename.endsWith(".ttl")) {
                        syntax = FileUtils.langTurtle;
                    } else if (filename.endsWith(".nt")) {
                        syntax = FileUtils.langNTriple;
                    }
                    FileManager.get().readModel(merge, localFile, base, syntax);
                    log.info("Saved upload " + filename + " to " + uploadDir);
                    started = true;
                }
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
     * @return the file created or null if the preserve failed due to size limits
     */
    private String save(String dirname, String filename, InputStream is, int limit) throws IOException {
        FileUtil.ensureDir(dirname);
        String fname = dirname + File.separator + filename;

        OutputStream os = new FileOutputStream(fname);
        try {
            int len = 0;
            byte[] buf = new byte[1024];
            int n;
    
            while ((n = is.read(buf, 0, buf.length)) >= 0) {
                os.write( buf, 0, n );
                len += n;
                if (len > limit) {
                    os.close();
                    new File(fname).delete();
                    return null;
                }
            }
            return fname;
        } finally {
            is.close();
            os.close();
        }
    }

    /**
     * Save an important upload to the preservation area
     */
    @Path("/save")
    @POST
    public Response saveData(@QueryParam("upload") String upload)  {
        log.info("Preserving upload: " + upload);
        try {
            String src = Config.get().getUploadDir(upload);
            String preserve = Config.get().getPreservationDir(upload);
            FileUtil.ensureDir(preserve);
            FileUtil.copyDirectory( Paths.get(src), Paths.get( preserve) );
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Problem during save", e);
            throw new WebApiException(500, "Internal error - " + e);
        }
    }
    
    /**
     * Run a Data Cube integrity check
     */
    @Path("/validate")
    @GET
    public Response validate(@QueryParam("upload") String upload, @QueryParam("check") String ic)  {
        JsonValue result = DataCube.get().validate(ic, upload);
        return Response.ok().type(MediaType.APPLICATION_JSON).entity( result.toString() ).build();
    }
}
