package org.eclipse.aether.transport.http;

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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UriUtilsTest
{

    private String resolve( URI base, String ref )
    {
        return UriUtils.resolve( base, URI.create( ref ) ).toString();
    }

    @Test
    public void testResolve_BaseEmptyPath()
    {
        URI base = URI.create( "http://host" );
        assertThat( resolve( base, "file.jar" ) ).isEqualTo( "http://host/file.jar" );
        assertThat( resolve( base, "dir/file.jar")).isEqualTo( "http://host/dir/file.jar" );
        assertThat( resolve( base, "?arg=val" ) ).isEqualTo("http://host?arg=val" );
        assertThat( resolve( base, "file?arg=val")).isEqualTo("http://host/file?arg=val" );
        assertThat( resolve( base, "dir/file?arg=val")).isEqualTo("http://host/dir/file?arg=val" );
    }

    @Test
    public void testResolve_BaseRootPath()
    {
        URI base = URI.create( "http://host/" );
        assertThat( resolve( base, "file.jar" ) ).isEqualTo( "http://host/file.jar" );
        assertThat( resolve( base, "dir/file.jar")).isEqualTo( "http://host/dir/file.jar" );
        assertThat( resolve( base, "?arg=val" ) ).isEqualTo("http://host?arg=val" );
        assertThat( resolve( base, "file?arg=val")).isEqualTo("http://host/file?arg=val" );
        assertThat( resolve( base, "dir/file?arg=val")).isEqualTo("http://host/dir/file?arg=val" );
    }

    @Test
    public void testResolve_BasePathTrailingSlash()
    {
        URI base = URI.create( "http://host/sub/dir/" );
        assertThat( resolve( base, "file.jar" ) ).isEqualTo( "http://host/sub/dir/file.jar" );
        assertThat( resolve( base, "dir/file.jar")).isEqualTo( "http://host/sub/dir/dir/file.jar" );
        assertThat( resolve( base, "?arg=val" ) ).isEqualTo("http://host/sub/dir?arg=val" );
        assertThat( resolve( base, "file?arg=val")).isEqualTo("http://host/sub/dir/file?arg=val" );
        assertThat( resolve( base, "dir/file?arg=val")).isEqualTo("http://host/sub/dir/dir/file?arg=val" );
    }

    @Test
    public void testResolve_BasePathNoTrailingSlash()
    {
        URI base = URI.create( "http://host/sub/d%20r" );
        assertThat( resolve( base, "file.jar" ) ).isEqualTo( "http://host/sub/d%20r/file.jar" );
        assertThat( resolve( base, "dir/file.jar")).isEqualTo( "http://host/sub/d%20r/dir/file.jar" );
        assertThat( resolve( base, "?arg=val" ) ).isEqualTo("http://host/sub/d%20r?arg=val" );
        assertThat( resolve( base, "file?arg=val")).isEqualTo("http://host/sub/d%20r/file?arg=val" );
        assertThat( resolve( base, "dir/file?arg=val")).isEqualTo("http://host/sub/d%20r/dir/file?arg=val" );
    }

    private List<URI> getDirs( String base, String uri )
    {
        return UriUtils.getDirectories( ( base != null ) ? URI.create( base ) : null, URI.create( uri ) );
    }

    private void assertUris( List<URI> actual, String... expected )
    {
        List<String> uris = new ArrayList<String>( actual.size() );
        for ( URI uri : actual )
        {
            uris.add( uri.toString() );
        }
        assertThat(uris ).isEqualTo(Arrays.asList( expected ));
    }

    @Test
    public void testGetDirectories_NoBase()
    {
        List<URI> parents = getDirs( null, "http://host/repo/sub/dir/file.jar" );
        assertUris( parents, "http://host/repo/sub/dir/", "http://host/repo/sub/", "http://host/repo/" );

        parents = getDirs( null, "http://host/repo/sub/dir/?file.jar" );
        assertUris( parents, "http://host/repo/sub/dir/", "http://host/repo/sub/", "http://host/repo/" );

        parents = getDirs( null, "http://host/" );
        assertUris( parents );
    }

    @Test
    public void testGetDirectories_ExplicitBaseTrailingSlash()
    {
        List<URI> parents = getDirs( "http://host/repo/", "http://host/repo/sub/dir/file.jar" );
        assertUris( parents, "http://host/repo/sub/dir/", "http://host/repo/sub/" );

        parents = getDirs( "http://host/repo/", "http://host/repo/sub/dir/?file.jar" );
        assertUris( parents, "http://host/repo/sub/dir/", "http://host/repo/sub/" );

        parents = getDirs( "http://host/repo/", "http://host/" );
        assertUris( parents );
    }

    @Test
    public void testGetDirectories_ExplicitBaseNoTrailingSlash()
    {
        List<URI> parents = getDirs( "http://host/repo", "http://host/repo/sub/dir/file.jar" );
        assertUris( parents, "http://host/repo/sub/dir/", "http://host/repo/sub/" );

        parents = getDirs( "http://host/repo", "http://host/repo/sub/dir/?file.jar" );
        assertUris( parents, "http://host/repo/sub/dir/", "http://host/repo/sub/" );

        parents = getDirs( "http://host/repo", "http://host/" );
        assertUris( parents );
    }

}
