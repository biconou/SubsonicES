/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package com.github.biconou.subsonic.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * 
 *
 * @author Rémi Cocula
 */
public class CMusController extends AbstractController {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CMusController.class);
	
    private CMusService cmusService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	LOG.debug("Begin handleRequestInternal");
    	//cmusService.cmusStatusChanged();
    	
    	return null;
    }


    public void setCmusService(CMusService cmusService) {
        this.cmusService = cmusService;
    }
}
