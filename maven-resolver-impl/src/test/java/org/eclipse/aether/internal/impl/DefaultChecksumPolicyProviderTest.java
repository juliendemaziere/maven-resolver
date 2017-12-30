package org.eclipse.aether.internal.impl;

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
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicy;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultChecksumPolicyProviderTest
{

    private static final String CHECKSUM_POLICY_UNKNOWN = "unknown";

    private DefaultRepositorySystemSession session;

    private DefaultChecksumPolicyProvider provider;

    private RemoteRepository repository;

    private TransferResource resource;

    @Before
    public void setup()
    {
        session = TestUtils.newSession();
        provider = new DefaultChecksumPolicyProvider();
        repository = new RemoteRepository.Builder( "test", "default", "file:/void" ).build();
        resource = new TransferResource( repository.getId(), repository.getUrl(), "file.txt", null, null );
    }

    @After
    public void teardown()
    {
        provider = null;
        session = null;
        repository = null;
        resource = null;
    }

    @Test
    public void testNewChecksumPolicy_Fail()
    {
        ChecksumPolicy policy =
            provider.newChecksumPolicy( session, repository, resource, RepositoryPolicy.CHECKSUM_POLICY_FAIL );
        assertThat(policy ).isNotNull();
        assertThat(policy.getClass() ).isEqualTo(FailChecksumPolicy.class);
    }

    @Test
    public void testNewChecksumPolicy_Warn()
    {
        ChecksumPolicy policy =
            provider.newChecksumPolicy( session, repository, resource, RepositoryPolicy.CHECKSUM_POLICY_WARN );
        assertThat(policy ).isNotNull();
        assertThat(policy.getClass() ).isEqualTo(WarnChecksumPolicy.class);
    }

    @Test
    public void testNewChecksumPolicy_Ignore()
    {
        ChecksumPolicy policy =
            provider.newChecksumPolicy( session, repository, resource, RepositoryPolicy.CHECKSUM_POLICY_IGNORE );
        assertThat( policy ).isNull();
    }

    @Test
    public void testNewChecksumPolicy_Unknown()
    {
        ChecksumPolicy policy = provider.newChecksumPolicy( session, repository, resource, CHECKSUM_POLICY_UNKNOWN );
        assertThat(policy ).isNotNull();
        assertThat(policy.getClass() ).isEqualTo(WarnChecksumPolicy.class);
    }

    @Test
    public void testGetEffectiveChecksumPolicy_EqualPolicies()
    {
        String[] policies =
            { RepositoryPolicy.CHECKSUM_POLICY_FAIL, RepositoryPolicy.CHECKSUM_POLICY_WARN,
                RepositoryPolicy.CHECKSUM_POLICY_IGNORE, CHECKSUM_POLICY_UNKNOWN };
        for ( String policy : policies )
        {
            assertThat( provider.getEffectiveChecksumPolicy( session, policy, policy )).isEqualTo( policy );
        }
    }

    @Test
    public void testGetEffectiveChecksumPolicy_DifferentPolicies()
    {
        String[][] testCases =
            { { RepositoryPolicy.CHECKSUM_POLICY_WARN, RepositoryPolicy.CHECKSUM_POLICY_FAIL },
                { RepositoryPolicy.CHECKSUM_POLICY_IGNORE, RepositoryPolicy.CHECKSUM_POLICY_FAIL },
                { RepositoryPolicy.CHECKSUM_POLICY_IGNORE, RepositoryPolicy.CHECKSUM_POLICY_WARN } };
        for ( String[] testCase : testCases )
        {
            assertThat( provider.getEffectiveChecksumPolicy( session, testCase[0], testCase[1] ) );
            assertThat( provider.getEffectiveChecksumPolicy( session, testCase[1], testCase[0] ) );
        }
    }

    @Test
    public void testGetEffectiveChecksumPolicy_UnknownPolicies()
    {
        String[][] testCases =
            { { RepositoryPolicy.CHECKSUM_POLICY_WARN, RepositoryPolicy.CHECKSUM_POLICY_FAIL },
                { RepositoryPolicy.CHECKSUM_POLICY_WARN, RepositoryPolicy.CHECKSUM_POLICY_WARN },
                { RepositoryPolicy.CHECKSUM_POLICY_IGNORE, RepositoryPolicy.CHECKSUM_POLICY_IGNORE } };
        for ( String[] testCase : testCases )
        {
            assertThat( provider.getEffectiveChecksumPolicy( session, CHECKSUM_POLICY_UNKNOWN, testCase[1] ) );
            assertThat( provider.getEffectiveChecksumPolicy( session, testCase[1], CHECKSUM_POLICY_UNKNOWN ) );
        }
    }

}
