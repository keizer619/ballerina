/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.ballerinalang.compiler.tree.expressions;

import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.model.tree.NodeKind;
import org.wso2.ballerinalang.compiler.tree.BLangNodeAnalyzer;
import org.wso2.ballerinalang.compiler.tree.BLangNodeTransformer;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represents xml step extension consisting of filters.
 * Example: {@code x/*.<filter1|filter2>, x/<name>.<filter1|filter2>}
 *
 * @since 2201.11.0
 */
public class BLangXMLFilterStepExtend extends BLangXMLStepExtend {

    public final List<BLangXMLElementFilter> filters;

    public BLangXMLFilterStepExtend(Location pos, List<BLangXMLElementFilter> filters) {
        this.pos = pos;
        this.filters = filters;
    }

    public void accept(BLangNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> void accept(BLangNodeAnalyzer<T> analyzer, T props) {
        analyzer.visit(this, props);
    }

    @Override
    public <T, R> R apply(BLangNodeTransformer<T, R> modifier, T props) {
        return modifier.transform(this, props);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.XML_STEP_FILTER_EXTEND;
    }

    @Override
    public String toString() {
        StringJoiner filters = new StringJoiner(" |");
        this.filters.forEach(f -> filters.add(f.toString()));
        return "." + "/<" + filters.toString() + ">";
    }
}
