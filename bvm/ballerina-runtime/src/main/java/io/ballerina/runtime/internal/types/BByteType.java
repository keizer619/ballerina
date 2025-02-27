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

package io.ballerina.runtime.internal.types;

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.types.ByteType;
import io.ballerina.runtime.api.types.TypeTags;

/**
 * {@code BByteType} represents byte type in Ballerina.
 *
 * @since 0.995.0
 */
public class BByteType extends BType implements ByteType {

    /**
     * Create a {@code BByteType} which represents the byte type.
     *
     * @param typeName string name of the type
     */
    public BByteType(String typeName, Module pkg) {
        super(typeName, pkg, Integer.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends Object> V getZeroValue() {
        return (V) new Integer(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends Object> V getEmptyValue() {
        return (V) new Integer(0);
    }

    @Override
    public int getTag() {
        return TypeTags.BYTE_TAG;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
