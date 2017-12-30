package org.eclipse.aether.util;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ConfigUtilsTest
{

    private Map<Object, Object> config = new HashMap<>();

    @Test
    public void testGetObject_Default()
    {
        Object val = new Object();
        assertThat( ConfigUtils.getObject( config, val, "no-value")).isSameAs( val );
    }

    @Test
    public void testGetObject_AlternativeKeys()
    {
        Object val = new Object();
        config.put( "some-object", val );
        assertThat( ConfigUtils.getObject( config, null, "no-object", "some-object")).isSameAs( val );
    }

    @Test
    public void testGetMap_Default()
    {
        Map<?, ?> val = new HashMap<Object, Object>();
        assertThat( ConfigUtils.getMap( config, val, "no-value")).isSameAs(val );
    }

    @Test
    public void testGetMap_AlternativeKeys()
    {
        Map<?, ?> val = new HashMap<Object, Object>();
        config.put( "some-map", val );
        assertThat( ConfigUtils.getMap( config, null, "some-map")).isSameAs( val );
    }

    @Test
    public void testGetList_Default()
    {
        List<?> val = new ArrayList<Object>();
        assertThat( ConfigUtils.getList( config, val, "no-value")).isSameAs( val );
    }

    @Test
    public void testGetList_AlternativeKeys()
    {
        List<?> val = new ArrayList<Object>();
        config.put( "some-list", val );
        assertThat( ConfigUtils.getList( config, null, "some-list")).isSameAs( val );
    }

    @Test
    public void testGetList_CollectionConversion()
    {
        Collection<?> val = Collections.singleton( "item" );
        config.put( "some-collection", val );
        assertThat( ConfigUtils.getList( config, null, "some-collection")).isEqualTo( Arrays.asList( "item" ) );
    }

    @Test
    public void testGetString_Default()
    {
        config.put( "no-string", new Object() );
        assertThat( ConfigUtils.getString( config, "default", "no-value")).isEqualTo( "default" );
        assertThat( ConfigUtils.getString( config, "default", "no-string")).isSameAs( "default" );
    }

    @Test
    public void testGetString_AlternativeKeys()
    {
        config.put( "no-string", new Object() );
        config.put( "some-string", "passed" );
        assertThat( ConfigUtils.getString( config, "default", "some-string")).isEqualTo( "passed" );
    }

    @Test
    public void testGetBoolean_Default()
    {
        config.put( "no-boolean", new Object() );
        assertThat( ConfigUtils.getBoolean( config, true, "no-value")).isTrue();
        assertThat( ConfigUtils.getBoolean( config, false, "no-value")).isFalse();
        assertThat( ConfigUtils.getBoolean( config, true, "no-boolean")).isTrue();
        assertThat( ConfigUtils.getBoolean( config, false, "no-boolean")).isFalse();
    }

    @Test
    public void testGetBoolean_AlternativeKeys()
    {
        config.put( "no-boolean", new Object() );
        config.put( "some-boolean", true );
        assertThat( ConfigUtils.getBoolean( config, false, "some-boolean")).isTrue();
        config.put( "some-boolean", false );
        assertThat( ConfigUtils.getBoolean( config, true, "some-boolean")).isFalse();
    }

    @Test
    public void testGetBoolean_StringConversion()
    {
        config.put( "some-boolean", "true" );
        assertThat( ConfigUtils.getBoolean( config, false, "some-boolean")).isTrue();
        config.put( "some-boolean", "false" );
        assertThat( ConfigUtils.getBoolean( config, true, "some-boolean")).isFalse();
    }

    @Test
    public void testGetInteger_Default()
    {
        config.put( "no-integer", new Object() );
        assertThat( ConfigUtils.getInteger( config, -17, "no-value")).isEqualTo( -17 );
        assertThat( ConfigUtils.getInteger( config, 43, "no-integer")).isEqualTo( 43 );
    }

    @Test
    public void testGetInteger_AlternativeKeys()
    {
        config.put( "no-integer", "text" );
        config.put( "some-integer", 23 );
        assertThat( ConfigUtils.getInteger( config, 0, "no-integer", "some-integer" ) ).isEqualTo( 23 );
    }

    @Test
    public void testGetInteger_StringConversion()
    {
        config.put( "some-integer", "-123456" );
        assertThat( ConfigUtils.getInteger( config, 0, "some-integer" ) ).isEqualTo( -123456 );
    }

    @Test
    public void testGetInteger_NumberConversion()
    {
        config.put( "some-number", -123456.789 );
        assertThat( ConfigUtils.getInteger( config, 0,"some-number" ) ).isEqualTo( -123456 );
    }

    @Test
    public void testGetLong_Default()
    {
        config.put( "no-long", new Object() );
        assertThat( ConfigUtils.getLong( config, -17L, "no-value" ) ).isEqualTo( -17L );
        assertThat( ConfigUtils.getLong( config, 43L, "no-long" ) ).isEqualTo( 43 );
    }

    @Test
    public void testGetLong_AlternativeKeys()
    {
        config.put( "no-long", "text" );
        config.put( "some-long", 23L );
        assertThat( ConfigUtils.getLong( config, 0, "no-long", "some-long" ) ).isEqualTo( 23L );
    }

    @Test
    public void testGetLong_StringConversion()
    {
        config.put( "some-long", "-123456789012" );
        assertThat( ConfigUtils.getLong( config, 0, "some-long")).isEqualTo(-123456789012L );
    }

    @Test
    public void testGetLong_NumberConversion()
    {
        config.put( "some-number", -123456789012.789 );
        assertThat( ConfigUtils.getLong( config, 0, "some-number")).isEqualTo(-123456789012L );
    }

    @Test
    public void testGetFloat_Default()
    {
        config.put( "no-float", new Object() );
        assertThat(ConfigUtils.getFloat( config, -17.1f, "no-value" )).isEqualTo(-17.1f );
        assertThat( ConfigUtils.getFloat( config, 43.2f, "no-float" )).isEqualTo(43.2f);
    }

    @Test
    public void testGetFloat_AlternativeKeys()
    {
        config.put( "no-float", "text" );
        config.put( "some-float", 12.3f );
        assertThat( ConfigUtils.getFloat( config, 0, "no-float", "some-float" )).isEqualTo(12.3f );
    }

    @Test
    public void testGetFloat_StringConversion()
    {
        config.put( "some-float", "-12.3" );
        assertThat( ConfigUtils.getFloat( config, 0, "some-float" )).isEqualTo( -12.3f );
        config.put( "some-float", "NaN" );
        assertThat( Float.isNaN( ConfigUtils.getFloat( config, 0, "some-float"))).isTrue();
    }

    @Test
    public void testGetFloat_NumberConversion()
    {
        config.put( "some-number", -1234f );
        assertThat( ConfigUtils.getFloat( config, 0, "some-number" )).isEqualTo( -1234f );
    }

}
