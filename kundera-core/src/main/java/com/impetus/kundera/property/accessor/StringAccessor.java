/*******************************************************************************
 * * Copyright 2012 Impetus Infotech.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 ******************************************************************************/
package com.impetus.kundera.property.accessor;

import java.io.UnsupportedEncodingException;

import com.impetus.kundera.Constants;
import com.impetus.kundera.property.PropertyAccessException;
import com.impetus.kundera.property.PropertyAccessor;

/**
 * The Class StringAccessor.
 * 
 * @author animesh.kumar
 */
public class StringAccessor implements PropertyAccessor<String>
{

    /* @see com.impetus.kundera.property.PropertyAccessor#fromBytes(byte[]) */
    /*
     * (non-Javadoc)
     * 
     * @see com.impetus.kundera.property.PropertyAccessor#fromBytes(byte[])
     */
    @Override
    public final String fromBytes(Class targetClass, byte[] bytes)
    {

        try
        {
            return bytes != null ? new String(bytes, Constants.ENCODING) : null;
        }
        catch (UnsupportedEncodingException e)
        {
            throw new PropertyAccessException(e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.kundera.property.PropertyAccessor#toBytes(java.lang.Object)
     */
    @Override
    public final byte[] toBytes(Object s) throws PropertyAccessException
    {

        try
        {
            return s != null ? ((String) s).getBytes(Constants.ENCODING) : null;
        }
        catch (UnsupportedEncodingException e)
        {
            throw new PropertyAccessException(e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.kundera.property.PropertyAccessor#toString(java.lang.Object)
     */
    @Override
    public final String toString(Object object)
    {
        if (object != null && object.getClass().isAssignableFrom(String.class))
        {
            return (String) object;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.impetus.kundera.property.PropertyAccessor#fromString(java.lang.String
     * )
     */
    @Override
    public String fromString(Class targetClass, String s)
    {
        return s;
    }

    @Override
    public String getCopy(Object object)
    {
        return object != null ? new String((String) object) : null;
    }

    public String getInstance(Class<?> clazz)
    {
        return new String();
    }
}
