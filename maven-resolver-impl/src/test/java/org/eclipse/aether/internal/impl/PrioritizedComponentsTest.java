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
import static org.assertj.core.api.Assertions.within;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import org.eclipse.aether.ConfigurationProperties;
import org.junit.Test;

public class PrioritizedComponentsTest
{

    @Test
    public void testGetConfigKeys()
    {
        String[] keys =
            { ConfigurationProperties.PREFIX_PRIORITY + "java.lang.String",
                ConfigurationProperties.PREFIX_PRIORITY + "String" };
        assertThat( PrioritizedComponents.getConfigKeys( String.class ) ).isEqualTo( keys );

        keys =
            new String[] { ConfigurationProperties.PREFIX_PRIORITY + "java.util.concurrent.ThreadFactory",
                ConfigurationProperties.PREFIX_PRIORITY + "ThreadFactory",
                ConfigurationProperties.PREFIX_PRIORITY + "Thread" };
        assertThat( PrioritizedComponents.getConfigKeys( ThreadFactory.class ) ).isEqualTo( keys );
    }

    @Test
    public void testAdd_PriorityOverride()
    {
        Exception comp1 = new IllegalArgumentException();
        Exception comp2 = new NullPointerException();
        Map<Object, Object> config = new HashMap<Object, Object>();
        config.put( ConfigurationProperties.PREFIX_PRIORITY + comp1.getClass().getName(), 6 );
        config.put( ConfigurationProperties.PREFIX_PRIORITY + comp2.getClass().getName(), 7 );
        PrioritizedComponents<Exception> components = new PrioritizedComponents<Exception>( config );
        components.add( comp1, 1 );
        components.add( comp2, 0 );
        List<PrioritizedComponent<Exception>> sorted = components.getEnabled();
        assertThat(sorted.size() ).isEqualTo(2);
        assertThat(sorted.get( 0 ).getComponent() ).isSameAs(comp2);
        assertThat(sorted.get( 0 ).getPriority()).isEqualTo( 7, within(0.1f));
        assertThat(sorted.get( 1 ).getComponent() ).isSameAs(comp1);
        assertThat(sorted.get( 1 ).getPriority()).isCloseTo(6, within(0.1f ));
    }

    @Test
    public void testAdd_ImplicitPriority()
    {
        Exception comp1 = new IllegalArgumentException();
        Exception comp2 = new NullPointerException();
        Map<Object, Object> config = new HashMap<Object, Object>();
        config.put( ConfigurationProperties.IMPLICIT_PRIORITIES, true );
        PrioritizedComponents<Exception> components = new PrioritizedComponents<Exception>( config );
        components.add( comp1, 1 );
        components.add( comp2, 2 );
        List<PrioritizedComponent<Exception>> sorted = components.getEnabled();
        assertThat(sorted.size() ).isEqualTo(2);
        assertThat(sorted.get( 0 ).getComponent() ).isSameAs(comp1);
        assertThat(sorted.get( 1 ).getComponent() ).isSameAs(comp2);
    }

    @Test
    public void testAdd_Disabled()
    {
        Exception comp1 = new IllegalArgumentException();
        Exception comp2 = new NullPointerException();
        Map<Object, Object> config = new HashMap<Object, Object>();
        PrioritizedComponents<Exception> components = new PrioritizedComponents<Exception>( config );

        components.add( new UnsupportedOperationException(), Float.NaN );
        List<PrioritizedComponent<Exception>> sorted = components.getEnabled();
        assertThat(sorted.size() ).isEqualTo(0);

        components.add( comp1, 1 );
        sorted = components.getEnabled();
        assertThat(sorted.size() ).isEqualTo(1);
        assertThat(sorted.get( 0 ).getComponent() ).isSameAs(comp1);

        components.add( new Exception(), Float.NaN );
        sorted = components.getEnabled();
        assertThat(sorted.size() ).isEqualTo(1);
        assertThat(sorted.get( 0 ).getComponent() ).isSameAs(comp1);

        components.add( comp2, 0 );
        sorted = components.getEnabled();
        assertThat( sorted ).hasSize( 2 );
        assertThat( sorted.get( 0 ).getComponent() ).isSameAs( comp1 );
        assertThat( sorted.get( 1 ).getComponent() ).isSameAs( comp2 );
    }
}
