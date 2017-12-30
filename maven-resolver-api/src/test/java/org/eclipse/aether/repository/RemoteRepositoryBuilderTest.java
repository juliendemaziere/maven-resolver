package org.eclipse.aether.repository;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.repository.RemoteRepository.Builder;
import org.junit.Before;
import org.junit.Test;

public class RemoteRepositoryBuilderTest
{

    private RemoteRepository prototype;

    @Before
    public void init()
    {
        prototype = new Builder( "id", "type", "file:void" ).build();
    }

    @Test
    public void testReusePrototype()
    {
        Builder builder = new Builder( prototype );
        assertThat(builder.build() ).isSameAs(prototype);
    }

    @Test( expected = NullPointerException.class )
    public void testPrototypeMandatory()
    {
        new Builder( null );
    }

    @Test
    public void testSetId()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setId( prototype.getId() ).build();
        assertThat(repo ).isSameAs(prototype);
        repo = builder.setId( "new-id" ).build();
        assertThat(repo.getId() ).isEqualTo("new-id");
    }

    @Test
    public void testSetContentType()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setContentType( prototype.getContentType() ).build();
        assertThat(repo ).isSameAs(prototype);
        repo = builder.setContentType( "new-type" ).build();
        assertThat(repo.getContentType() ).isEqualTo("new-type");
    }

    @Test
    public void testSetUrl()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setUrl( prototype.getUrl() ).build();
        assertThat(repo ).isSameAs(prototype);
        repo = builder.setUrl( "file:new" ).build();
        assertThat(repo.getUrl() ).isEqualTo("file:new");
    }

    @Test
    public void testSetPolicy()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setPolicy( prototype.getPolicy( false ) ).build();
        assertThat(repo ).isSameAs(prototype);
        RepositoryPolicy policy = new RepositoryPolicy( true, "never", "fail" );
        repo = builder.setPolicy( policy ).build();
        assertThat(repo.getPolicy( true ) ).isEqualTo(policy);
        assertThat(repo.getPolicy( false ) ).isEqualTo(policy);
    }

    @Test
    public void testSetReleasePolicy()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setReleasePolicy( prototype.getPolicy( false ) ).build();
        assertThat(repo ).isSameAs(prototype);
        RepositoryPolicy policy = new RepositoryPolicy( true, "never", "fail" );
        repo = builder.setReleasePolicy( policy ).build();
        assertThat(repo.getPolicy( false ) ).isEqualTo(policy);
        assertThat(repo.getPolicy( true ) ).isEqualTo(prototype.getPolicy( true ));
    }

    @Test
    public void testSetSnapshotPolicy()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setSnapshotPolicy( prototype.getPolicy( true ) ).build();
        assertThat(repo ).isSameAs(prototype);
        RepositoryPolicy policy = new RepositoryPolicy( true, "never", "fail" );
        repo = builder.setSnapshotPolicy( policy ).build();
        assertThat(repo.getPolicy( true ) ).isEqualTo(policy);
        assertThat(repo.getPolicy( false ) ).isEqualTo(prototype.getPolicy( false ));
    }

    @Test
    public void testSetProxy()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setProxy( prototype.getProxy() ).build();
        assertThat(repo ).isSameAs(prototype);
        Proxy proxy = new Proxy( "http", "localhost", 8080 );
        repo = builder.setProxy( proxy ).build();
        assertThat(repo.getProxy() ).isEqualTo(proxy);
    }

    @Test
    public void testSetAuthentication()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setAuthentication( prototype.getAuthentication() ).build();
        assertThat(repo ).isSameAs(prototype);
        Authentication auth = new Authentication()
        {
            public void fill( AuthenticationContext context, String key, Map<String, String> data )
            {
            }

            public void digest( AuthenticationDigest digest )
            {
            }
        };
        repo = builder.setAuthentication( auth ).build();
        assertThat(repo.getAuthentication() ).isEqualTo(auth);
    }

    @Test
    public void testSetMirroredRepositories()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setMirroredRepositories( prototype.getMirroredRepositories() ).build();
        assertThat(repo ).isSameAs(prototype);
        List<RemoteRepository> mirrored = new ArrayList<RemoteRepository>( Arrays.asList( repo ) );
        repo = builder.setMirroredRepositories( mirrored ).build();
        assertThat(repo.getMirroredRepositories() ).isEqualTo(mirrored);
    }

    @Test
    public void testAddMirroredRepository()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.addMirroredRepository( null ).build();
        assertThat(repo ).isSameAs(prototype);
        repo = builder.addMirroredRepository( prototype ).build();
        assertThat(repo.getMirroredRepositories() ).isEqualTo(Arrays.asList( prototype ));
    }

    @Test
    public void testSetRepositoryManager()
    {
        Builder builder = new Builder( prototype );
        RemoteRepository repo = builder.setRepositoryManager( prototype.isRepositoryManager() ).build();
        assertThat(repo ).isSameAs(prototype);
        repo = builder.setRepositoryManager( !prototype.isRepositoryManager() ).build();
        assertThat(repo.isRepositoryManager() ).isEqualTo(!prototype.isRepositoryManager());
    }

}
