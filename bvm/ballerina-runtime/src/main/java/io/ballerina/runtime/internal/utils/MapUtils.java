/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.runtime.internal.utils;

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.flags.SymbolFlags;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.types.semtype.Builder;
import io.ballerina.runtime.api.types.semtype.Core;
import io.ballerina.runtime.api.types.semtype.SemType;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.internal.TypeChecker;
import io.ballerina.runtime.internal.errors.ErrorCodes;
import io.ballerina.runtime.internal.errors.ErrorHelper;
import io.ballerina.runtime.internal.types.BRecordType;
import io.ballerina.runtime.internal.types.BTypeReferenceType;
import io.ballerina.runtime.internal.values.MapValue;

import static io.ballerina.runtime.api.constants.RuntimeConstants.MAP_LANG_LIB;
import static io.ballerina.runtime.internal.errors.ErrorReasons.INHERENT_TYPE_VIOLATION_ERROR_IDENTIFIER;
import static io.ballerina.runtime.internal.errors.ErrorReasons.MAP_KEY_NOT_FOUND_ERROR;
import static io.ballerina.runtime.internal.errors.ErrorReasons.OPERATION_NOT_SUPPORTED_IDENTIFIER;
import static io.ballerina.runtime.internal.errors.ErrorReasons.getModulePrefixedReason;

/**
 * Common utility methods used for MapValue insertion/manipulation.
 *
 * @since 0.995.0
 */
public final class MapUtils {

    private MapUtils() {
    }

    public static void handleMapStore(MapValue<BString, Object> mapValue, BString fieldName, Object value) {
        updateMapValue(mapValue.getType(), mapValue, fieldName, value);
    }

    public static void handleInherentTypeViolatingMapUpdate(Object value, MapType mapType) {
        if (TypeChecker.checkIsType(value, mapType.getConstrainedType())) {
            return;
        }

        Type expType = mapType.getConstrainedType();
        Type valuesType = TypeChecker.getType(value);

        throw ErrorCreator.createError(getModulePrefixedReason(MAP_LANG_LIB,
                                                               INHERENT_TYPE_VIOLATION_ERROR_IDENTIFIER),
                                       ErrorHelper.getErrorDetails(ErrorCodes.INVALID_MAP_INSERTION,
                                                                               expType, valuesType));
    }

    public static boolean handleInherentTypeViolatingRecordUpdate(
            MapValue<?, ?> mapValue, BString fieldName, Object value,
            BRecordType recType, boolean initialValue) {
        Field recField = recType.getFields().get(fieldName.getValue());
        Type recFieldType;

        if (recField != null) {
            // If there is a corresponding field in the record, check if it can be updated.
            // i.e., it is not a `readonly` field or this is an insertion on creation.
            // `initialValue` is only true if this is an update for a field provided in the mapping constructor
            // expression.
            if (!initialValue && SymbolFlags.isFlagOn(recField.getFlags(), SymbolFlags.READONLY)) {

                throw ErrorCreator.createError(
                        getModulePrefixedReason(MAP_LANG_LIB, INHERENT_TYPE_VIOLATION_ERROR_IDENTIFIER),
                        ErrorHelper.getErrorDetails(ErrorCodes.RECORD_INVALID_READONLY_FIELD_UPDATE,
                                                             fieldName, mapValue.getType()));
            }

            // If it can be updated, use it.
            recFieldType = recField.getFieldType();
            if (value == null && SymbolFlags.isFlagOn(recField.getFlags(), SymbolFlags.OPTIONAL)
                    && !containsNilType(recFieldType)) {
                return false;
            }
        } else if (recType.restFieldType != null) {
            // If there isn't a corresponding field, but there is a rest field, use it
            recFieldType = recType.restFieldType;
        } else {
            // If both of the above conditions fail, the implication is that this is an attempt to insert a
            // value to a non-existent field in a closed record.
            throw ErrorCreator.createError(MAP_KEY_NOT_FOUND_ERROR, ErrorHelper.getErrorDetails(
                    ErrorCodes.INVALID_RECORD_FIELD_ACCESS, fieldName, mapValue.getType()));
        }

        if (TypeChecker.checkIsType(value, recFieldType)) {
            return true;
        }
        Type valuesType = TypeChecker.getType(value);

        throw ErrorCreator.createError(getModulePrefixedReason(MAP_LANG_LIB,
                                                               INHERENT_TYPE_VIOLATION_ERROR_IDENTIFIER),
                                       ErrorHelper.getErrorDetails(
                                                  ErrorCodes.INVALID_RECORD_FIELD_ADDITION, fieldName, recFieldType,
                                                  valuesType));
    }

    private static boolean containsNilType(Type type) {
        return Core.containsBasicType(SemType.tryInto(TypeChecker.context(), type), Builder.getNilType());
    }

    public static BError createOpNotSupportedError(Type type, String op) {
        return ErrorCreator.createError(getModulePrefixedReason(MAP_LANG_LIB, OPERATION_NOT_SUPPORTED_IDENTIFIER),
                ErrorHelper.getErrorDetails(ErrorCodes.OPERATION_NOT_SUPPORTED_ERROR, op, type));
    }

    public static void checkIsMapOnlyOperation(Type mapType, String op) {
        switch (TypeUtils.getImpliedType(mapType).getTag()) {
            case TypeTags.MAP_TAG:
            case TypeTags.JSON_TAG:
            case TypeTags.RECORD_TYPE_TAG:
                return;
            default:
                throw createOpNotSupportedError(mapType, op);
        }
    }

    private static void updateMapValue(Type mapType, MapValue<BString, Object> mapValue, BString fieldName,
                                       Object value) {
        mapType = TypeUtils.getImpliedType(mapType);
        switch (mapType.getTag()) {
            case TypeTags.MAP_TAG:
                handleInherentTypeViolatingMapUpdate(value, (MapType) mapType);
                mapValue.put(fieldName, value);
                return;
            case TypeTags.RECORD_TYPE_TAG:
                if (handleInherentTypeViolatingRecordUpdate(mapValue, fieldName, value, (BRecordType) mapType, false)) {
                    mapValue.put(fieldName, value);
                    return;
                }
                mapValue.remove(fieldName);
                return;
            case TypeTags.TYPE_REFERENCED_TYPE_TAG:
                updateMapValue(((BTypeReferenceType) mapType).getReferredType(), mapValue, fieldName, value);
        }
    }
}
