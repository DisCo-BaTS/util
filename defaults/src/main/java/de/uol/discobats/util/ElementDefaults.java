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
 # Module: defaults                                                                                    #
 # File: ElementDefaults.java                                                                          #
 # Last Updated: 2026-02-17 21:58:03                                                                   #
 ######################################################################################################*/

package de.uol.discobats.util;

/**
 * TODO javadoc (class description)
 *
 * @version 1
 * @author David Reiher (https://github.com/dvdrhr)
 */
public record ElementDefaults() {

    public final static Integer DEFAULT_INT = 0;
    public final static String DEFAULT_STRING = "N/A";
    public final static Double  DEFAULT_DOUBLE = 0.0D;
    public final static Float DEFAULT_FLOAT = 0.0F;
    public final static Long DEFAULT_LONG = 0L;
    public final static Boolean DEFAULT_BOOLEAN = false;

    public final static int TIMESTEP_SIZE = 1;
    public final static int COMPONENT_EXECUTION_GROUP = 1;

    public final static String NAME = DEFAULT_STRING;

    public final static double POSITION_LAT = DEFAULT_DOUBLE;
    public final static double POSITION_LNG = DEFAULT_DOUBLE;
    public final static double POSITION_ALT = DEFAULT_DOUBLE;
    public final static double ROTATION = DEFAULT_DOUBLE;

    public final static boolean PHYSICAL = false;
    public final static boolean DYNAMIC = false;
    public final static boolean LOCAL = false;

    public static boolean isDefault(Object object) {
        return switch (object.getClass().getSimpleName()) {
            case "String" -> object.equals(DEFAULT_STRING);
            case "Integer" -> object.equals(DEFAULT_INT);
            case "Double" -> object.equals(DEFAULT_DOUBLE);
            case "Float" -> object.equals(DEFAULT_FLOAT);
            case "Long" -> object.equals(DEFAULT_LONG);
            default -> false;
        };
    }

}