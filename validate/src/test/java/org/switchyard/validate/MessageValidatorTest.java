/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.switchyard.validate;

import org.junit.Assert;
import org.junit.Test;
import org.switchyard.Message;
import org.switchyard.internal.DefaultMessage;

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * @author <a href="mailto:tm.igarashi@gmail.com">Tomohisa Igarashi</a>
 */
public class MessageValidatorTest {

    @Test
    public void test() throws IOException {
        final QName A = new QName("a");

        DefaultMessage message = new DefaultMessage().setContent(A);
        MessageValidator<Message> validator = new MessageValidator<Message>();

        ValidationResult result = validator.validate(message);
        Assert.assertTrue(result.isValid());
        Assert.assertNull(result.getDetail());
        Assert.assertEquals(message, validator.getMessage());
    }
}
