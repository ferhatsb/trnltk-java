/*
 * Copyright  2013  Ali Ok (aliokATapacheDOTorg)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.trnltk.morphology.contextless.parser.formbased;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.Validate;
import org.trnltk.morphology.model.Root;
import org.trnltk.morphology.morphotactics.SuffixGraph;
import org.trnltk.morphology.morphotactics.SuffixGraphState;
import org.trnltk.morphology.morphotactics.SuffixGraphStateType;
import org.trnltk.morphology.model.lexicon.tr.PhoneticAttribute;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SuffixFormGraph {

    private final Map<SuffixFormGraphNodeKey, SuffixFormGraphNode> nodeMap = new HashMap<SuffixFormGraphNodeKey, SuffixFormGraphNode>();
    private final SuffixGraph suffixGraph;

    public SuffixFormGraph(SuffixGraph suffixGraph) {
        this.suffixGraph = suffixGraph;
    }

    public SuffixFormGraphNode addNode(SuffixFormGraphNodeKey suffixFormGraphNodeKey, SuffixGraphStateType suffixGraphStateType, Set<PhoneticAttribute> phonAttrSet) {
        final SuffixFormGraphNode suffixFormGraphNode = new SuffixFormGraphNode(suffixFormGraphNodeKey, suffixGraphStateType, Sets.immutableEnumSet(phonAttrSet));
        final SuffixFormGraphNode existingSuffixFormGraphNode = this.nodeMap.put(suffixFormGraphNodeKey, suffixFormGraphNode);
        Validate.isTrue(existingSuffixFormGraphNode == null);

        return suffixFormGraphNode;
    }

    public void addNode(SuffixFormGraphNodeKey suffixFormGraphNodeKey, SuffixFormGraphNode suffixFormGraphNode) {
        final SuffixFormGraphNode existingNode = nodeMap.put(suffixFormGraphNodeKey, suffixFormGraphNode);
        Validate.isTrue(existingNode == null);
    }

    public SuffixFormGraphNode getNode(SuffixFormGraphNodeKey suffixFormGraphNodeKey) {
        return nodeMap.get(suffixFormGraphNodeKey);
    }

    public SuffixGraphState getDefaultStateForRoot(Root root) {
        final SuffixGraphState defaultStateForRoot = this.suffixGraph.getDefaultStateForRoot(root);
        if (defaultStateForRoot == null)
            throw new IllegalStateException("No default state found for root " + root);

        return defaultStateForRoot;
    }

    public SuffixGraph getSuffixGraph() {
        return suffixGraph;
    }

    // for tests
    Map<SuffixFormGraphNodeKey, SuffixFormGraphNode> getMap() {
        return nodeMap;
    }
}
