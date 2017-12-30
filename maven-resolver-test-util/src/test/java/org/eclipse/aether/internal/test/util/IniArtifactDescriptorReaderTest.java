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
import java.util.Iterator;
import java.util.List;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.internal.test.util.IniArtifactDescriptorReader;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class IniArtifactDescriptorReaderTest
{

    private IniArtifactDescriptorReader reader;

    private RepositorySystemSession session;

    @Before
    public void setup()
        throws IOException
    {
        reader = new IniArtifactDescriptorReader( "org/eclipse/aether/internal/test/util/" );
        session = TestUtils.newSession();
    }

    @Test( expected = ArtifactDescriptorException.class )
    public void testMissingDescriptor()
        throws ArtifactDescriptorException
    {
        Artifact art = new DefaultArtifact( "missing:aid:ver:ext" );
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest( art, null, "" );
        reader.readArtifactDescriptor( session, request );
    }

    @Test
    public void testLookup()
        throws ArtifactDescriptorException
    {
        Artifact art = new DefaultArtifact( "gid:aid:ext:ver" );
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest( art, null, "" );
        ArtifactDescriptorResult description = reader.readArtifactDescriptor( session, request );

        assertThat(description.getRequest() ).isEqualTo(request);
        assertThat(description.getArtifact() ).isEqualTo(art.setVersion( "1" ));

        assertThat(description.getRelocations().size() ).isEqualTo(1);
        Artifact artifact = description.getRelocations().get( 0 );
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
