/******************************************************************
 * File:        CSVToTable.java
 * Created by:  Dave Reynolds
 * Created on:  4 Nov 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Hack to convert CSV files to mediawiki tables - used for Org CR reporting.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CSVToTable {

    static final String SRC = "/home/der/projects/w3c/gld/org/usage.csv";
    
    public static void main(String[] args) throws IOException {
        String src = SRC;
        if (args.length > 1) {
            src = args[0];
        }
        FileReader in = new FileReader(src);
        CSVReader reader = new CSVReader(in);
        
        String[] headers = reader.readNext();
        System.out.println("{| class='datatable' style='width: 100%'\n|+\n" + lineWithSep(headers, "!") + "");
        
        while(true) {
            String[] line = reader.readNext();
            if (line == null || line.length == 0) {
                break;
            }
            System.out.println("|-\n" + lineWithSep(line, "|"));
        }
        System.out.println("|}");
        reader.close();
    }
    
    private static String lineWithSep(String[] ents, String sep) {
        StringBuffer buff = new StringBuffer();
        buff.append(sep + " ");
        for (int i = 0; i < ents.length; i++) {
            buff.append(ents[i]);
            if (i < (ents.length - 1)) {
                buff.append(" " + sep + sep + " ");
            }
        }
        return buff.toString();
    }
}
