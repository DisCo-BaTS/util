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
 # Module: json                                                                                        #
 # File: JSONUtils.java                                                                                #
 # Last Updated: 2026-02-17 21:58:03                                                                   #
 ######################################################################################################*/

package de.uol.discobats.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static de.uol.discobats.util.log.LogLevel.WARN;
import static de.uol.discobats.util.log.LogService.*;

/**
 * TODO javadoc
 *
 * @version 1
 * @author David Reiher (https://github.com/dvdrhr)
 */
public class JSONUtils {

    public final static String DEFAULT_PATH_SEPERATOR = ".";
    public final static String DEFAULT_PATH_LIST_OPENING = "[";
    public final static String DEFAULT_PATH_LIST_CLOSING = "]";
    public final static String DEFAULT_PATH_LIST_WILDCARD = "*";
    public final static String DEFAULT_PATH_LIST_FIELDVALUELINK = "=";

    public static int[] getIndexes(JSONArray jsonArray) {
        int arrayLength = jsonArray.length();
        int[] indexes = new int[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            indexes[i] = i;
        }
        return indexes;
    }

    public static List<Object> getValueList(JSONObject jsonObject, String path) {
        return getValueList(jsonObject, path, DEFAULT_PATH_SEPERATOR);
    }

    public static List<Object> getValueList(JSONObject jsonObject, String path, String separator) {

        if (jsonObject == null) {
            log(WARN, "tried to get the value of jsonObject=null, returning null");
            return null;
        }
        if (StringUtils.isBlank(path)) {
            log(WARN, "tried to get the value of path=null/empty, returning null");
            return null;
        }

        List<Object> values = new ArrayList<>();

        String[] arrayPathSegments = getArrayPathSegments(path, separator);
        final String preArrayPath = arrayPathSegments[0];
        final String arrayFieldName = arrayPathSegments[1];
        final String postArrayPath = arrayPathSegments[2];

        JSONObject preArrayJsonObject = getJSONObject(jsonObject, preArrayPath);
        if (preArrayJsonObject == null) {
            return values;
        }
        JSONArray jsonArray = preArrayJsonObject.getJSONArray(arrayFieldName);
        if (jsonArray == null) {
            return values;
        }

        for (Object arrayEntry : jsonArray) {
            JSONObject arrayJsonObject = (JSONObject) arrayEntry;
            values.add(getValue(jsonObject, postArrayPath));
        }

        return values;
    }

    public static Object getValue(JSONObject jsonObject, String path) {
        return getValue(jsonObject, path, DEFAULT_PATH_SEPERATOR);
    }

    public static Object getValue(JSONObject jsonObject, String path, String seperator) {
        String[] pathSegments = StringUtils.split(path, seperator);
        return getValue(jsonObject, pathSegments);
    }

    public static Object getValue(JSONObject jsonObject, String[] path) {

        if (jsonObject == null) {
            log(WARN, "tried to get the value of jsonObject=null, returning null");
            return null;
        }
        if (path == null || path.length == 0) {
            log(WARN, "tried to get the value of path=null/empty, returning null");
            return null;
        }
        if (hasWildcardArraySegment(path)) {
            throw (logAndReturn(new IllegalArgumentException("can't get single value of a json path that contains an array")));
        }

        JSONObject jsonObjectParent;
        String preValueFieldPath = "";
        for (int i = 0; i < path.length - 1; i++) {
            final String pathSegment = path[i];
            preValueFieldPath = StringUtils.join(preValueFieldPath,
                                                 i > 0 ? DEFAULT_PATH_SEPERATOR : "",
                                                 pathSegment);
        }

        jsonObject = getJSONObject(jsonObject, preValueFieldPath);
        if (jsonObject != null) {
            return jsonObject.opt(path[path.length - 1]);
        } else {
            return null;
        }
    }

    public static JSONObject getJSONObject(JSONObject jsonObject, String path) {

        if (jsonObject == null) {
            log(WARN, "tried to get the value of jsonObject=null, returning null");
            return null;
        }
        if (StringUtils.isBlank(path)) {
            log(WARN, "tried to get the value of path=null/empty, returning null");
            return null;
        }

        String[] pathSegments = getPathSegments(path, DEFAULT_PATH_SEPERATOR);
        JSONObject jsonObjectParent;
        for (final String pathSegment : pathSegments) {
            jsonObjectParent = jsonObject;

            if (hasNumericIdentifierArray(pathSegment)) { // path segment contains 'arrayField[number]'
                // get the name of the field containing the array
                String fieldName = getArrayName(pathSegment);
                // get the index of the object to look for in the array
                int numericIdentifier = getNumericArrayIdentifier(pathSegment);
                if (numericIdentifier < 0) {
                    log(WARN, "numeric identifier '{}' is less than 0 -> returning null", numericIdentifier);
                    return null;
                }
                // get the array
                JSONArray jsonArray = jsonObject.optJSONArray(fieldName);
                if (jsonArray == null) {
                    logWithArgs(WARN, "no array field with name '{}' could be found in json object {} -> returning null", fieldName, jsonObject);
                    return null;
                }
                // get the object at the given position
                jsonObject = jsonArray.optJSONObject(numericIdentifier);

            } else if (hasFieldValueIdentifierArray(pathSegment)) { // path segment contains 'arrayField[field=value]'
                // get the name of the field containing the array
                String fieldName = getArrayName(pathSegment);
                // get the key-value pair with which to search for the object in the array
                String[] keyValueIdentifiers = getFieldAndValueIdentifiers(pathSegment);
                if (keyValueIdentifiers == null
                    || keyValueIdentifiers.length < 2
                    || StringUtils.isBlank(keyValueIdentifiers[0])
                    || StringUtils.isBlank(keyValueIdentifiers[1])) {
                    logWithArgs(WARN, "could not extract a valid key-value pair from path segment '{}' -> returning null", pathSegment);
                    return null;
                }
                // get the array
                JSONArray jsonArray = jsonObject.optJSONArray(fieldName);
                if (jsonArray == null) {
                    logWithArgs(WARN, "no array field with name '{}' could be found in json object {} -> returning null", fieldName, jsonObject);
                    return null;
                }
                // find the object of which the field identified by the given field name contains the given value
                String identifierKey = keyValueIdentifiers[0];
                String identifierValue = keyValueIdentifiers[1];
                for (Object arrayObject : jsonArray) { // check every element in the array
                    if (arrayObject instanceof JSONObject arrayJsonObject) { // it has to be a JSONObject
                        Object fieldValue = arrayJsonObject.opt(identifierKey); // get the value of the field identified by the given key
                        if (fieldValue != null) {
                            if (fieldValue.toString().equals(identifierValue)) { // check every value datatype using toString for easier checking
                                jsonObject = arrayJsonObject;
                                break;
                            }
                        }
                    }
                }

            } else { // path segment is a plain old segment without any array identifiers -> just get the JSONObject associated with the segment-field
                jsonObject = jsonObject.optJSONObject(pathSegment);
            }

            // if the jsonObject is still null, there probably is a null value or an object of an type incompatible to JSONObject
            if (jsonObject == null) {
                logWithArgs(WARN, "key / path segment {} does not exist in json object {} -> returning null", pathSegment, jsonObjectParent);
                return null;
            }
        }

        // return the JSONObject that was found at the end of the given path (the inner field identified by the last segment)
        return jsonObject;
    }

    public static JSONObject putValue(JSONObject jsonObject, String path, Object value) {
        return putValue(jsonObject, path, DEFAULT_PATH_SEPERATOR, value);
    }

    public static JSONObject putValue(JSONObject jsonObject, String path, String seperator, Object value) {
        final JSONObject innerJsonObject = createNestedJsonObjects(jsonObject, path, seperator);
        final int lastSeperatorIndex = Strings.CI.lastIndexOf(path, seperator);
        final String fieldNameSegment = StringUtils.substring(path, lastSeperatorIndex + 1);
        innerJsonObject.put(fieldNameSegment, value);
        return innerJsonObject;
    }

    public static JSONObject createNestedJsonObjects(JSONObject jsonObject, String path) {
        return createNestedJsonObjects(jsonObject, path, DEFAULT_PATH_SEPERATOR);
    }

    public static JSONObject createNestedJsonObjects(JSONObject jsonObject, String path, String seperator) {
        final String[] pathSegments = StringUtils.split(path, seperator);
        JSONObject currentJsonObject = jsonObject;
        for (int i = 0; i < pathSegments.length - 1; i++) {
            String pathSegment = pathSegments[i];
            if (!currentJsonObject.has(pathSegment)) {
                JSONObject nestedJsonObject = new JSONObject();
                currentJsonObject.put(pathSegment, nestedJsonObject);
                currentJsonObject = nestedJsonObject;
            } else if (currentJsonObject.get(pathSegment) instanceof JSONObject) {
                currentJsonObject = (JSONObject) currentJsonObject.get(pathSegment);
            } else {
                logAndThrow(new RuntimeException("cant create inner JSONObject for segment " + pathSegment + " of path " + path
                                                 + " because there is already an object with the same name that isn't a JSONObject"));
            }
        }
        return currentJsonObject;
    }

    public static List<JSONObject> putValues(JSONObject jsonObject, String path, List<Object> values) {
        return putValues(jsonObject, path, DEFAULT_PATH_SEPERATOR, values);
    }

    public static List<JSONObject> putValues(JSONObject jsonObject, String path, String seperator, List<Object> values) {

        String[] arrayPathSegments = getArrayPathSegments(path, seperator);
        final String preArrayPath = arrayPathSegments[0];
        final String arrayFieldName = arrayPathSegments[1];
        final String postArrayPath = arrayPathSegments[2];

        JSONObject arrayParentJsonObject = JSONUtils.createNestedJsonObjects(jsonObject,
                                                                             preArrayPath,
                                                                             seperator);

        JSONArray jsonArray;
        if (arrayParentJsonObject.has(arrayFieldName)) {
            jsonArray = (JSONArray) arrayParentJsonObject.get(arrayFieldName);
        } else {
            jsonArray = new JSONArray();
            arrayParentJsonObject.put(arrayFieldName, jsonArray);
        }

        List<JSONObject> listJsonObjects = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            JSONObject listJsonObject = jsonArray.length() > i ? (JSONObject) jsonArray.get(i) : null;
            if (listJsonObject == null) {
                listJsonObject = new JSONObject();
                jsonArray.put(listJsonObject);
            }
            JSONObject innerJsonObject = JSONUtils.putValue(listJsonObject,
                                                            postArrayPath,
                                                            seperator,
                                                            values.get(i));
            listJsonObjects.add(innerJsonObject);
        }
        return listJsonObjects;
    }

    public static String[] getPathSegments(String path) {
        return getPathSegments(path, DEFAULT_PATH_SEPERATOR);
    }

    public static String[] getPathSegments(String path, String seperator) {
        return StringUtils.split(path, seperator);
    }

    public static String[] getArrayPathSegments(String path) {
        return getArrayPathSegments(path, DEFAULT_PATH_SEPERATOR);
    }

    public static String[] getArrayPathSegments(String path, String seperator) {
        final List<String> preArrayPathSegments = new ArrayList<>();
        final List<String> postArrayPathSegments = new ArrayList<>();

        String arrayFieldName = null;
        for (String pathSegment : StringUtils.split(path, seperator)) {
            if (pathSegment.contains(DEFAULT_PATH_LIST_OPENING)
                && pathSegment.contains(DEFAULT_PATH_LIST_CLOSING)) {
                // dont add the array path segment to any of the two lists
                arrayFieldName = StringUtils.substringBefore(pathSegment, DEFAULT_PATH_LIST_OPENING);
            } else if (arrayFieldName == null) {
                // collect all path segments located before the array
                preArrayPathSegments.add(pathSegment);
            } else {
                // collect all path segments located behind the array
                postArrayPathSegments.add(pathSegment);
            }
        }

        final String preArrayPath = StringUtils.join(preArrayPathSegments, seperator);
        final String postArrayPath = StringUtils.join(postArrayPathSegments, seperator);
        return new String[]{preArrayPath, arrayFieldName, postArrayPath};
    }

    public static boolean hasFieldValueIdentifierArray(String pathSegment) {
        String arrayIdentifier = getArrayIdentifier(pathSegment);
        if (!arrayIdentifier.contains(DEFAULT_PATH_LIST_FIELDVALUELINK)) {
            return false;
        }
        String[] fieldAndValueIdentifiers = getFieldAndValueFromIdentifier(arrayIdentifier);
        return fieldAndValueIdentifiers != null;
    }

    public static String[] getFieldAndValueIdentifiers(String pathSegment) {
        String arrayIdentifier = getArrayIdentifier(pathSegment);
        return getFieldAndValueFromIdentifier(arrayIdentifier);
    }

    private static String[] getFieldAndValueFromIdentifier(String arrayIdentifier) {
        String[] fieldAndValue = StringUtils.split(arrayIdentifier, DEFAULT_PATH_LIST_FIELDVALUELINK);
        if (fieldAndValue.length != 2) {
            return null;
        } else {
            return fieldAndValue;
        }
    }

    public static boolean hasNumericIdentifierArray(String pathSegment) {

        String arrayIdentifier = getArrayIdentifier(pathSegment);

        if (StringUtils.isBlank(arrayIdentifier)) {
            return false;
        }

        arrayIdentifier = StringUtils.split(arrayIdentifier, ".")[0];
        arrayIdentifier = StringUtils.split(arrayIdentifier, ",")[0];

        if (StringUtils.isBlank(arrayIdentifier)) {
            return false;
        }

        return StringUtils.isNumeric(arrayIdentifier);
    }

    public static int getNumericArrayIdentifier(String pathSegment) {
        String arrayIdentifier = getArrayIdentifier(pathSegment);
        if (!StringUtils.isNumeric(arrayIdentifier)) {
            return -1;
        }
        return Integer.parseInt(arrayIdentifier);
    }

    private static String getArrayIdentifier(String pathSegment) {
        if (!hasArray(pathSegment)) {
            return "";
        }

        String[] arrayIdentifiers = StringUtils.substringsBetween(pathSegment, DEFAULT_PATH_LIST_OPENING, DEFAULT_PATH_LIST_CLOSING);
        if (arrayIdentifiers.length == 0) {
            return "";
        }

        String arrayIdentifier = arrayIdentifiers[0].trim();
        if (StringUtils.isBlank(arrayIdentifier)) {
            return "";
        }

        return arrayIdentifier;
    }

    private static String getArrayName(String pathSegment) {
        if (!hasArray(pathSegment)) {
            return "";
        }
        return StringUtils.substringBefore(pathSegment, DEFAULT_PATH_LIST_OPENING).trim();
    }

    public static boolean hasArray(String pathSegment) {
        if (!Strings.CI.contains(pathSegment, DEFAULT_PATH_LIST_OPENING)
            || !Strings.CI.contains(pathSegment, DEFAULT_PATH_LIST_CLOSING)) {
            return false;
        }
        return true;
    }

    public static boolean hasArraySegment(String[] pathSegments) {
        for (String pathSegment : pathSegments) {
            if (pathSegment.contains(DEFAULT_PATH_LIST_OPENING)
                && pathSegment.contains(DEFAULT_PATH_LIST_CLOSING)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasArraySegment(String path) {
        return hasArraySegment(path, DEFAULT_PATH_SEPERATOR);
    }

    public static boolean hasArraySegment(String path, String seperator) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        if (StringUtils.isEmpty(seperator)) {
            throw (logAndReturn(new IllegalArgumentException("seperator must not be null or empty")));
        }
        return hasArraySegment(StringUtils.split(path, seperator));
    }

    public static int countWildcardArraySegments(String path) {
        return countWildcardArraySegments(path, DEFAULT_PATH_SEPERATOR);
    }

    public static int countWildcardArraySegments(String path, String seperator) {
        if (StringUtils.isBlank(path)) {
            return -1;
        }
        if (StringUtils.isEmpty(seperator)) {
            throw (logAndReturn(new IllegalArgumentException("seperator must not be null or empty")));
        }
        return countWildcardArraySegments(StringUtils.split(path, seperator));
    }

    public static int countWildcardArraySegments(String[] pathSegments) {
        int counter = 0;
        for (String pathSegment : pathSegments) {
            if (pathSegment.contains(DEFAULT_PATH_LIST_OPENING + DEFAULT_PATH_LIST_WILDCARD + DEFAULT_PATH_LIST_CLOSING)) {
                counter++;
            }
        }
        return counter;
    }

    public static boolean hasWildcardArraySegment(String path) {
        return hasWildcardArraySegment(path, DEFAULT_PATH_SEPERATOR);
    }

    public static boolean hasWildcardArraySegment(String path, String seperator) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        if (StringUtils.isEmpty(seperator)) {
            throw (logAndReturn(new IllegalArgumentException("seperator must not be null or empty")));
        }
        return hasWildcardArraySegment(StringUtils.split(path, seperator));
    }

    public static boolean hasWildcardArraySegment(String[] pathSegments) {
        for (String pathSegment : pathSegments) {
            if (pathSegment.contains(DEFAULT_PATH_LIST_OPENING + DEFAULT_PATH_LIST_WILDCARD + DEFAULT_PATH_LIST_CLOSING)) {
                return true;
            }
        }
        return false;
    }

    public static String getPathStringWithArrayIndex(String path, int index) {
        String[] pathSegments = getArrayPathSegments(path);
        return StringUtils.join(
            pathSegments[0],
            pathSegments[1] + DEFAULT_PATH_LIST_OPENING + index + DEFAULT_PATH_LIST_CLOSING,
            pathSegments[2]
        );
    }

}
