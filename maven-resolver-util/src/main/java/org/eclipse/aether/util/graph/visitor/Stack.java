package org.eclipse.aether.util.graph.visitor;

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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * A non-synchronized stack with a non-modifiable list view which starts at the top of the stack. While
 * {@code LinkedList} can provide the same behavior, it creates many temp objects upon frequent pushes/pops.
 */
class Stack<E>
    extends AbstractList<E>
    implements RandomAccess
{

    @SuppressWarnings( "unchecked" )
    private E[] elements = (E[]) new Object[96];

    private int size;

    public void push( E element )
    {
        if ( size >= elements.length )
        {
            @SuppressWarnings( "unchecked" )
            E[] tmp = (E[]) new Object[size + 64];
            System.arraycopy( elements, 0, tmp, 0, elements.length );
            elements = tmp;
        }
        elements[size++] = element;
    }

    public E pop()
    {
        if ( size <= 0 )
        {
            throw new NoSuchElementException();
        }
        return elements[--size];
    }

    public E peek()
    {
        if ( size <= 0 )
        {
            return null;
        }
        return elements[size - 1];
    }

    @Override
    public E get( int index )
    {
        if ( index < 0 || index >= size )
        {
            throw new IndexOutOfBoundsException( "Index: " + index + ", Size: " + size );
        }
        return elements[size - index - 1];
    }

    @Override
    public int size()
    {
        return size;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        if ( !super.equals( o ) ) {
            return false;
        }
        Stack<?> stack = (Stack<?>) o;
        return size == stack.size &&
                Arrays.equals( elements, stack.elements );
    }

    @Override
    public int hashCode() {

        int result = Objects.hash( super.hashCode(), size );
        result = 31 * result + Arrays.hashCode( elements );
        return result;
    }
}
