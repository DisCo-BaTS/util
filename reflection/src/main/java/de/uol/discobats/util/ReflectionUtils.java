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
 # Module: reflection                                                                                  #
 # File: ReflectionUtils.java                                                                          #
 # Last Updated: 2026-02-17 21:57:58                                                                   #
 ######################################################################################################*/

package de.uol.discobats.util;

import com.google.common.primitives.Primitives;
import de.uol.discobats.metamodel.simulation.AbstractScenarioSpecificationItem;
import de.uol.discobats.metamodel.simulation.property.ImmutableProperty;
import de.uol.discobats.metamodel.simulation.property.MutableProperty;
import de.uol.discobats.metamodel.simulation.property.Property;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;

import static de.uol.discobats.metamodel.jaxb.AdapterUtils.getValueOfProperty;
import static de.uol.discobats.metamodel.simulation.property.MutableProperty.of;
import static de.uol.discobats.util.log.LogLevel.WARN;
import static de.uol.discobats.util.log.LogService.*;

/**
 * TODO javadoc (class description)
 *
 * @author David Reiher (https://github.com/dvdrhr)
 * @version 1
 */
public class ReflectionUtils {

    /**
     * generates a list of all the classes fields, including all superclasses.
     *
     * @param classOfScenarioItem Class for which the list is going to be generated
     * @return list of all attributes in the class
     */
    public static ArrayList<Field> getFieldsOfScenarioSpecItemClass(Class<?> classOfScenarioItem) {
        ArrayList<Field> allObjectFields = new ArrayList<>(
            Arrays.asList(classOfScenarioItem.getDeclaredFields())
        );
        Class<?> superClass = classOfScenarioItem.getSuperclass();
        while (superClass != null && !superClass.equals(AbstractScenarioSpecificationItem.class)) {
            allObjectFields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        return allObjectFields;
    }

    public static ArrayList<Field> getAllFieldsOfClass(Class<?> clazz) {
        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        Class<?> superClazz = clazz.getSuperclass();
        while (superClazz != null) {
            fields.addAll(Arrays.asList(superClazz.getDeclaredFields()));
            superClazz = superClazz.getSuperclass();
        }
        return fields;
    }

    public static void setPropertyValueOfObject(Object object, String propertyName, Object value) {
        if (object == null) {
            throw (logAndReturn(new IllegalArgumentException("object can't be null")));
        }
        if (StringUtils.isBlank(propertyName)) {
            throw (logAndReturn(new IllegalArgumentException("prorpertyName can't be null or blank")));
        }

    }

    public static Field getFieldOfObjectByName(Object object, String fieldName) {
        return getFieldOfClassByName(object.getClass(), fieldName);
    }

    public static Field getFieldOfClassByName(Class<?> clazz, String fieldName) {
        return getFieldFromFieldListByName(getAllFieldsOfClass(clazz), fieldName);
    }

    public static ArrayList<Field> getAllFieldsOfObject(Object object) {
        return getAllFieldsOfClass(object.getClass());
    }

    public static Field getFieldOfScenarioItemClassByName(Class<?> clazz, String fieldName) {
        return getFieldFromFieldListByName(getFieldsOfScenarioSpecItemClass(clazz), fieldName);
    }

    public static Field getFieldFromFieldListByName(List<Field> fieldList, String fieldName) {
        return fieldList.stream()
                        .filter(f -> f.getName().equalsIgnoreCase(fieldName))
                        .findFirst()
                        .orElseThrow();
    }

    public static Field getDeepFieldOfObjectByName(Object object, String fieldName) {
        return getDeepFieldOfClassByName(object.getClass(), fieldName);
    }

    public static Field getDeepFieldOfClassByName(Class<?> clazz, String fieldPath) {
        String[] attributeNamesSplitByDot = fieldPath.split("\\.");

        // get the field representing the attribute inside the object
        Field attributeField = ReflectionUtils.getFieldOfClassByName(clazz, attributeNamesSplitByDot[0]);

        // attributes are always of type SimulationAttribute<T>, we therefore have to get the actual generic type
        Class<?> attributeType = ReflectionUtils.getGenericTypeOfField(attributeField);

        // if the attribute is a compound attribute (indicated by attribute names chained with a dot in between)
        if (attributeNamesSplitByDot.length > 1) {
            // ... we have to get we have to go along the data structure according to the chained name until we arrive at the final field
            for (int i = 1; i < attributeNamesSplitByDot.length; i++) {
                String attributeName = attributeNamesSplitByDot[i];
                attributeField = ReflectionUtils.getFieldOfClassByName(attributeType, attributeName);
            }
        }
        return attributeField;
    }

    public static Property<?> setObjectsPropertyValueByString(Object objectToUpdate, String propertyName, String value) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        Map.Entry<Object, Field> objectAndFieldToUpdate = ReflectionUtils.getCreateDeepValueObjectAndFieldByPath(objectToUpdate, propertyName);
        Field fieldToUpdate = objectAndFieldToUpdate.getValue();
        Class<?> fieldType = fieldToUpdate.getType();
        Class<?> valueType = ReflectionUtils.getGenericTypeOfField(fieldToUpdate);

        Method valueOf = valueType.getMethod("valueOf", String.class);
        Object valueObject = valueOf.invoke(null, value);
        return setObjectsPropertyValue(objectToUpdate, propertyName, valueType, valueObject);
    }

    @SuppressWarnings("rawtypes")
    public static Property<?> setObjectsPropertyValue(Object objectToUpdate, String propertyName, Class<?> valueType, Object value) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {

        Map.Entry<Object, Field> objectAndFieldToUpdate = ReflectionUtils.getCreateDeepValueObjectAndFieldByPath(objectToUpdate, propertyName);
        Field fieldToUpdate = objectAndFieldToUpdate.getValue();
        Object fieldHoldingObject = objectAndFieldToUpdate.getKey();
        Class<?> fieldType = fieldToUpdate.getType();
        Class<?> propertyValueType = ReflectionUtils.getGenericTypeOfField(fieldToUpdate);

        @Nullable
        Property<Object> propertyToUpdate = (Property<Object>) fieldToUpdate.get(fieldHoldingObject);

        // if the target value is of type enum and the given value is of type string
        // create the enum value based on the string value
        if (valueType.isEnum() && !value.getClass().isEnum()) {
            Method valueOf = valueType.getMethod("valueOf", String.class);
            value = valueOf.invoke(null, (String) value);
        }

        // make sure the incoming value has the class that our property expects then set it
        if (valueType.equals(propertyValueType)) {
            fieldToUpdate.setAccessible(true);

            if (propertyToUpdate == null) {
                // field is null: create new Property, assign property to field of object
                if (fieldType.equals(MutableProperty.class)) {
                    propertyToUpdate = of(value, fieldToUpdate.getName());
                } else {
                    propertyToUpdate = ImmutableProperty.of(value, fieldToUpdate.getName());
                }
                // assign the created property instance to the scenario element object
                fieldToUpdate.setAccessible(true);
                fieldToUpdate.set(fieldHoldingObject, propertyToUpdate);
                fieldToUpdate.setAccessible(false);

            } else {
                // field is not null: update existing property
                if (fieldType.equals(MutableProperty.class)) {
                    // set property name to field name (the name is used to find properties in some places)
                    propertyToUpdate.setName(fieldToUpdate.getName());
                    // set value
                    ((MutableProperty) propertyToUpdate).setSingleValue(value);
                } else {
                    // property is immutable: check if it was ever set to a meaningful value up to this point
                    if (propertyToUpdate.getValue() == null                         // value isn't set: it's probably a dummy property, create a new one with the received value
                        || propertyToUpdate.getValue().equals(StringUtils.EMPTY)    // OR value is a default value: it's probably a dummy property / value, update the value
                        || ElementDefaults.isDefault(propertyToUpdate.getValue())) {

                        // set property name to field name (the name is used to find properties in some places)
                        Field nameField = getFieldOfClassByName(propertyToUpdate.getClass(), "name");
                        nameField.setAccessible(true);
                        nameField.set(propertyToUpdate, fieldToUpdate.getName());

                        // set the value
                        Field valueListField = getFieldOfClassByName(propertyToUpdate.getClass(), "values");
                        valueListField.setAccessible(true);
                        List<Object> valueList = (List<Object>) valueListField.get(propertyToUpdate);
                        valueList.clear();
                        valueList.add(value);

                    } else {
                        // value replacing a meaningful value with a new one isn't allowed for immutable properties: do nothing and warn
                        logWithArgs(WARN, "the property '{}' of the object with '{}' can't be updated to value '{}' because it is of type '{}'", propertyName, objectToUpdate, propertyToUpdate.getValue(), propertyToUpdate.getClass().getSimpleName());
                    }
                }

            }

        } else if (valueType == ArrayList.class) {
            // if the target is an arrayList, get the list and add the data to it
            if (propertyToUpdate != null) {
                ArrayList<Object> targetList = (ArrayList<Object>) propertyToUpdate.getValue();
                targetList.add(value);
            }
        }
        if (propertyToUpdate != null) {
            propertyToUpdate.setUpdatePending(true);
        }
        return propertyToUpdate;
    }

    @SuppressWarnings({"rawtypes"})
    public static Map.Entry<Object, Field> getCreateDeepValueObjectAndFieldByPath(Object object, String fieldPath) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        String[] attributeNamesSplitByDot = fieldPath.split("\\.");

        Class<?> clazz = object.getClass();
        String attributeName = attributeNamesSplitByDot[0];
        Field attributeField = ReflectionUtils.getFieldOfClassByName(clazz, attributeNamesSplitByDot[0]);
        attributeField.setAccessible(true);

        Class<?> attributeType = ReflectionUtils.getGenericTypeOfField(attributeField);

        // TODO extract duplicate code
        if (attributeField.get(object) == null) {
            Property newProperty = (Property) getEmptyConstructorIfPresent(attributeField.getType()).newInstance();
            newProperty.setName(attributeName);
            if (Primitives.isWrapperType(attributeType)) {
                // nothing to do
            } else if (attributeType.isEnum()) {
                // nothing to do
            } else {
                Field propertyValueField = ReflectionUtils.getFieldOfClassByName(newProperty.getClass(), "values");
                propertyValueField.setAccessible(true);
                ((List) propertyValueField.get(newProperty)).add(getEmptyConstructorIfPresent(attributeType).newInstance());
                propertyValueField.setAccessible(false);

            }
            attributeField.set(object, newProperty);
        }

        // if the property value is a complex nested property (indicated by attribute names chained with a dot in between)
        if (attributeNamesSplitByDot.length > 1) {
            // ... we have to go down the data structure according to the chained name until we arrive at the final field
            for (int i = 1; i < attributeNamesSplitByDot.length; i++) {
                object = getValueOfProperty((Property<?>) attributeField.get(object));
                clazz = object.getClass();
                attributeName = attributeNamesSplitByDot[i];
                attributeField = ReflectionUtils.getFieldOfClassByName(clazz, attributeName);
                attributeField.setAccessible(true);
                if (attributeField.get(object) == null) {
                    Property newProperty = (Property) getEmptyConstructorIfPresent(attributeField.getType()).newInstance();
                    newProperty.setName(attributeName);
                    if (Primitives.isWrapperType(attributeType)) {
                        // newSimulationAttribute.setSingleValue(0);
                    } else if (attributeType.isEnum()) {
                        // nothing to do
                    } else {
                        Field propertyValueField = ReflectionUtils.getFieldOfClassByName(newProperty.getClass(), "values");
                        propertyValueField.setAccessible(true);
                        ((List) propertyValueField.get(newProperty)).add(getEmptyConstructorIfPresent(attributeType).newInstance());
                        propertyValueField.setAccessible(false);
                    }
                    attributeField.set(object, newProperty);
                }
            }
        }
        return Map.entry(object, attributeField);
    }

    /**
     * Tries to get the object that is the value of the field in the given object by using a getter method.
     *
     * @param field  field of which the value should be returned
     * @param object object from which the attribute value should be got
     */
    public static Object getValueObjectFromField(Field field, Object object) {
        try {
            String property = field.getName();
            Method method = new PropertyDescriptor(property, object.getClass(), "is" + Character.toUpperCase(property.charAt(0)) + property.substring(1), null).getReadMethod();
            if (method == null) {
                return null;
            }
            return method.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
            return null;
        }
    }

    public static Class<?> getGenericTypeOfField(Field genericField) {
        ParameterizedType fieldParamType = (ParameterizedType) genericField.getGenericType();
        Type actualType = fieldParamType.getActualTypeArguments()[0];
        return (Class<?>) actualType;
    }

    public static boolean hasClassOrSuperclass(Object object, String simpleClazzName) {
        if (object == null) {
            throw new NullPointerException("param 'object' should not be null");
        }
        if (StringUtils.isBlank(simpleClazzName)) {
            throw new NullPointerException("param 'simpleClazzName' should not be null or empty");
        }
        return getInheritanceStackOfObject(object)
            .stream()
            .anyMatch(clazz -> Strings.CI.equals(clazz.getSimpleName(), simpleClazzName));
    }

    public static Stack<Class<?>> getInheritanceStackOfObject(Object object, Class<?> stopAt) {
        if (object == null) {
            throw new NullPointerException("param 'object' should not be null");
        }
        return getInheritanceStackOfClass(object.getClass(), stopAt);
    }

    public static Stack<Class<?>> getInheritanceStackOfObject(Object object) {
        if (object == null) {
            throw new NullPointerException("param 'object' should not be null");
        }
        return getInheritanceStackOfObject(object, null);
    }

    public static Stack<Class<?>> getInheritanceStackOfClass(Class<?> clazz, Class<?> stopAt) {
        // create a stack of classes representing the inheritance structure
        Stack<Class<?>> classes = new Stack<>();
        classes.push(clazz);
        while (classes.peek().getSuperclass() != null) {
            Class<?> superClass = classes.peek().getSuperclass();
            classes.push(superClass);
            if (superClass != null && superClass == stopAt) {
                break;
            }
        }
        return classes;
    }

    public static Stack<Class<?>> getInheritanceStackOfClass(Class<?> clazz) {
        return getInheritanceStackOfClass(clazz, null);
    }

    public static String findName(Object object) {
        String name = "";
        ArrayList<Field> fieldArrayList = ReflectionUtils.getFieldsOfScenarioSpecItemClass(object.getClass());

        // Search for a name attribute inside the simulated object
        for (Field aField : fieldArrayList) {
            if (aField.getName().toLowerCase().contains("name")) {
                try {
                    String property = aField.getName();
                    Method method = new PropertyDescriptor(property, object.getClass(), "is" + Character.toUpperCase(property.charAt(0)) + property.substring(1), null).getReadMethod();
                    if (method == null) {
                        continue;
                    }

                    Object nameAttribute = method.invoke(object);
                    if (nameAttribute instanceof String) {
                        name = (String) nameAttribute;

                    } else if (nameAttribute instanceof Property) {
                        if (((Property<?>) nameAttribute).getDataType().equals(String.class.getName())) {
                            // noinspection unchecked
                            name = (String) getValueOfProperty((Property<String>) nameAttribute);
                        }
                    }
                    break;
                } catch (IllegalAccessException | InvocationTargetException | IntrospectionException | ClassCastException e) {
                    log(e);
                }
            }
        }
        return name;
    }

    private static Constructor<?> getEmptyConstructorIfPresent(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> constructor = null;
        for (Constructor<?> value : constructors) {
            if (value.getGenericParameterTypes().length == 0) {
                constructor = value;
            }
        }
        return constructor;
    }

}