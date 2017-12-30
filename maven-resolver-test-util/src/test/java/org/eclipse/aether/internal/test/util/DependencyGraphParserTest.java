package org.eclipse.aether.internal.test.util;

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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.test.util.DependencyGraphParser;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class DependencyGraphParserTest
{

    private DependencyGraphParser parser;

    @Before
    public void setup()
    {
        this.parser = new DependencyGraphParser();
    }

    @Test
    public void testOnlyRoot()
        throws IOException
    {
        String def = "gid:aid:jar:1 scope";

        DependencyNode node = parser.parseLiteral( def );

        assertThat(node ).isNotNull();
        assertThat(node.getChildren().size() ).isEqualTo(0);

        Dependency dependency = node.getDependency();
        assertThat(dependency ).isNotNull();
        assertThat(dependency.getScope() ).isEqualTo("scope");

        Artifact artifact = dependency.getArtifact();
        assertThat(artifact ).isNotNull();

        assertThat(artifact.getGroupId() ).isEqualTo("gid");
        assertThat(artifact.getArtifactId() ).isEqualTo("aid");
        assertThat(artifact.getExtension() ).isEqualTo("jar");
        assertThat(artifact.getVersion() ).isEqualTo("1");
    }

    @Test
    public void testOptionalScope()
        throws IOException
    {
        String def = "gid:aid:jar:1";

        DependencyNode node = parser.parseLiteral( def );

        assertThat(node ).isNotNull();
        assertThat(node.getChildren().size() ).isEqualTo(0);

        Dependency dependency = node.getDependency();
        assertThat(dependency ).isNotNull();
        assertThat(dependency.getScope() ).isEqualTo("");
    }

    @Test
    public void testWithChildren()
        throws IOException
    {
        String def =
            "gid1:aid1:ext1:ver1 scope1\n" + "+- gid2:aid2:ext2:ver2 scope2\n" + "\\- gid3:aid3:ext3:ver3 scope3\n";

        DependencyNode node = parser.parseLiteral( def );
        assertThat(node ).isNotNull();

        int idx = 1;

        assertNodeProperties( node, idx++ );

        List<DependencyNode> children = node.getChildren();
        assertThat(children.size() ).isEqualTo(2);

        for ( DependencyNode child : children )
        {
            assertNodeProperties( child, idx++ );
        }

    }

    @Test
    public void testDeepChildren()
        throws IOException
    {
        String def =
            "gid1:aid1:ext1:ver1\n" + "+- gid2:aid2:ext2:ver2 scope2\n" + "|  \\- gid3:aid3:ext3:ver3\n"
                + "\\- gid4:aid4:ext4:ver4 scope4";

        DependencyNode node = parser.parseLiteral( def );
        assertNodeProperties( node, 1 );

        assertThat(node.getChildren().size() ).isEqualTo(2);
        assertNodeProperties( node.getChildren().get( 1 ), 4 );
        DependencyNode lvl1Node = node.getChildren().get( 0 );
        assertNodeProperties( lvl1Node, 2 );

        assertThat(lvl1Node.getChildren().size() ).isEqualTo(1);
        assertNodeProperties( lvl1Node.getChildren().get( 0 ), 3 );
    }

    private void assertNodeProperties( DependencyNode node, int idx )
    {
        assertNodeProperties( node, String.valueOf( idx ) );
    }

    private void assertNodeProperties( DependencyNode node, String suffix )
    {
        assertThat(node ).isNotNull();
        Dependency dependency = node.getDependency();
        assertThat(dependency ).isNotNull();
        if ( !"".equals( dependency.getScope() ) )
        {
            assertThat(dependency.getScope() ).isEqualTo("scope" + suffix);
        }

        Artifact artifact = dependency.getArtifact();
        assertThat(artifact ).isNotNull();

        assertThat(artifact.getGroupId() ).isEqualTo("gid" + suffix);
        assertThat(artifact.getArtifactId() ).isEqualTo("aid" + suffix);
        assertThat(artifact.getExtension() ).isEqualTo("ext" + suffix);
        assertThat(artifact.getVersion() ).isEqualTo("ver" + suffix);
    }

    @Test
    public void testComments()
        throws IOException
    {
        String def = "# first line\n#second line\ngid:aid:ext:ver # root artifact asdf:qwer:zcxv:uip";

        DependencyNode node = parser.parseLiteral( def );

        assertNodeProperties( node, "" );
    }

    @Test
    public void testId()
        throws IOException
    {
        String def = "gid:aid:ext:ver (id)\n\\- ^id";
        DependencyNode node = parser.parseLiteral( def );
        assertNodeProperties( node, "" );

        assertThat(node.getChildren() ).isNotNull();
        assertThat(node.getChildren().size() ).isEqualTo(1);

        assertThat(node.getChildren().get( 0 ) ).isSameAs(node);
    }

    @Test
    public void testResourceLoading()
        throws IOException
    {
        String prefix = "org/eclipse/aether/internal/test/util/";
        String name = "testResourceLoading.txt";

        DependencyNode node = parser.parseResource( prefix + name );
        assertThat(node.getChildren().size() ).isEqualTo(0);
        assertNodeProperties( node, "" );
    }

    @Test
    public void testResourceLoadingWithPrefix()
        throws IOException
    {
        String prefix = "org/eclipse/aether/internal/test/util/";
        parser = new DependencyGraphParser( prefix );

        String name = "testResourceLoading.txt";

        DependencyNode node = parser.parseResource( name );
        assertThat(node.getChildren().size() ).isEqualTo(0);
        assertNodeProperties( node, "" );
    }

    @Test
    public void testProperties()
        throws IOException
    {
        String def = "gid:aid:ext:ver props=test:foo,test2:fizzle";
        DependencyNode node = parser.parseLiteral( def );

        assertNodeProperties( node, "" );

        Map<String, String> properties = node.getDependency().getArtifact().getProperties();
        assertThat(properties ).isNotNull();
        assertThat(properties.size() ).isEqualTo(2);

        assertThat(properties.containsKey( "test" ) ).isTrue();
        assertThat(properties.get( "test" ) ).isEqualTo("foo");
        assertThat(properties.containsKey( "test2" ) ).isTrue();
        assertThat(properties.get( "test2" ) ).isEqualTo("fizzle");
    }

    @Test
    public void testSubstitutions()
        throws IOException
    {
        parser.setSubstitutions( Arrays.asList( "subst1", "subst2" ) );
        String def = "%s:%s:ext:ver";
        DependencyNode root = parser.parseLiteral( def );
        Artifact artifact = root.getDependency().getArtifact();
        assertThat(artifact.getArtifactId() ).isEqualTo("subst2");
        assertThat(artifact.getGroupId() ).isEqualTo("subst1");

        def = "%s:aid:ext:ver\n\\- %s:aid:ext:ver";
        root = parser.parseLiteral( def );

        assertThat(root.getDependency().getArtifact().getGroupId() ).isEqualTo("subst1");
        assertThat(root.getChildren().get( 0 ).getDependency().getArtifact().getGroupId() ).isEqualTo("subst2");
    }

    @Test
    public void testMultiple()
        throws IOException
    {
        String prefix = "org/eclipse/aether/internal/test/util/";
        String name = "testResourceLoading.txt";

        List<DependencyNode> nodes = parser.parseMultiResource( prefix + name );

        assertThat(nodes.size() ).isEqualTo(2);
        assertThat(nodes.get( 0 ).getDependency().getArtifact().getArtifactId() ).isEqualTo("aid");
        assertThat(nodes.get( 1 ).getDependency().getArtifact().getArtifactId() ).isEqualTo("aid2");
    }

    @Test
    public void testRootNullDependency()
        throws IOException
    {
        String literal = "(null)\n+- gid:aid:ext:ver";
        DependencyNode root = parser.parseLiteral( literal );

        assertThat(root.getDependency() ).isNull();
        assertThat(root.getChildren().size() ).isEqualTo(1);
    }

    @Test
    public void testChildNullDependency()
        throws IOException
    {
        String literal = "gid:aid:ext:ver\n+- (null)";
        DependencyNode root = parser.parseLiteral( literal );

        assertThat(root.getDependency() ).isNotNull();
        assertThat(root.getChildren().size() ).isEqualTo(1);
        assertThat(root.getChildren().get( 0 ).getDependency() ).isNull();
    }

    @Test
    public void testOptional()
        throws IOException
    {
        String def = "gid:aid:jar:1 compile optional";

        DependencyNode node = parser.parseLiteral( def );

        assertThat(node ).isNotNull();
        assertThat(node.getChildren().size() ).isEqualTo(0);

        Dependency dependency = node.getDependency();
        assertThat(dependency ).isNotNull();
        assertThat(dependency.getScope() ).isEqualTo("compile");
        assertThat(dependency.isOptional() ).isEqualTo(true);
    }

}
