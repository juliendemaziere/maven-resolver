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
import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;
import org.junit.Test;

public class PatternExclusionsDependencyFilterTest
{

    @Test
    public void acceptTestCornerCases()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.artifactId( "testArtifact" );
        DependencyNode node = builder.build();
        List<DependencyNode> parents = new LinkedList<DependencyNode>();

        // Empty String, Empty List
        assertThat(dontAccept( node, "" ) ).isTrue();
        assertThat(new PatternExclusionsDependencyFilter( new LinkedList<String>() ).accept( node, parents ) ).isTrue();
        assertThat(new PatternExclusionsDependencyFilter( (String[]) null ).accept( node, parents ) ).isTrue();
        assertThat(new PatternExclusionsDependencyFilter( (VersionScheme) null, "[1,10]" ).accept( node, parents ) ).isTrue();
    }

    @Test
    public void acceptTestMatches()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );
        DependencyNode node = builder.build();

        // full match
        assertThat( dontAccept( node, "com.example.test:testArtifact:jar:1.0.3" ) )
                .as("com.example.test:testArtifact:jar:1.0.3").isTrue();

        // single wildcard
        assertThat( dontAccept( node, "*:testArtifact:jar:1.0.3" ) ).isTrue();
        assertThat( dontAccept( node, "com.example.test:*:jar:1.0.3" ) ).isTrue();
        assertThat( dontAccept( node, "com.example.test:testArtifact:*:1.0.3" ) ).isTrue();
        assertThat( dontAccept( node, "com.example.test:testArtifact:*:1.0.3" ) );

        // implicit wildcard
        assertThat( dontAccept( node, ":testArtifact:jar:1.0.3")).isTrue();
        assertThat( dontAccept( node, "com.example.test::jar:1.0.3")).isTrue();
        assertThat( dontAccept( node, "com.example.test:testArtifact::1.0.3" ) ).isTrue();
        assertThat( dontAccept( node, "com.example.test:testArtifact:jar:" ) ).isTrue();

        // multi wildcards
        assertThat( dontAccept( node, "*:*:jar:1.0.3" )).isTrue();
        assertThat( dontAccept( node, "com.example.test:*:*:1.0.3")).isTrue();
        assertThat( dontAccept( node, "com.example.test:testArtifact:*:*")).isTrue();
        assertThat( dontAccept( node, "*:testArtifact:jar:*")).isTrue();
        assertThat( dontAccept( node, "*:*:jar:*")).isTrue();
        assertThat(dontAccept( node, ":*:jar:")).isTrue();

        // partial wildcards
        assertThat( dontAccept( node, "*.example.test:testArtifact:jar:1.0.3" ) ).isTrue();
        assertThat( dontAccept( node, "com.example.test:testArtifact:*ar:1.0.*" ) ).isTrue();
        assertThat( dontAccept( node, "com.example.test:testArtifact:jar:1.0.*" ) ).isTrue();
        assertThat( dontAccept( node, "*.example.*:testArtifact:jar:1.0.3" ) ).isTrue();

        // wildcard as empty string
        assertThat( dontAccept( node, "com.example.test*:testArtifact:jar:1.0.3" ) ).isTrue();
    }

    @Test
    public void acceptTestLessToken()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );
        DependencyNode node = builder.build();

        assertThat( dontAccept( node, "com.example.test:testArtifact:jar")).isTrue();
        assertThat( dontAccept( node, "com.example.test:testArtifact")).isTrue();
        assertThat( dontAccept( node, "com.example.test")).isTrue();

        assertThat( dontAccept( node, "com.example.foo")).isFalse();
    }

    @Test
    public void acceptTestMissmatch()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );
        DependencyNode node = builder.build();

        assertThat( dontAccept( node, "OTHER.GROUP.ID:testArtifact:jar:1.0.3" ) ).isFalse();
        assertThat( dontAccept( node, "com.example.test:OTHER_ARTIFACT:jar:1.0.3" ) ).isFalse();
        assertThat( dontAccept( node, "com.example.test:OTHER_ARTIFACT:jar:1.0.3" ) ).isFalse();
        assertThat( dontAccept( node, "com.example.test:testArtifact:WAR:1.0.3" ) ).isFalse();
        assertThat( dontAccept( node, "com.example.test:testArtifact:jar:SNAPSHOT" ) ).isFalse();

        assertThat( dontAccept( node, "*:*:war:*")).isFalse();
        assertThat( dontAccept( node, "OTHER.GROUP.ID")).isFalse();
    }

    @Test
    public void acceptTestMoreToken()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );

        DependencyNode node = builder.build();
        assertThat( dontAccept( node, "com.example.test:testArtifact:jar:1.0.3:foo" ) ).as("com.example.test:testArtifact:jar:1.0.3:foo").isFalse();
    }

    @Test
    public void acceptTestRange()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.groupId( "com.example.test" ).artifactId( "testArtifact" ).ext( "jar" ).version( "1.0.3" );
        DependencyNode node = builder.build();

        String prefix = "com.example.test:testArtifact:jar:";

        assertThat( dontAcceptVersionRange( node, prefix + "[1.0.3,1.0.4)" ) ).as(prefix + "[1.0.3,1.0.4)").isTrue();
        assertThat( dontAcceptVersionRange( node, prefix + "[1.0.3,)" ) ).as(prefix + "[1.0.3,)").isTrue();
        assertThat( dontAcceptVersionRange( node, prefix + "[1.0.3,]" ) ).isTrue();
        assertThat( dontAcceptVersionRange( node, prefix + "(,1.0.3]" ) ).isTrue();
        assertThat( dontAcceptVersionRange( node, prefix + "[1.0,]" ) ).isTrue();
        assertThat( dontAcceptVersionRange( node, prefix + "[1,4]" ) ).isTrue();
        assertThat( dontAcceptVersionRange( node, prefix + "(1,4)" ) ).isTrue();

        assertThat( dontAcceptVersionRange( node, prefix + "(1.0.2,1.0.3]", prefix + "(1.1,)" ) ).as(prefix + "(1.0.2,1.0.3]").isTrue();

        assertThat( dontAcceptVersionRange( node, prefix + "(1.0.3,2.0]" ) ).isFalse();
        assertThat( dontAcceptVersionRange( node, prefix + "(1,1.0.2]" ) ).isFalse();

        assertThat( dontAcceptVersionRange( node, prefix + "(1.0.2,1.0.3)", prefix + "(1.0.3,)" ) ).isFalse();
    }

    private boolean dontAccept( DependencyNode node, String expression )
    {
        return !new PatternExclusionsDependencyFilter( expression )
                .accept( node, new LinkedList<DependencyNode>() );
    }

    private boolean dontAcceptVersionRange( DependencyNode node, String... expression )
    {
        return !new PatternExclusionsDependencyFilter( new GenericVersionScheme(), expression )
                .accept( node, new LinkedList<DependencyNode>() );
    }

}
