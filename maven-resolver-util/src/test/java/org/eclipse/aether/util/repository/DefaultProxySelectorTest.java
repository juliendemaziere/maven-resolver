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

import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.junit.Test;

/**
 */
public class DefaultProxySelectorTest
{

    private boolean isNonProxyHost( String host, String nonProxyHosts )
    {
        return new DefaultProxySelector.NonProxyHosts( nonProxyHosts ).isNonProxyHost( host );
    }

    @Test
    public void testIsNonProxyHost_Blank()
    {
        assertThat(isNonProxyHost( "www.eclipse.org", null ) ).isFalse();
        assertThat(isNonProxyHost( "www.eclipse.org", "" ) ).isFalse();
    }

    @Test
    public void testIsNonProxyHost_Wildcard()
    {
        assertThat(isNonProxyHost( "www.eclipse.org", "*" ) ).isTrue();
        assertThat(isNonProxyHost( "www.eclipse.org", "*.org" ) ).isTrue();
        assertThat(isNonProxyHost( "www.eclipse.org", "*.com" ) ).isFalse();
        assertThat(isNonProxyHost( "www.eclipse.org", "www.*" ) ).isTrue();
        assertThat(isNonProxyHost( "www.eclipse.org", "www.*.org" ) ).isTrue();
    }

    @Test
    public void testIsNonProxyHost_Multiple()
    {
        assertThat(isNonProxyHost( "eclipse.org", "eclipse.org|host2" ) ).isTrue();
        assertThat(isNonProxyHost( "eclipse.org", "host1|eclipse.org" ) ).isTrue();
        assertThat(isNonProxyHost( "eclipse.org", "host1|eclipse.org|host2" ) ).isTrue();
    }

    @Test
    public void testIsNonProxyHost_Misc()
    {
        assertThat(isNonProxyHost( "www.eclipse.org", "www.eclipse.com" ) ).isFalse();
        assertThat(isNonProxyHost( "www.eclipse.org", "eclipse.org" ) ).isFalse();
    }

    @Test
    public void testIsNonProxyHost_CaseInsensitivity()
    {
        assertThat(isNonProxyHost( "www.eclipse.org", "www.ECLIPSE.org" ) ).isTrue();
        assertThat(isNonProxyHost( "www.ECLIPSE.org", "www.eclipse.org" ) ).isTrue();
    }

}
