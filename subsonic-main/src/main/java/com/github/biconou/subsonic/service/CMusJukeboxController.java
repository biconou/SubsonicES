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

import net.sourceforge.subsonic.service.IJukeboxService;

import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.github.biconou.cmus.CMusController;

/**
 * 
 *
 * @author R�mi Cocula
 */
public class CMusJukeboxController extends AbstractController {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CMusJukeboxController.class);
	
    private IJukeboxService jukeboxService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	LOG.debug("Begin handleRequestInternal");
    	CMusJukeboxService CMUSService = (CMusJukeboxService)jukeboxService;
    	CMUSService.cmusStatusChanged();
    	
    	return null;
    }


    public void setJukeboxService(IJukeboxService jukeboxService) {
        this.jukeboxService = jukeboxService;
    }
}
