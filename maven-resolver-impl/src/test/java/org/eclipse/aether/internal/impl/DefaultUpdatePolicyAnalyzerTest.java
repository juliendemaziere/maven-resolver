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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.aether.repository.RepositoryPolicy.*;

import java.util.Calendar;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class DefaultUpdatePolicyAnalyzerTest
{

    private DefaultUpdatePolicyAnalyzer analyzer;

    private DefaultRepositorySystemSession session;

    @Before
    public void setup()
    {
        analyzer = new DefaultUpdatePolicyAnalyzer();
        session = TestUtils.newSession();
    }

    private long now()
    {
        return System.currentTimeMillis();
    }

    @Test
    public void testIsUpdateRequired_PolicyNever()
    {
        String policy = RepositoryPolicy.UPDATE_POLICY_NEVER;
        assertThat( analyzer.isUpdatedRequired( session, Long.MIN_VALUE, policy ) ).isFalse();
        assertThat( analyzer.isUpdatedRequired( session, Long.MAX_VALUE, policy ) ).isFalse();
        assertThat( analyzer.isUpdatedRequired( session, 0, policy ) ).isFalse();
        assertThat( analyzer.isUpdatedRequired( session, 1, policy )).isFalse();
        assertThat( analyzer.isUpdatedRequired( session, now() - 604800000, policy )).isFalse();
    }

    @Test
    public void testIsUpdateRequired_PolicyAlways()
    {
        String policy = RepositoryPolicy.UPDATE_POLICY_ALWAYS;
        assertThat( analyzer.isUpdatedRequired( session, Long.MIN_VALUE, policy )).isTrue();
        assertThat( analyzer.isUpdatedRequired( session, Long.MAX_VALUE, policy )).isTrue();
        assertThat( analyzer.isUpdatedRequired( session, 0, policy )).isTrue();
        assertThat( analyzer.isUpdatedRequired( session, 1, policy )).isTrue();
        assertThat( analyzer.isUpdatedRequired( session, now() - 1000, policy )).isTrue();
    }

    @Test
    public void testIsUpdateRequired_PolicyDaily()
    {
        Calendar cal = Calendar.getInstance();
        cal.set( Calendar.HOUR_OF_DAY, 0 );
        cal.set( Calendar.MINUTE, 0 );
        cal.set( Calendar.SECOND, 0 );
        cal.set( Calendar.MILLISECOND, 0 );
        long localMidnight = cal.getTimeInMillis();

        String policy = RepositoryPolicy.UPDATE_POLICY_DAILY;
        assertThat(analyzer.isUpdatedRequired( session, Long.MIN_VALUE, policy )).isTrue();
        assertThat( analyzer.isUpdatedRequired( session, Long.MAX_VALUE, policy )).isFalse();
        assertThat(analyzer.isUpdatedRequired( session, localMidnight + 0, policy )).isFalse();
        assertThat(analyzer.isUpdatedRequired( session, localMidnight + 1, policy )).isFalse();
        assertThat(analyzer.isUpdatedRequired( session, localMidnight - 1, policy )).isFalse();
    }

    @Test
    public void testIsUpdateRequired_PolicyInterval()
    {
        String policy = RepositoryPolicy.UPDATE_POLICY_INTERVAL + ":5";
        assertThat( analyzer.isUpdatedRequired( session, Long.MIN_VALUE, policy ) ).isTrue();
        assertThat( analyzer.isUpdatedRequired( session, Long.MAX_VALUE, policy )).isFalse();
        assertThat( analyzer.isUpdatedRequired( session, now(), policy )).isFalse();
        assertThat(analyzer.isUpdatedRequired( session, now() - 5 - 1, policy )).isFalse();
        assertThat(analyzer.isUpdatedRequired( session, now() - 1000 * 5 - 1, policy )).isFalse();
        assertThat(analyzer.isUpdatedRequired( session, now() - 1000 * 60 * 5 - 1, policy )).isTrue();

        policy = RepositoryPolicy.UPDATE_POLICY_INTERVAL + ":invalid";
        assertThat( analyzer.isUpdatedRequired( session, now(), policy )).isFalse();
    }

    @Test
    public void testEffectivePolicy()
    {
        assertThat( analyzer.getEffectiveUpdatePolicy( session, UPDATE_POLICY_ALWAYS, UPDATE_POLICY_DAILY ) )
                .isEqualTo(UPDATE_POLICY_ALWAYS);
        assertThat( analyzer.getEffectiveUpdatePolicy( session, UPDATE_POLICY_ALWAYS, UPDATE_POLICY_NEVER ) )
                .isEqualTo(UPDATE_POLICY_ALWAYS);
        assertThat( analyzer.getEffectiveUpdatePolicy( session, UPDATE_POLICY_DAILY, UPDATE_POLICY_NEVER ) )
                .isEqualTo( UPDATE_POLICY_DAILY );
        assertThat( analyzer.getEffectiveUpdatePolicy( session, UPDATE_POLICY_DAILY, UPDATE_POLICY_INTERVAL + ":60" ) )
                .isEqualTo( UPDATE_POLICY_INTERVAL + ":60" );
        assertThat( analyzer.getEffectiveUpdatePolicy(
                session, UPDATE_POLICY_INTERVAL + ":100",UPDATE_POLICY_INTERVAL + ":60" ) )
                .isEqualTo( UPDATE_POLICY_INTERVAL + ":60" );
    }

}
