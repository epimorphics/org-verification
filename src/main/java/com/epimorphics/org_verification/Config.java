/******************************************************************
 * File:        Config.java
 * Created by:  Dave Reynolds
 * Created on:  22 May 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.org_verification;

import java.util.Map;

import javax.servlet.ServletContext;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;

public class Config extends ServiceBase implements Service {
    public static final String DIRBASE_PARAM = "dirbase";

    static Config config;
    public static Config get() {
        return config;
    }

    String dirbase;

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        super.init(config, context);
        dirbase = getRequiredParam(DIRBASE_PARAM);
    }


    @Override
    public void postInit() {
        config = this;
    }


    public String getDirbase() {
        return dirbase;
    }

}
