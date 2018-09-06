package org.eclipse.aether.util.repository;

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

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;

public class ComponentAuthenticationTest
{

    private static class Component
    {
    }

    private RepositorySystemSession newSession()
    {
        return new DefaultRepositorySystemSession();
    }

    private RemoteRepository newRepo( Authentication auth )
    {
        return new RemoteRepository.Builder( "test", "default", "http://localhost" ).setAuthentication( auth ).build();
    }

    private AuthenticationContext newContext( Authentication auth )
    {
        return AuthenticationContext.forRepository( newSession(), newRepo( auth ) );
    }

    private String newDigest( Authentication auth )
    {
        return AuthenticationDigest.forRepository( newSession(), newRepo( auth ) );
    }

    @Test
    public void testFill()
    {
        Component comp = new Component();
        Authentication auth = new ComponentAuthentication( "key", comp );
        AuthenticationContext context = newContext( auth );
        assertThat( context.get( "another-key" ) ).isNull();
        assertThat( context.get( "key", Component.class )).isSameAs( comp );
    }

    @Test
    public void testDigest()
    {
        Authentication auth1 = new ComponentAuthentication( "key", new Component() );
        Authentication auth2 = new ComponentAuthentication( "key", new Component() );
        String digest1 = newDigest( auth1 );
        String digest2 = newDigest( auth2 );
        assertThat(digest2 ).isEqualTo(digest1);

        Authentication auth3 = new ComponentAuthentication( "key", new Object() );
        String digest3 = newDigest( auth3 );
        assertThat(digest3.equals( digest1 ) ).isFalse();

        Authentication auth4 = new ComponentAuthentication( "Key", new Component() );
        String digest4 = newDigest( auth4 );
        assertThat(digest4.equals( digest1 ) ).isFalse();
    }

    @Test
    public void testEquals()
    {
        Authentication auth1 = new ComponentAuthentication( "key", new Component() );
        Authentication auth2 = new ComponentAuthentication( "key", new Component() );
        Authentication auth3 = new ComponentAuthentication( "key", new Object() );
        assertThat(auth2 ).isEqualTo(auth1);
        assertThat(auth1.equals( auth3 ) ).isFalse();
        assertThat(auth1.equals( null ) ).isFalse();
    }

    @Test
    public void testHashCode()
    {
        Authentication auth1 = new ComponentAuthentication( "key", new Component() );
        Authentication auth2 = new ComponentAuthentication( "key", new Component() );
        assertThat(auth2.hashCode() ).isEqualTo(auth1.hashCode());
    }

}
