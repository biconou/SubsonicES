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
package net.sourceforge.subsonic.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.schema.Schema;
import net.sourceforge.subsonic.dao.schema.Schema25;
import net.sourceforge.subsonic.dao.schema.Schema26;
import net.sourceforge.subsonic.dao.schema.Schema27;
import net.sourceforge.subsonic.dao.schema.Schema28;
import net.sourceforge.subsonic.dao.schema.Schema29;
import net.sourceforge.subsonic.dao.schema.Schema30;
import net.sourceforge.subsonic.dao.schema.Schema31;
import net.sourceforge.subsonic.dao.schema.Schema32;
import net.sourceforge.subsonic.dao.schema.Schema33;
import net.sourceforge.subsonic.dao.schema.Schema34;
import net.sourceforge.subsonic.dao.schema.Schema35;
import net.sourceforge.subsonic.dao.schema.Schema36;
import net.sourceforge.subsonic.dao.schema.Schema37;
import net.sourceforge.subsonic.dao.schema.Schema38;
import net.sourceforge.subsonic.dao.schema.Schema40;
import net.sourceforge.subsonic.dao.schema.Schema43;
import net.sourceforge.subsonic.dao.schema.Schema45;
import net.sourceforge.subsonic.dao.schema.Schema46;
import net.sourceforge.subsonic.dao.schema.Schema47;
import net.sourceforge.subsonic.dao.schema.Schema49;
import net.sourceforge.subsonic.dao.schema.Schema50;
import net.sourceforge.subsonic.dao.schema.Schema51;
import net.sourceforge.subsonic.dao.schema.Schema52;
import net.sourceforge.subsonic.service.SettingsService;

/**
 * DAO helper class which creates the data source, and updates the database schema.
 *
 * @author Sindre Mehus
 */
public interface DaoHelper {

    /**
     * Returns a JDBC template for performing database operations.
     *
     * @return A JDBC template.
     */
    JdbcTemplate getJdbcTemplate();

    /**
     * Returns a named parameter JDBC template for performing database operations.
     *
     * @return A named parameter JDBC template.
     */
    NamedParameterJdbcTemplate getNamedParameterJdbcTemplate();

}
