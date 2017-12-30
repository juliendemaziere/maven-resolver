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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.internal.test.util.ArtifactDescription;
import org.eclipse.aether.internal.test.util.IniArtifactDataReader;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class IniArtifactDataReaderTest
{

    private IniArtifactDataReader parser;

    @Before
    public void setup()
        throws Exception
    {
        this.parser = new IniArtifactDataReader( "org/eclipse/aether/internal/test/util/" );
    }

    @Test
    public void testRelocation()
        throws IOException
    {
        String def = "[relocation]\ngid:aid:ext:ver";

        ArtifactDescription description = parser.parseLiteral( def );

        Artifact artifact = description.getRelocation();
        assertThat(artifact ).isNotNull();
        assertThat(artifact.getArtifactId() ).isEqualTo("aid");
        assertThat(artifact.getGroupId() ).isEqualTo("gid");
        assertThat(artifact.getVersion() ).isEqualTo("ver");
        assertThat(artifact.getExtension() ).isEqualTo("ext");
    }

    @Test
    public void testDependencies()
        throws IOException
    {
        String def = "[dependencies]\ngid:aid:ext:ver\n-exclusion:aid\ngid2:aid2:ext2:ver2";

        ArtifactDescription description = parser.parseLiteral( def );

        List<Dependency> dependencies = description.getDependencies();
        assertThat(dependencies ).isNotNull();
        assertThat(dependencies.size() ).isEqualTo(2);

        Dependency dependency = dependencies.get( 0 );
        assertThat(dependency.getScope() ).isEqualTo("compile");

        Artifact artifact = dependency.getArtifact();
        assertThat(artifact ).isNotNull();
        assertThat(artifact.getArtifactId() ).isEqualTo("aid");
        assertThat(artifact.getGroupId() ).isEqualTo("gid");
        assertThat(artifact.getVersion() ).isEqualTo("ver");
        assertThat(artifact.getExtension() ).isEqualTo("ext");

        Collection<Exclusion> exclusions = dependency.getExclusions();
        assertThat(exclusions ).isNotNull();
        assertThat(exclusions.size() ).isEqualTo(1);
        Exclusion exclusion = exclusions.iterator().next();
        assertThat(exclusion.getGroupId() ).isEqualTo("exclusion");
        assertThat(exclusion.getArtifactId() ).isEqualTo("aid");
        assertThat(exclusion.getClassifier() ).isEqualTo("*");
        assertThat(exclusion.getExtension() ).isEqualTo("*");

        dependency = dependencies.get( 1 );

        artifact = dependency.getArtifact();
        assertThat(artifact ).isNotNull();
        assertThat(artifact.getArtifactId() ).isEqualTo("aid2");
        assertThat(artifact.getGroupId() ).isEqualTo("gid2");
        assertThat(artifact.getVersion() ).isEqualTo("ver2");
        assertThat(artifact.getExtension() ).isEqualTo("ext2");
    }

    @Test
    public void testManagedDependencies()
        throws IOException
    {
        String def = "[managed-dependencies]\ngid:aid:ext:ver\n-exclusion:aid\ngid2:aid2:ext2:ver2:runtime";

        ArtifactDescription description = parser.parseLiteral( def );

        List<Dependency> dependencies = description.getManagedDependencies();
        assertThat(dependencies ).isNotNull();
        assertThat(dependencies.size() ).isEqualTo(2);

        Dependency dependency = dependencies.get( 0 );
        assertThat(dependency.getScope() ).isEqualTo("");

        Artifact artifact = dependency.getArtifact();
        assertThat(artifact ).isNotNull();
        assertThat(artifact.getArtifactId() ).isEqualTo("aid");
        assertThat(artifact.getGroupId() ).isEqualTo("gid");
        assertThat(artifact.getVersion() ).isEqualTo("ver");
        assertThat(artifact.getExtension() ).isEqualTo("ext");

        Collection<Exclusion> exclusions = dependency.getExclusions();
        assertThat(exclusions ).isNotNull();
        assertThat(exclusions.size() ).isEqualTo(1);
        Exclusion exclusion = exclusions.iterator().next();
        assertThat(exclusion.getGroupId() ).isEqualTo("exclusion");
        assertThat(exclusion.getArtifactId() ).isEqualTo("aid");
        assertThat(exclusion.getClassifier() ).isEqualTo("*");
        assertThat(exclusion.getExtension() ).isEqualTo("*");

        dependency = dependencies.get( 1 );
        assertThat(dependency.getScope() ).isEqualTo("runtime");

        artifact = dependency.getArtifact();
        assertThat(artifact ).isNotNull();
        assertThat(artifact.getArtifactId() ).isEqualTo("aid2");
        assertThat(artifact.getGroupId() ).isEqualTo("gid2");
        assertThat(artifact.getVersion() ).isEqualTo("ver2");
        assertThat(artifact.getExtension() ).isEqualTo("ext2");

        assertThat(dependency.getExclusions().size() ).isEqualTo(0);
    }

    @Test
    public void testResource()
        throws IOException
    {
        ArtifactDescription description = parser.parse( "ArtifactDataReaderTest.ini" );

        Artifact artifact = description.getRelocation();
        assertThat(artifact.getGroupId() ).isEqualTo("gid");
        assertThat(artifact.getArtifactId() ).isEqualTo("aid");
        assertThat(artifact.getVersion() ).isEqualTo("ver");
        assertThat(artifact.getExtension() ).isEqualTo("ext");

        assertThat(description.getRepositories().size() ).isEqualTo(1);
        RemoteRepository repo = description.getRepositories().get( 0 );
        assertThat(repo.getId() ).isEqualTo("id");
        assertThat(repo.getContentType() ).isEqualTo("type");
        assertThat(repo.getUrl() ).isEqualTo("protocol://some/url?for=testing");

        assertDependencies( description.getDependencies() );
        assertDependencies( description.getManagedDependencies() );

    }

    private void assertDependencies( List<Dependency> deps )
    {
        assertThat(deps.size() ).isEqualTo(4);

        Dependency dep = deps.get( 0 );
        assertThat(dep.getScope() ).isEqualTo("scope");
        assertThat(dep.isOptional() ).isEqualTo(false);
        assertThat(dep.getExclusions().size() ).isEqualTo(2);
        Iterator<Exclusion> it = dep.getExclusions().iterator();
        Exclusion excl = it.next();
        assertThat(excl.getGroupId() ).isEqualTo("gid3");
        assertThat(excl.getArtifactId() ).isEqualTo("aid");
        excl = it.next();
        assertThat(excl.getGroupId() ).isEqualTo("gid2");
        assertThat(excl.getArtifactId() ).isEqualTo("aid2");

        Artifact art = dep.getArtifact();
        assertThat(art.getGroupId() ).isEqualTo("gid");
        assertThat(art.getArtifactId() ).isEqualTo("aid");
        assertThat(art.getVersion() ).isEqualTo("ver");
        assertThat(art.getExtension() ).isEqualTo("ext");

        dep = deps.get( 1 );
        assertThat(dep.getScope() ).isEqualTo("scope");
        assertThat(dep.isOptional() ).isEqualTo(true);
        assertThat(dep.getExclusions().size() ).isEqualTo(0);

        art = dep.getArtifact();
        assertThat(art.getGroupId() ).isEqualTo("gid");
        assertThat(art.getArtifactId() ).isEqualTo("aid2");
        assertThat(art.getVersion() ).isEqualTo("ver");
        assertThat(art.getExtension() ).isEqualTo("ext");

        dep = deps.get( 2 );
        assertThat(dep.getScope() ).isEqualTo("scope");
        assertThat(dep.isOptional() ).isEqualTo(true);
        assertThat(dep.getExclusions().size() ).isEqualTo(0);

        art = dep.getArtifact();
        assertThat(art.getGroupId() ).isEqualTo("gid");
        assertThat(art.getArtifactId() ).isEqualTo("aid");
        assertThat(art.getVersion() ).isEqualTo("ver3");
        assertThat(art.getExtension() ).isEqualTo("ext");

        dep = deps.get( 3 );
        assertThat(dep.getScope() ).isEqualTo("scope5");
        assertThat(dep.isOptional() ).isEqualTo(true);
        assertThat(dep.getExclusions().size() ).isEqualTo(0);

        art = dep.getArtifact();
        assertThat(art.getGroupId() ).isEqualTo("gid1");
        assertThat(art.getArtifactId() ).isEqualTo("aid");
        assertThat(art.getVersion() ).isEqualTo("ver");
        assertThat(art.getExtension() ).isEqualTo("ext");
    }

}
