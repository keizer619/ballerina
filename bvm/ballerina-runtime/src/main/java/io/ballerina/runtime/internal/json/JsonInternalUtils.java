/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.runtime.internal.json;

import io.ballerina.runtime.api.constants.TypeConstants;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.JsonType;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.StructureType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BRefValue;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTable;
import io.ballerina.runtime.internal.TypeChecker;
import io.ballerina.runtime.internal.TypeConverter;
import io.ballerina.runtime.internal.errors.ErrorCodes;
import io.ballerina.runtime.internal.errors.ErrorHelper;
import io.ballerina.runtime.internal.errors.ErrorReasons;
import io.ballerina.runtime.internal.types.BArrayType;
import io.ballerina.runtime.internal.types.BFiniteType;
import io.ballerina.runtime.internal.types.BJsonType;
import io.ballerina.runtime.internal.types.BStructureType;
import io.ballerina.runtime.internal.types.BUnionType;
import io.ballerina.runtime.internal.values.ArrayValue;
import io.ballerina.runtime.internal.values.ArrayValueImpl;
import io.ballerina.runtime.internal.values.DecimalValue;
import io.ballerina.runtime.internal.values.ErrorValue;
import io.ballerina.runtime.internal.values.ListInitialValueEntry;
import io.ballerina.runtime.internal.values.MapValue;
import io.ballerina.runtime.internal.values.MapValueImpl;
import io.ballerina.runtime.internal.values.MappingInitialValueEntry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import static io.ballerina.runtime.api.constants.RuntimeConstants.MAP_LANG_LIB;
import static io.ballerina.runtime.internal.TypeChecker.isByteLiteral;
import static io.ballerina.runtime.internal.errors.ErrorReasons.INHERENT_TYPE_VIOLATION_ERROR_IDENTIFIER;
import static io.ballerina.runtime.internal.errors.ErrorReasons.JSON_OPERATION_ERROR;
import static io.ballerina.runtime.internal.errors.ErrorReasons.MAP_KEY_NOT_FOUND_ERROR;
import static io.ballerina.runtime.internal.errors.ErrorReasons.getModulePrefixedReason;

/**
 * Common utility methods used for JSON manipulation.
 *
 * @since 0.995.0
 */
@SuppressWarnings("unchecked")
public final class JsonInternalUtils {

    public static final String OBJECT = "object";
    public static final String ARRAY = "array";

    /**
     * Check whether JSON has particular field.
     *
     * @param json        JSON to be considered.
     * @param elementName String name json field to be considered.
     * @return Boolean 'true' if JSON has given field.
     */
    public static boolean hasElement(Object json, String elementName) {
        if (!isJSONObject(json)) {
            return false;
        }
        return ((MapValueImpl<BString, ?>) json).containsKey(StringUtils.fromString(elementName));
    }

    /**
     * Convert {@link ArrayValue} to JSON.
     *
     * @param bArray {@link ArrayValue} to be converted to JSON
     * @return JSON representation of the provided bArray
     */
    public static ArrayValue convertArrayToJSON(BArray bArray) {
        if (bArray == null) {
            return null;
        }

        Type elementType = TypeUtils.getImpliedType(bArray.getElementType());
        if (elementType == PredefinedTypes.TYPE_INT) {
            return convertIntArrayToJSON(bArray);
        } else if (elementType == PredefinedTypes.TYPE_BOOLEAN) {
            return convertBooleanArrayToJSON(bArray);
        } else if (elementType == PredefinedTypes.TYPE_FLOAT) {
            return convertFloatArrayToJSON(bArray);
        } else if (elementType == PredefinedTypes.TYPE_STRING) {
            return convertStringArrayToJSON(bArray);
        } else {
            return convertRefArrayToJSON(bArray);
        }
    }

    /**
     * Convert map value to JSON.
     *
     * @param map value {@link MapValueImpl} to be converted to JSON
     * @param targetType the target JSON type to be convert to
     * @return JSON representation of the provided array
     */
    public static Object convertMapToJSON(BMap<BString, ?> map, JsonType targetType) {
        if (map == null) {
            return null;
        }

        MapValueImpl<BString, Object> json = new MapValueImpl<>(targetType);
        for (Entry<BString, ?> structField : map.entrySet()) {
            BString key = structField.getKey();
            Object value = structField.getValue();
            populateJSON(json, key, value, PredefinedTypes.TYPE_JSON);
        }
        return json;
    }

    /**
     * Get an element from a JSON.
     *
     * @param json JSON to get the element from
     * @param elementName Name of the element to be retrieved
     * @return Element of the JSON for the provided key, if the JSON is object type. Error if not an object or nil
     * if the object does not have the key.
     */
    public static Object getElementOrNil(Object json, BString elementName) {
        return getMappingElement(json, elementName, true);
    }

    /**
     * Get an element from a JSON.
     *
     * @param json JSON to get the element from
     * @param elementName Name of the element to be retrieved
     * @return Element of the JSON for the provided key, if the JSON is object type. Error if not an object or does
     * not have the key.
     */
    public static Object getElement(Object json, BString elementName) {
        return getMappingElement(json, elementName, false);
    }

    /**
     * Get an element from a JSON.
     *
     * @param json JSON object to get the element from
     * @param elementName Name of the element to be retrieved
     * @param returnNilOnMissingKey Whether to return nil on missing key instead of error
     * @return Element of JSON having the provided name, if the JSON is object type. Null otherwise.
     */
    private static Object getMappingElement(Object json, BString elementName, boolean returnNilOnMissingKey) {
        if (!isJSONObject(json)) {
            return ErrorCreator.createError(JSON_OPERATION_ERROR,
                                            StringUtils.fromString("JSON value is not a mapping"));
        }

        MapValueImpl<BString, Object> jsonObject = (MapValueImpl<BString, Object>) json;

        if (!jsonObject.containsKey(elementName)) {
            if (returnNilOnMissingKey) {
                return null;
            }

            return ErrorCreator.createError(MAP_KEY_NOT_FOUND_ERROR,
                    ErrorHelper.getErrorDetails(ErrorCodes.KEY_NOT_FOUND_IN_JSON_MAPPING, elementName));
        }

        try {
            return jsonObject.get(elementName);
        } catch (Throwable t) {
            throw ErrorHelper.getRuntimeException(ErrorCodes.JSON_GET_ERROR, t.getMessage());
        }
    }

    /**
     * Set an element in a JSON. If an element with the given name already exists,
     * this method will update the existing element. Otherwise, a new element with
     * the given name will be added. If the JSON is not object type, then this
     * operation has no effect.
     * 
     * @param json JSON object to set the element
     * @param elementName Name of the element to be set
     * @param element JSON element
     */
    public static void setElement(Object json, String elementName, Object element) {
        if (!isJSONObject(json)) {
            return;
        }

        try {
            ((MapValueImpl<BString, Object>) json).put(StringUtils.fromString(elementName), element);
        } catch (ErrorValue e) {
            throw e;
        } catch (Throwable t) {
            throw ErrorHelper.getRuntimeException(
                    getModulePrefixedReason(MAP_LANG_LIB, INHERENT_TYPE_VIOLATION_ERROR_IDENTIFIER),
                    ErrorCodes.JSON_SET_ERROR, t.getMessage());
        }
    }

    /**
     * Check whether provided JSON object is a JSON Array.
     *
     * @param json JSON to execute array condition.
     * @return returns true if provided JSON is a JSON Array.
     */
    public static boolean isJSONArray(Object json) {
        if (!(json instanceof BRefValue refValue)) {
            return false;
        }
        return TypeUtils.getImpliedType(refValue.getType()).getTag() == TypeTags.ARRAY_TAG;
    }

    /**
     * Check whether provided JSON object is a JSON Object.
     *
     * @param json JSON to execute array condition.
     * @return returns true if provided JSON is a JSON Object.
     */
    public static boolean isJSONObject(Object json) {
        if (!(json instanceof BRefValue refValue)) {
            return false;
        }

        Type type = TypeUtils.getImpliedType(refValue.getType());
        int typeTag = type.getTag();
        return typeTag == TypeTags.MAP_TAG || typeTag == TypeTags.RECORD_TYPE_TAG;
    }

    /**
     * Convert a JSON node to a map.
     *
     * @param json JSON to convert
     * @param mapType MapType which the JSON is converted to.
     * @return If the provided JSON is of object-type, this method will return a {@link MapValueImpl} containing the
     *          values of the JSON object. Otherwise a {@link BError} will be thrown.
     */
    public static MapValueImpl<BString, ?> jsonToMap(Object json, MapType mapType) {
        if (!isJSONObject(json)) {
            throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE,
                                                           getComplexObjectTypeName(OBJECT), getTypeName(json));
        }

        MapValueImpl<BString, Object> map = new MapValueImpl<>(mapType);
        Type mapConstraint = TypeUtils.getImpliedType(mapType.getConstrainedType());
        if (mapConstraint.getTag() == TypeTags.ANY_TAG || mapConstraint.getTag() == TypeTags.JSON_TAG) {
            ((MapValueImpl<BString, Object>) json).forEach(map::put);

            return map;
        }

        // We reach here if the map is constrained.
        ((MapValueImpl<BString, Object>) json).forEach((key, value) -> map.put(key, convertJSON(value, mapConstraint)));

        return map;
    }

    /**
     * Convert a BJSON to a user defined record.
     *
     * @param json       JSON to convert
     * @param structType Type (definition) of the target record
     * @return If the provided JSON is of object-type, this method will return a {@link MapValueImpl} containing the
     * values of the JSON object. Otherwise the method will throw a {@link BError}.
     */
    public static MapValueImpl<BString, Object> convertJSONToRecord(Object json, StructureType structType) {
        if (!isJSONObject(json)) {
            throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE,
                                                           getComplexObjectTypeName(OBJECT), getTypeName(json));
        }

        ArrayList<MappingInitialValueEntry.KeyValueEntry> keyValuePairs = new ArrayList<>();
        MapValueImpl<BString, Object> jsonObject = (MapValueImpl<BString, Object>) json;
        for (Entry<String, Field> field : structType.getFields().entrySet()) {
            Type fieldType = field.getValue().getFieldType();
            BString fieldName = StringUtils.fromString(field.getValue().getFieldName());
            try {
                // If the field does not exists in the JSON, set the default value for that struct field.
                if (!jsonObject.containsKey(fieldName)) {
                    keyValuePairs.add(new MappingInitialValueEntry.KeyValueEntry(fieldName, fieldType.getZeroValue()));
                    continue;
                }

                Object jsonValue = jsonObject.get(fieldName);
                keyValuePairs.add(new MappingInitialValueEntry.KeyValueEntry(fieldName,
                        convertJSON(jsonValue, fieldType)));
            } catch (Exception e) {
                handleError(e, fieldName.getValue());
            }
        }
        return new MapValueImpl<>(structType, keyValuePairs.toArray(MappingInitialValueEntry.KeyValueEntry[]::new));
    }

    public static Object convertJSON(Object jsonValue, Type targetType) {
        targetType = TypeUtils.getImpliedType(targetType);
        Type matchingType;
        switch (targetType.getTag()) {
            case TypeTags.BYTE_TAG:
                return jsonNodeToByte(jsonValue);
            case TypeTags.INT_TAG:
                return jsonNodeToInt(jsonValue);
            case TypeTags.FLOAT_TAG:
                return jsonNodeToFloat(jsonValue);
            case TypeTags.DECIMAL_TAG:
                return jsonNodeToDecimal(jsonValue);
            case TypeTags.STRING_TAG:
                if (jsonValue instanceof BString) {
                    return jsonValue;
                }
                return StringUtils.fromString(jsonValue.toString());
            case TypeTags.BOOLEAN_TAG:
                return jsonNodeToBoolean(jsonValue);
            case TypeTags.JSON_TAG:
                if (jsonValue != null && !TypeChecker.checkIsType(jsonValue, targetType)) {
                    throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE, targetType,
                                                                   getTypeName(jsonValue));
                }
                // fall through
            case TypeTags.ANY_TAG:
                return jsonValue;
            case TypeTags.UNION_TAG:
                matchingType = TypeConverter.getConvertibleTypeInTargetUnionType(jsonValue,
                        (BUnionType) targetType, null, new ArrayList<>(), new HashSet<>(), true);
                if (matchingType == null) {
                    throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE, targetType,
                            getTypeName(jsonValue));
                }
                return convertJSON(jsonValue, matchingType);
            case TypeTags.FINITE_TYPE_TAG:
                matchingType = TypeConverter.getConvertibleFiniteType(jsonValue, (BFiniteType) targetType,
                        null, new ArrayList<>(), new HashSet<>(), true);
                if (matchingType == null) {
                    throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE, targetType,
                            getTypeName(jsonValue));
                }
                return convertJSON(jsonValue, matchingType);
            case TypeTags.OBJECT_TYPE_TAG:
            case TypeTags.RECORD_TYPE_TAG:
                return convertJSONToRecord(jsonValue, (BStructureType) targetType);
            case TypeTags.ARRAY_TAG:
                return convertJSONToBArray(jsonValue, (BArrayType) targetType);
            case TypeTags.MAP_TAG:
                return jsonToMap(jsonValue, (MapType) targetType);
            case TypeTags.NULL_TAG:
                if (jsonValue == null) {
                    return null;
                }
                // fall through
            default:
                throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE, targetType,
                        getTypeName(jsonValue));
        }
    }

    /**
     * Returns the keys of a JSON as a {@link ArrayValue}.
     * 
     * @param json JSON to get the keys
     * @return Keys of the JSON as a {@link ArrayValue}
     */
    public static ArrayValue getKeys(Object json) {
        if (!isJSONObject(json)) {
            return new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_STRING));
        }

        BString[] keys = ((MapValueImpl<BString, ?>) json).getKeys();
        return new ArrayValueImpl(keys, false);
    }

    public static Object convertUnionTypeToJSON(Object source, JsonType targetType) {
        if (source == null) {
            return null;
        }

        Type type = TypeUtils.getImpliedType(TypeChecker.getType(source));
        return switch (type.getTag()) {
            case TypeTags.INT_TAG,
                 TypeTags.FLOAT_TAG,
                 TypeTags.DECIMAL_TAG,
                 TypeTags.STRING_TAG,
                 TypeTags.BOOLEAN_TAG,
                 TypeTags.JSON_TAG -> source;
            case TypeTags.NULL_TAG -> null;
            case TypeTags.MAP_TAG,
                 TypeTags.OBJECT_TYPE_TAG,
                 TypeTags.RECORD_TYPE_TAG -> convertMapToJSON((MapValueImpl<BString, Object>) source, targetType);
            default -> throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE,
                    PredefinedTypes.TYPE_JSON, type);
        };
    }

    /**
     * Remove a field from JSON. Has no effect if the JSON if not object types or if the given field doesn't exists.
     * 
     * @param json JSON object
     * @param fieldName Name of the field to remove
     */
    public static void remove(Object json, BString fieldName) {
        if (!isJSONObject(json)) {
            return;
        }

        ((MapValueImpl<BString, ?>) json).remove(fieldName);
    }

    public static BError getErrorIfUnmergeable(Object j1, Object j2, List<ObjectPair> visitedPairs) {
        if (j1 == null || j2 == null) {
            return null;
        }

        Type j1Type = TypeUtils.getImpliedType(TypeChecker.getType(j1));
        Type j2Type = TypeUtils.getImpliedType(TypeChecker.getType(j2));

        if (j1Type.getTag() != TypeTags.MAP_TAG || j2Type.getTag() != TypeTags.MAP_TAG) {
            return ErrorCreator.createError(ErrorReasons.MERGE_JSON_ERROR,
                    ErrorHelper.getErrorDetails(ErrorCodes.MERGE_JSON_ERROR, j1Type, j2Type));
        }

        ObjectPair currentPair = new ObjectPair(j1, j2);
        if (visitedPairs.contains(currentPair)) {
            return ErrorCreator.createError(ErrorReasons.MERGE_JSON_ERROR,
                                            StringUtils
                                                     .fromString("Cannot merge JSON values with cyclic references"));
        }
        visitedPairs.add(currentPair);

        MapValue<BString, Object> m1 = (MapValue<BString, Object>) j1;
        MapValue<BString, Object> m2 = (MapValue<BString, Object>) j2;

        for (Entry<BString, Object> entry : m2.entrySet()) {
            BString key = entry.getKey();

            if (!m1.containsKey(key)) {
                continue;
            }

            BError elementMergeNullableError = getErrorIfUnmergeable(m1.get(key), entry.getValue(), visitedPairs);

            if (elementMergeNullableError == null) {
                continue;
            }
            MappingInitialValueEntry[] initialValues = new MappingInitialValueEntry[2];
            initialValues[0] = new MappingInitialValueEntry.KeyValueEntry(TypeConstants.DETAIL_MESSAGE,
                                                StringUtils.fromString("JSON Merge failed for key '" + key + "'"));
            initialValues[1] = new MappingInitialValueEntry.KeyValueEntry(TypeConstants.DETAIL_CAUSE,
                                                                          elementMergeNullableError);
            MapValueImpl<BString, Object> detailMap = new MapValueImpl<>(PredefinedTypes.TYPE_ERROR_DETAIL,
                                                                        initialValues);
            return ErrorCreator.createError(ErrorReasons.MERGE_JSON_ERROR, detailMap);
        }
        return null;
    }

    public static Object mergeJson(Object j1, Object j2, boolean checkMergeability) {
        if (j1 == null) {
            return j2;
        }

        if (j2 == null) {
            return j1;
        }

        if (checkMergeability) {
            Type j1Type = TypeUtils.getImpliedType(TypeChecker.getType(j1));
            Type j2Type = TypeUtils.getImpliedType(TypeChecker.getType(j2));

            if (j1Type.getTag() != TypeTags.MAP_TAG || j2Type.getTag() != TypeTags.MAP_TAG) {
                return ErrorCreator.createError(ErrorReasons.MERGE_JSON_ERROR,
                        ErrorHelper.getErrorDetails(ErrorCodes.MERGE_JSON_ERROR, j1Type, j2Type));
            }
        }

        MapValue<BString, Object> m1 = (MapValue<BString, Object>) j1;
        MapValue<BString, Object> m2 = (MapValue<BString, Object>) j2;
        return m1.merge(m2, true);
    }

    /**
     * Convert a JSON node to an array.
     *
     * @param json JSON to convert
     * @param targetArrayType Type of the target array
     * @return If the provided JSON is of array type, this method will return a {@link BArrayType} containing the values
     *         of the JSON array. Otherwise the method will throw a {@link BError}.
     */
    public static ArrayValue convertJSONToBArray(Object json, BArrayType targetArrayType) {
        if (!(json instanceof ArrayValue jsonArray)) {
            throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE,
                    getComplexObjectTypeName(ARRAY), getTypeName(json));
        }

        Type targetElementType = TypeUtils.getImpliedType(targetArrayType.getElementType());
        switch (targetElementType.getTag()) {
            case TypeTags.INT_TAG:
                return jsonArrayToBIntArray(jsonArray);
            case TypeTags.FLOAT_TAG:
                return jsonArrayToBFloatArray(jsonArray);
            case TypeTags.DECIMAL_TAG:
                return jsonArrayToBDecimalArray(jsonArray);
            case TypeTags.STRING_TAG:
                return jsonArrayToBStringArray(jsonArray);
            case TypeTags.BOOLEAN_TAG:
                return jsonArrayToBooleanArray(jsonArray);
            case TypeTags.ANY_TAG:
                ArrayList<ListInitialValueEntry> array = new ArrayList<>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    array.add(i, new ListInitialValueEntry.ExpressionEntry(jsonArray.getRefValue(i)));
                }
                return new ArrayValueImpl(targetArrayType, array.toArray(ListInitialValueEntry[]::new));
            default:
                array = new ArrayList<>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    array.add(new ListInitialValueEntry.ExpressionEntry(convertJSON(jsonArray.getRefValue(i),
                            targetElementType)));
                }
                return new ArrayValueImpl(targetArrayType, array.toArray(ListInitialValueEntry[]::new));
        }
    }

    /**
     * Convert {@link BTable} to JSON.
     *
     * @param table {@link BTable} to be converted
     * @return JSON representation of the provided table
     */
    public static Object toJSON(BTable<?, ?> table) {
        TableJsonDataSource jsonDataSource = new TableJsonDataSource(table);
        return jsonDataSource.build();
    }

    public static BError createJsonConversionError(Throwable throwable, String prefix) {
        BString detail = StringUtils.fromString(throwable.getMessage() != null ?
                prefix + ": " + throwable.getMessage() :
                "error occurred in JSON Conversion");
        return ErrorCreator.createError(ErrorReasons.JSON_CONVERSION_ERROR, detail);
    }

    // Private methods

    /**
     * Convert to int.
     *
     * @param json node to be converted
     * @return BInteger value of the JSON, if its a integer or a long JSON node. Error, otherwise.
     */
    private static long jsonNodeToInt(Object json) {
        if (!(json instanceof Long || json instanceof Integer)) {
            throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE_FOR_CASTING_JSON,
                                                           PredefinedTypes.TYPE_INT, getTypeName(json));
        }

        return ((Number) json).longValue();
    }

    /**
     * Convert to byte.
     *
     * @param json node to be converted
     * @return JSON value as a long, if the value is within byte range. Error, otherwise.
     */
    private static long jsonNodeToByte(Object json) {
        if (json instanceof Long || json instanceof Integer) {
            long x = ((Number) json).longValue();
            if (isByteLiteral(x)) {
                return x;
            }
        }

        throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE_FOR_CASTING_JSON,
                PredefinedTypes.TYPE_BYTE, getTypeName(json));
    }

    /**
     * Convert to float.
     *
     * @param json node to be converted
     * @return BFloat value of the JSON, if its a double or a float JSON node. Error, otherwise.
     */
    private static double jsonNodeToFloat(Object json) {
        if (json instanceof Integer i) {
            return i.longValue();
        } else if (json instanceof Double d) {
            return d;
        } else if (json instanceof DecimalValue decimalValue) {
            return decimalValue.floatValue();
        } else {
            throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE_FOR_CASTING_JSON,
                                                           PredefinedTypes.TYPE_FLOAT, getTypeName(json));
        }
    }

    /**
     * Convert JSON to decimal.
     *
     * @param json JSON to be converted
     * @return BDecimal value of the JSON, if it's a valid convertible JSON node. Error, otherwise.
     */
    private static DecimalValue jsonNodeToDecimal(Object json) {
        BigDecimal decimal;
        if (json instanceof Integer i) {
            decimal = new BigDecimal(i.longValue());
        } else if (json instanceof Double d) {
            decimal = BigDecimal.valueOf(d);
        } else if (json instanceof BigDecimal bigDecimal) {
            decimal = bigDecimal;
        } else if (json instanceof DecimalValue decimalValue) {
            return decimalValue;
        } else {
            throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE_FOR_CASTING_JSON,
                                                           PredefinedTypes.TYPE_DECIMAL, getTypeName(json));
        }

        return new DecimalValue(decimal);
    }

    /**
     * Convert to boolean.
     *
     * @param json node to be converted
     * @return Boolean value of the JSON, if its a boolean node. Error, otherwise.
     */
    private static boolean jsonNodeToBoolean(Object json) {
        if (!(json instanceof Boolean b)) {
            throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE_FOR_CASTING_JSON,
                                                           PredefinedTypes.TYPE_BOOLEAN, getTypeName(json));
        }
        return b;
    }

    private static ArrayValue jsonArrayToBIntArray(ArrayValue arrayNode) {
        ArrayValue intArray = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_INT));
        for (int i = 0; i < arrayNode.size(); i++) {
            Object jsonValue = arrayNode.getRefValue(i);
            intArray.add(i, jsonNodeToInt(jsonValue));
        }
        return intArray;
    }

    private static ArrayValue jsonArrayToBFloatArray(ArrayValue arrayNode) {
        ArrayValue floatArray = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_FLOAT));
        for (int i = 0; i < arrayNode.size(); i++) {
            Object jsonValue = arrayNode.getRefValue(i);
            floatArray.add(i, jsonNodeToFloat(jsonValue));
        }
        return floatArray;
    }

    private static ArrayValue jsonArrayToBDecimalArray(ArrayValue arrayNode) {
        ArrayValue decimalArray = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_DECIMAL));
        for (int i = 0; i < arrayNode.size(); i++) {
            Object jsonValue = arrayNode.getRefValue(i);
            decimalArray.add(i, jsonNodeToDecimal(jsonValue));
        }
        return decimalArray;
    }

    private static ArrayValue jsonArrayToBStringArray(ArrayValue arrayNode) {
        ArrayValue stringArray = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_STRING));
        for (int i = 0; i < arrayNode.size(); i++) {
            stringArray.add(i, arrayNode.getRefValue(i).toString());
        }
        return stringArray;
    }

    private static ArrayValue jsonArrayToBooleanArray(ArrayValue arrayNode) {
        ArrayValue booleanArray = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_BOOLEAN));
        for (int i = 0; i < arrayNode.size(); i++) {
            Object jsonValue = arrayNode.getRefValue(i);
            booleanArray.add(i, jsonNodeToBoolean(jsonValue));
        }
        return booleanArray;
    }

    /**
     * Convert {@link ArrayValue} to JSON.
     *
     * @param refValueArray {@link ArrayValue} to be converted to JSON
     * @return JSON representation of the provided refValueArray
     */
    private static ArrayValue convertRefArrayToJSON(BArray refValueArray) {
        ArrayValue json = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_JSON));
        for (int i = 0; i < refValueArray.size(); i++) {
            Object value = refValueArray.getRefValue(i);
            if (value == null) {
                json.append(null);
            }

            Type type = TypeUtils.getImpliedType(TypeChecker.getType(value));
            switch (type.getTag()) {
                case TypeTags.JSON_TAG:
                    json.append(value);
                    break;
                case TypeTags.MAP_TAG:
                case TypeTags.RECORD_TYPE_TAG:
                case TypeTags.OBJECT_TYPE_TAG:
                    json.append(convertMapToJSON((MapValueImpl<BString, ?>) value, PredefinedTypes.TYPE_JSON));
                    break;
                case TypeTags.ARRAY_TAG:
                    json.append(convertArrayToJSON((ArrayValue) value));
                    break;
                default:
                    throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE,
                                                                   PredefinedTypes.TYPE_JSON, type);
            }
        }
        return json;
    }

    /**
     * Convert {@link ArrayValue} to JSON.
     *
     * @param intArray {@link ArrayValue} to be converted to JSON
     * @return JSON representation of the provided intArray
     */
    private static ArrayValue convertIntArrayToJSON(BArray intArray) {
        ArrayValue json = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_JSON));
        for (int i = 0; i < intArray.size(); i++) {
            long value = intArray.getInt(i);
            json.append(value);
        }
        return json;
    }

    /**
     * Convert {@link ArrayValue} to JSON.
     *
     * @param floatArray {@link ArrayValue} to be converted to JSON
     * @return JSON representation of the provided floatArray
     */
    private static ArrayValue convertFloatArrayToJSON(BArray floatArray) {
        ArrayValue json = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_JSON));
        for (int i = 0; i < floatArray.size(); i++) {
            double value = floatArray.getFloat(i);
            json.append(value);
        }
        return json;
    }

    /**
     * Convert {@link ArrayValue} to JSON.
     *
     * @param stringArray {@link ArrayValue} to be converted to JSON
     * @return JSON representation of the provided stringArray
     */
    private static ArrayValue convertStringArrayToJSON(BArray stringArray) {
        ArrayValue json = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_JSON));
        for (int i = 0; i < stringArray.size(); i++) {
            json.append(stringArray.getBString(i));
        }
        return json;
    }

    /**
     * Convert {@link ArrayValue} to JSON.
     *
     * @param booleanArray {@link ArrayValue} to be converted to JSON
     * @return JSON representation of the provided booleanArray
     */
    private static ArrayValue convertBooleanArrayToJSON(BArray booleanArray) {
        ArrayValue json = new ArrayValueImpl(new BArrayType(PredefinedTypes.TYPE_JSON));
        for (int i = 0; i < booleanArray.size(); i++) {
            boolean value = booleanArray.getBoolean(i);
            json.append(value);
        }
        return json;
    }

    private static void populateJSON(BMap<BString, Object> json, BString key, Object value, Type exptType) {
        try {
            if (value == null) {
                json.put(key, null);
                return;
            }

            Type type = TypeUtils.getImpliedType(TypeChecker.getType(value));
            switch (type.getTag()) {
                case TypeTags.INT_TAG:
                case TypeTags.FLOAT_TAG:
                case TypeTags.DECIMAL_TAG:
                case TypeTags.STRING_TAG:
                case TypeTags.BOOLEAN_TAG:
                case TypeTags.JSON_TAG:
                    json.put(key, value);
                    break;
                case TypeTags.ARRAY_TAG:
                    json.put(key, convertArrayToJSON((ArrayValue) value));
                    break;
                case TypeTags.MAP_TAG:
                case TypeTags.RECORD_TYPE_TAG:
                case TypeTags.OBJECT_TYPE_TAG:
                    json.put(key, convertMapToJSON((MapValueImpl<BString, ?>) value, (BJsonType) exptType));
                    break;
                default:
                    throw ErrorHelper.getRuntimeException(ErrorCodes.INCOMPATIBLE_TYPE,
                                                                   PredefinedTypes.TYPE_JSON, type);
            }
        } catch (Exception e) {
            handleError(e, key.getValue());
        }
    }

    private static String getTypeName(Object jsonValue) {
        if (jsonValue == null) {
            return PredefinedTypes.TYPE_NULL.toString();
        }

        return TypeChecker.getType(jsonValue).toString();
    }

    private static String getComplexObjectTypeName(String nodeType) {
        return "json-" + nodeType;
    }

    private static void handleError(Exception e, String fieldName) {
        String errorMsg = e.getCause() == null ? "error while mapping '" + fieldName + "': " : "";
        throw ErrorCreator.createError(StringUtils.fromString(errorMsg + e.getMessage()), e);
    }

    private static class ObjectPair {
        Object lhsObject;
        Object rhsObject;

        public ObjectPair(Object lhsObject, Object rhsObject) {
            this.lhsObject = lhsObject;
            this.rhsObject = rhsObject;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ObjectPair other)) {
                return false;
            }

            return this.lhsObject == other.lhsObject && this.rhsObject == other.rhsObject;
        }
    }

    private JsonInternalUtils() {
    }
}
