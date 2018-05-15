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

import java.util.Objects;

import static java.lang.String.valueOf;

final class PrioritizedComponent<T>
    implements Comparable<PrioritizedComponent<?>>
{

    private final T component;

    private final Class<?> type;

    private final float priority;

    private final int index;

    PrioritizedComponent( T component, Class<?> type, float priority, int index )
    {
        this.component = component;
        this.type = type;
        this.priority = priority;
        this.index = index;
    }

    public T getComponent()
    {
        return component;
    }

    public Class<?> getType()
    {
        return type;
    }

    public float getPriority()
    {
        return priority;
    }

    public boolean isDisabled()
    {
        return Float.isNaN( priority );
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        PrioritizedComponent<?> that = (PrioritizedComponent<?>) o;
        return Float.compare( that.getPriority(), getPriority() ) == 0 &&
                index == that.index &&
                Objects.equals( getComponent(), that.getComponent() ) &&
                Objects.equals( getType(), that.getType() );
    }

    @Override
    public int hashCode() {

        return Objects.hash( getComponent(), getType(), getPriority(), index );
    }

    public int compareTo( PrioritizedComponent<?> o )
    {
        int rel = ( isDisabled() ? 1 : 0 ) - ( o.isDisabled() ? 1 : 0 );
        if ( rel == 0 )
        {
            rel = Float.compare( o.priority, priority );
            if ( rel == 0 )
            {
                rel = index - o.index;
            }
        }
        return rel;
    }

    @Override
    public String toString()
    {
        return priority + " (#" + index + "): " + valueOf( component );
    }

}
