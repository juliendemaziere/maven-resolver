package org.eclipse.aether.util.filter;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.test.util.NodeBuilder;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.junit.Test;

public class ExclusionDependencyFilterTest
{

    @Test
    public void acceptTest()
    {

        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" );
        List<DependencyNode> parents = new LinkedList<DependencyNode>();
        String[] excludes;

        excludes = new String[] { "com.example.test:testArtifact" };
        assertThat(new ExclusionsDependencyFilter( Arrays.asList( excludes ) ).accept( builder.build(), parents ) ).isFalse();

        excludes = new String[] { "com.example.test:testArtifact", "com.foo:otherArtifact" };
        assertThat(new ExclusionsDependencyFilter( Arrays.asList( excludes ) ).accept( builder.build(), parents ) ).isFalse();

        excludes = new String[] { "testArtifact" };
        assertThat(new ExclusionsDependencyFilter( Arrays.asList( excludes ) ).accept( builder.build(), parents ) ).isFalse();

        excludes = new String[] { "otherArtifact" };
        assertThat(new ExclusionsDependencyFilter( Arrays.asList( excludes ) ).accept( builder.build(), parents ) ).isTrue();

        assertThat(new ExclusionsDependencyFilter( (Collection<String>) null ).accept( builder.build(), parents ) ).isTrue();
    }
}
