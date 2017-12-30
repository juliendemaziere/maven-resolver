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

import java.io.File;
import java.util.Map;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class AuthenticationContextTest
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

    private Authentication newAuth()
    {
        return new Authentication()
        {
            public void fill( AuthenticationContext context, String key, Map<String, String> data )
            {
                assertThat( context ).isNotNull();
                assertThat( context.getSession() ).isNotNull();
                assertThat( context.getRepository() ).isNotNull();
                assertThat( context.get( "key" ) ).as( "fill() should only be called once" ).isNull();
                context.put( "key", "value" );
            }

            public void digest( AuthenticationDigest digest )
            {
                fail( "AuthenticationContext should not call digest()" );
            }
        };
    }

    @Test
    public void testForRepository()
    {
        RepositorySystemSession session = newSession();
        RemoteRepository repo = newRepo( newAuth(), newProxy( newAuth() ) );
        AuthenticationContext context = AuthenticationContext.forRepository( session, repo );
        assertThat( context ).isNotNull();
        assertThat( context.getSession() ).isSameAs(session);
        assertThat( context.getRepository() ).isSameAs(repo);
        assertThat( context.getProxy() ).isNull();
        assertThat( context.get( "key" ) ).isEqualTo("value");
        assertThat( context.get( "key" ) ).isEqualTo("value");
    }

    @Test
    public void testForRepository_NoAuth()
    {
        RepositorySystemSession session = newSession();
        RemoteRepository repo = newRepo( null, newProxy( newAuth() ) );
        AuthenticationContext context = AuthenticationContext.forRepository( session, repo );
        assertThat( context ).isNull();
    }

    @Test
    public void testForProxy()
    {
        RepositorySystemSession session = newSession();
        Proxy proxy = newProxy( newAuth() );
        RemoteRepository repo = newRepo( newAuth(), proxy );
        AuthenticationContext context = AuthenticationContext.forProxy( session, repo );
        assertThat( context ).isNotNull();
        assertThat( context.getSession() ).isSameAs(session);
        assertThat( context.getRepository() ).isSameAs(repo);
        assertThat( context.getProxy() ).isSameAs(proxy);
        assertThat( context.get( "key" ) ).isEqualTo("value");
        assertThat( context.get( "key" ) ).isEqualTo("value");
    }

    @Test
    public void testForProxy_NoProxy()
    {
        RepositorySystemSession session = newSession();
        Proxy proxy = null;
        RemoteRepository repo = newRepo( newAuth(), proxy );
        AuthenticationContext context = AuthenticationContext.forProxy( session, repo );
        assertThat( context ).isNull();
    }

    @Test
    public void testForProxy_NoProxyAuth()
    {
        RepositorySystemSession session = newSession();
        Proxy proxy = newProxy( null );
        RemoteRepository repo = newRepo( newAuth(), proxy );
        AuthenticationContext context = AuthenticationContext.forProxy( session, repo );
        assertThat( context ).isNull();
    }

    @Test
    public void testGet_StringVsChars()
    {
        AuthenticationContext context = AuthenticationContext.forRepository( newSession(), newRepo( newAuth(), null ) );
        context.put( "key", new char[] { 'v', 'a', 'l', '1' } );
        assertThat( context.get( "key" ) ).isEqualTo("val1");
        context.put( "key", "val2" );
        assertThat( context.get( "key", char[].class)).isEqualTo( new char[] { 'v', 'a', 'l', '2' } );
    }

    @Test
    public void testGet_StringVsFile()
    {
        AuthenticationContext context = AuthenticationContext.forRepository( newSession(), newRepo( newAuth(), null ) );
        context.put( "key", "val1" );
        assertThat( context.get( "key", File.class )).isEqualTo( new File( "val1" ));
        context.put( "key", new File( "val2" ) );
        assertThat(context.get( "key" ) ).isEqualTo("val2");
    }

    @Test
    public void testPut_EraseCharArrays()
    {
        AuthenticationContext context = AuthenticationContext.forRepository( newSession(), newRepo( newAuth(), null ) );
        char[] secret = { 'v', 'a', 'l', 'u', 'e' };
        context.put( "key", secret );
        context.put( "key", secret.clone() );
        assertThat( secret ).isEqualTo( new char[] { 0, 0, 0, 0, 0 } );
    }

    @Test
    public void testClose_EraseCharArrays()
    {
        AuthenticationContext.close( null );

        AuthenticationContext context = AuthenticationContext.forRepository( newSession(), newRepo( newAuth(), null ) );
        char[] secret = { 'v', 'a', 'l', 'u', 'e' };
        context.put( "key", secret );
        AuthenticationContext.close( context );
        assertThat( secret ).isEqualTo( new char[] { 0, 0, 0, 0, 0 } );
    }

}
