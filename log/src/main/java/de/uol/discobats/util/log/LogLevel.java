/*######################################################################################################
 # This file is part of the Distributed Component-Based Traffic Simulation (DisCo-BaTS) project.       #
 # Copyright (C) 2026 David Reiher <https://github.com/dvdrhr>                                         #
 #                                                                                                     #
 # This program is free software: you can redistribute it and/or modify it under the terms of the      #
 # GNU Lesser General Public License version 3 as published by the Free Software Foundation            #
 # This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;           #
 # without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           #
 # See the GNU Lesser General Public License version 3 for more details.                               #
 # You should have received a copy of the GNU Lesser General Public License along with this program.   #
 # If not, see <https://www.gnu.org/licenses/lgpl+gpl-3.0.txt/>.                                       #
 #                                                                                                     #
 # Module: log                                                                                         #
 # File: LogLevel.java                                                                                 #
 # Last Updated: 2026-02-17 20:25:05                                                                   #
 ######################################################################################################*/

package de.uol.discobats.util.log;

import org.apache.logging.log4j.Level;

public enum LogLevel {

    VERBOSE(Level.getLevel("ALL")),
    DEBUG(Level.getLevel("DEBUG")),
    INFO(Level.getLevel("INFO")),
    SIMULATION(Level.forName("SIM", 350)),
    WARN(Level.getLevel("WARN")),
    DEMO(Level.forName("DEMO", 450)),
    ERROR(Level.getLevel("ERROR")),
    FATAL(Level.getLevel("FATAL"));

    private final Level log4jLevel;

    LogLevel(Level log4jLevel) {
        this.log4jLevel = log4jLevel;
    }

    public Level get() {
        return this.log4jLevel;
    }

}
