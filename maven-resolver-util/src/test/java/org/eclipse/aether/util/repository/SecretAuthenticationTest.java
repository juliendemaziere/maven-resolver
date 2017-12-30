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

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SecretAuthenticationTest
{

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
    public void testConstructor_CopyChars()
    {
        char[] value = { 'v', 'a', 'l' };
        new SecretAuthentication( "key", value );
        // TODO
        assertThat( value ).isEqualTo(new char[] { 'v', 'a', 'l' });
    }

    @Test
    public void testFill()
    {
        Authentication auth = new SecretAuthentication( "key", "value" );
        AuthenticationContext context = newContext( auth );
        assertThat( context.get( "another-key" ) ).isNull();
        assertThat( context.get( "key" ) ).isEqualTo( "value" );
    }

    @Test
    public void testDigest()
    {
        Authentication auth1 = new SecretAuthentication( "key", "value" );
        Authentication auth2 = new SecretAuthentication( "key", "value" );
        String digest1 = newDigest( auth1 );
        String digest2 = newDigest( auth2 );
        assertThat( digest2 ).isEqualTo( digest1 );

        Authentication auth3 = new SecretAuthentication( "key", "Value" );
        String digest3 = newDigest( auth3 );
        assertThat( digest3.equals( digest1 ) ).isFalse();

        Authentication auth4 = new SecretAuthentication( "Key", "value" );
        String digest4 = newDigest( auth4 );
        assertThat( digest4.equals( digest1 ) ).isFalse();
    }

    @Test
    public void testEquals()
    {
        Authentication auth1 = new SecretAuthentication( "key", "value" );
        Authentication auth2 = new SecretAuthentication( "key", "value" );
        Authentication auth3 = new SecretAuthentication( "key", "Value" );
        assertThat( auth2 ).isEqualTo(auth1);
        assertThat( auth1.equals( auth3 ) ).isFalse();
        assertThat( auth1.equals( null ) ).isFalse();
    }

    @Test
    public void testHashCode()
    {
        Authentication auth1 = new SecretAuthentication( "key", "value" );
        Authentication auth2 = new SecretAuthentication( "key", "value" );
        assertThat( auth2.hashCode() ).isEqualTo(auth1.hashCode());
    }

}
