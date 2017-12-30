package org.eclipse.aether;

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

import java.util.Map;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;

/**
 */
public class DefaultRepositorySystemSessionTest
{

    @Test
    public void testDefaultProxySelectorUsesExistingProxy()
    {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();

        RemoteRepository repo = new RemoteRepository.Builder( "id", "default", "void" ).build();
        assertThat(session.getProxySelector().getProxy( repo ) ).isSameAs(null);

        Proxy proxy = new Proxy( "http", "localhost", 8080, null );
        repo = new RemoteRepository.Builder( repo ).setProxy( proxy ).build();
        assertThat(session.getProxySelector().getProxy( repo ) ).isSameAs(proxy);
    }

    @Test
    public void testDefaultAuthenticationSelectorUsesExistingAuth()
    {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();

        RemoteRepository repo = new RemoteRepository.Builder( "id", "default", "void" ).build();
        assertThat(session.getAuthenticationSelector().getAuthentication( repo ) ).isSameAs(null);

        Authentication auth = new Authentication()
        {
            public void fill( AuthenticationContext context, String key, Map<String, String> data )
            {
            }

            public void digest( AuthenticationDigest digest )
            {
            }
        };
        repo = new RemoteRepository.Builder( repo ).setAuthentication( auth ).build();
        assertThat(session.getAuthenticationSelector().getAuthentication( repo ) ).isSameAs(auth);
    }

    @Test
    public void testCopyConstructorCopiesPropertiesDeep()
    {
        DefaultRepositorySystemSession session1 = new DefaultRepositorySystemSession();
        session1.setUserProperties( System.getProperties() );
        session1.setSystemProperties( System.getProperties() );
        session1.setConfigProperties( System.getProperties() );

        DefaultRepositorySystemSession session2 = new DefaultRepositorySystemSession( session1 );
        session2.setUserProperty( "key", "test" );
        session2.setSystemProperty( "key", "test" );
        session2.setConfigProperty( "key", "test" );

        assertThat(session1.getUserProperties().get( "key" ) ).isEqualTo(null);
        assertThat(session1.getSystemProperties().get( "key" ) ).isEqualTo(null);
        assertThat(session1.getConfigProperties().get( "key" ) ).isEqualTo(null);
    }

    @Test
    public void testReadOnlyProperties()
    {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();

        try
        {
            session.getUserProperties().put( "key", "test" );
            fail( "user properties are modifiable" );
        }
        catch ( UnsupportedOperationException e )
        {
            // expected
        }

        try
        {
            session.getSystemProperties().put( "key", "test" );
            fail( "system properties are modifiable" );
        }
        catch ( UnsupportedOperationException e )
        {
            // expected
        }

        try
        {
            session.getConfigProperties().put( "key", "test" );
            fail( "config properties are modifiable" );
        }
        catch ( UnsupportedOperationException e )
        {
            // expected
        }
    }

}
