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

import java.util.Map;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class AuthenticationDigestTest
{

    private RepositorySystemSession newSession()
    {
        return new DefaultRepositorySystemSession();
    }

    private RemoteRepository newRepo( Authentication auth, Proxy proxy )
    {
        return new RemoteRepository.Builder( "test", "default", "http://localhost" ) //
        .setAuthentication( auth ).setProxy( proxy ).build();
    }

    private Proxy newProxy( Authentication auth )
    {
        return new Proxy( Proxy.TYPE_HTTP, "localhost", 8080, auth );
    }

    @Test
    public void testForRepository()
    {
        final RepositorySystemSession session = newSession();
        final RemoteRepository[] repos = { null };

        Authentication auth = new Authentication()
        {
            public void fill( AuthenticationContext context, String key, Map<String, String> data )
            {
                fail( "AuthenticationDigest should not call fill()" );
            }

            public void digest( AuthenticationDigest digest )
            {
                assertThat( digest ).isNotNull();
                assertThat( digest.getSession() ).isSameAs(session);
                assertThat( digest.getRepository() ).isNotNull();
                assertThat( digest.getProxy() ).isNull();
                assertThat( repos[0] ).as( "digest() should only be called once" ).isNull();
                repos[0] = digest.getRepository();

                digest.update( (byte[]) null );
                digest.update( (char[]) null );
                digest.update( (String[]) null );
                digest.update( null, null );
            }
        };

        RemoteRepository repo = newRepo( auth, newProxy( null ) );

        String digest = AuthenticationDigest.forRepository( session, repo );
        assertThat(repos[0] ).isSameAs(repo);
        assertThat(digest ).isNotNull();
        assertThat(digest.length() > 0 ).isTrue();
    }

    @Test
    public void testForRepository_NoAuth()
    {
        RemoteRepository repo = newRepo( null, null );

        String digest = AuthenticationDigest.forRepository( newSession(), repo );
        assertThat(digest ).isEqualTo("");
    }

    @Test
    public void testForProxy()
    {
        final RepositorySystemSession session = newSession();
        final Proxy[] proxies = { null };

        Authentication auth = new Authentication()
        {
            public void fill( AuthenticationContext context, String key, Map<String, String> data )
            {
                fail( "AuthenticationDigest should not call fill()" );
            }

            public void digest( AuthenticationDigest digest )
            {
                assertThat(digest ).isNotNull();
                assertThat(digest.getSession() ).isSameAs(session);
                assertThat(digest.getRepository() ).isNotNull();
                assertThat(digest.getProxy() ).isNotNull();
                assertThat( proxies[0] ).as( "digest() should only be called once" ).isNull();
                proxies[0] = digest.getProxy();

                digest.update( (byte[]) null );
                digest.update( (char[]) null );
                digest.update( (String[]) null );
                digest.update( null, null );
            }
        };

        Proxy proxy = newProxy( auth );

        String digest = AuthenticationDigest.forProxy( session, newRepo( null, proxy ) );
        assertThat(proxies[0] ).isSameAs(proxy);
        assertThat(digest ).isNotNull();
        assertThat(digest.length() > 0 ).isTrue();
    }

    @Test
    public void testForProxy_NoProxy()
    {
        RemoteRepository repo = newRepo( null, null );

        String digest = AuthenticationDigest.forProxy( newSession(), repo );
        assertThat(digest ).isEqualTo("");
    }

    @Test
    public void testForProxy_NoProxyAuth()
    {
        RemoteRepository repo = newRepo( null, newProxy( null ) );

        String digest = AuthenticationDigest.forProxy( newSession(), repo );
        assertThat(digest ).isEqualTo("");
    }

}
