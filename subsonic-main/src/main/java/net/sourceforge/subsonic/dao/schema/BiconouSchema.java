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
package net.sourceforge.subsonic.dao.schema;

import org.springframework.jdbc.core.JdbcTemplate;

import net.sourceforge.subsonic.Logger;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Biconou.
 *
 * @author Sindre Mehus
 */
public class BiconouSchema extends Schema {

    private static final Logger LOG = Logger.getLogger(BiconouSchema.class);

    @Override
    public void execute(JdbcTemplate template) {


        if (!columnExists(template, "cmushost", "player")) {
            LOG.info("Database column 'player.cmushost' not found.  Creating it.");
            template.execute("alter table player add cmushost varchar");
            LOG.info("Database column 'player.cmushost' was added successfully.");
        }

        if (!columnExists(template, "cmusport", "player")) {
            LOG.info("Database column 'player.cmusport' not found.  Creating it.");
            template.execute("alter table player add cmusport int");
            LOG.info("Database column 'player.cmusport' was added successfully.");
        }

        if (!columnExists(template, "cmuspassword", "player")) {
            LOG.info("Database column 'player.cmuspassword' not found.  Creating it.");
            template.execute("alter table player add cmuspassword varchar");
            LOG.info("Database column 'player.cmuspassword' was added successfully.");
        }
        
        //

        if (!tableExists(template, "cmus_player_folder_path")) {
            LOG.info("Database table 'cmus_player_folder_path' not found.  Creating it.");
            template.execute("create table cmus_player_folder_path (" +
                    "music_folder_id int not null," +
                    "player_id int not null," +
                    "cmuspath varchar not null)");

            LOG.info("Database table 'cmus_player_folder_path' was created successfully.");
        }

    }
}