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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.test.util.NodeBuilder;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;
import org.junit.Test;

public class PatternInclusionsDependencyFilterTest
    extends AbstractDependencyFilterTest
{

    @Test
    public void acceptTestCornerCases()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.artifactId( "testArtifact" );
        DependencyNode node = builder.build();
        List<DependencyNode> parents = new LinkedList<>();

        // Empty String, Empty List
        assertThat( accept( node, "" ) ).isTrue();
        assertThat( new PatternInclusionsDependencyFilter( new LinkedList<String>() ).accept( node, parents ) ).isFalse();
        assertThat( new PatternInclusionsDependencyFilter( (String[]) null ).accept( node, parents ) ).isFalse();
        assertThat( new PatternInclusionsDependencyFilter( (VersionScheme) null, "[1,10]" ).accept( node, parents ) ).isFalse();
    }

    @Test
    public void acceptTestMatches()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );
        DependencyNode node = builder.build();

        // full match
        assertThat(  accept( node, "com.example.test:testArtifact:jar:1.0.3" ) ).isTrue();

        // single wildcard
        assertThat( accept( node, "*:testArtifact:jar:1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test:*:jar:1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test:testArtifact:*:1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test:testArtifact:*:1.0.3" ) ).isTrue();

        // implicit wildcard
        assertThat( accept( node, ":testArtifact:jar:1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test::jar:1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test:testArtifact::1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test:testArtifact:jar:" ) ).isTrue();

        // multi wildcards
        assertThat( accept( node, "*:*:jar:1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test:*:*:1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test:testArtifact:*:*" ) ).isTrue();
        assertThat( accept( node, "*:testArtifact:jar:*" ) ).isTrue();
        assertThat( accept( node, "*:*:jar:*" ) ).isTrue();
        assertThat( accept( node, ":*:jar:" ) ).isTrue();

        // partial wildcards
        assertThat( accept( node, "*.example.test:testArtifact:jar:1.0.3" ) ).isTrue();
        assertThat( accept( node, "com.example.test:testArtifact:*ar:1.0.*" ) ).isTrue();
        assertThat( accept( node, "com.example.test:testArtifact:jar:1.0.*" ) ).isTrue();
        assertThat( accept( node, "*.example.*:testArtifact:jar:1.0.3" ) ).isTrue();

        // wildcard as empty string
        assertThat( accept( node, "com.example.test*:testArtifact:jar:1.0.3" ) ).isTrue();
    }

    @Test
    public void acceptTestLessToken()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );
        DependencyNode node = builder.build();

        assertThat( accept( node, "com.example.test:testArtifact:jar" ) ).isTrue();
        assertThat( accept( node, "com.example.test:testArtifact" ) ).isTrue();
        assertThat( accept( node, "com.example.test" ) ).isTrue();

        assertThat( accept( node, "com.example.test" ) ).isTrue();
    }

    @Test
    public void acceptTestMissmatch()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );
        DependencyNode node = builder.build();

        assertThat( accept( node, "OTHER.GROUP.ID:testArtifact:jar:1.0.3" ) ).isFalse();
        assertThat( accept( node, "com.example.test:OTHER_ARTIFACT:jar:1.0.3" ) ).isFalse();
        assertThat( accept( node, "com.example.test:OTHER_ARTIFACT:jar:1.0.3" ) ).isFalse();
        assertThat( accept( node, "com.example.test:testArtifact:WAR:1.0.3" ) ).isFalse();
        assertThat( accept( node, "com.example.test:testArtifact:jar:SNAPSHOT" ) ).isFalse();

        assertThat( accept( node, "*:*:war:*" ) ).isFalse();
        assertThat( accept( node, "OTHER.GROUP.ID" ) ).isFalse();
    }

    @Test
    public void acceptTestMoreToken()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );

        DependencyNode node = builder.build();
        assertThat( accept( node, "com.example.test:testArtifact:jar:1.0.3:foo" ) ).isFalse();
    }

    @Test
    public void acceptTestRange()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );
        DependencyNode node = builder.build();

        String prefix = "com.example.test:testArtifact:jar:";

        assertThat( acceptVersionRange( node, prefix + "[1.0.3,1.0.4)" ) ).isTrue();
        assertThat( acceptVersionRange( node, prefix + "[1.0.3,)" ) ).isTrue();
        assertThat( acceptVersionRange( node, prefix + "[1.0.3,]" ) ).isTrue();
        assertThat( acceptVersionRange( node, prefix + "(,1.0.3]" ) ).isTrue();
        assertThat( acceptVersionRange( node, prefix + "[1.0,]" ) ).isTrue();
        assertThat( acceptVersionRange( node, prefix + "[1,4]" ) ).isTrue();
        assertThat( acceptVersionRange( node, prefix + "(1,4)" ) ).isTrue();

        assertThat( acceptVersionRange( node, prefix + "(1.0.2,1.0.3]", prefix + "(1.1,)" ) ).isTrue();

        assertThat( acceptVersionRange( node, prefix + "(1.0.3,2.0]" ) ).isFalse();
        assertThat( acceptVersionRange( node, prefix + "(1,1.0.2]" ) ).isFalse();

        assertThat( acceptVersionRange( node, prefix + "(1.0.2,1.0.3)", prefix + "(1.0.3,)" ) ).isFalse();
    }

    public boolean accept( DependencyNode node, String expression )
    {
        return new PatternInclusionsDependencyFilter( expression )
                .accept( node, new LinkedList<DependencyNode>() );
    }

    public boolean acceptVersionRange( DependencyNode node, String... expression )
    {
        return new PatternInclusionsDependencyFilter( new GenericVersionScheme(), expression )
                .accept( node, new LinkedList<DependencyNode>() );
    }

}
