/*
 * JBoss, Home of Professional Open Source Copyright 2009, Red Hat Middleware
 * LLC, and individual contributors by the @authors tag. See the copyright.txt
 * in the distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.switchyard.test;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.naming.InMemoryNamingStore;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.junit.Assert;

/**
 * Setup the naming service.
 * 
 * @author <a href="mailto:tm.igarashi@gmail.com">Tomohisa Igarashi</a>
 *
 */
public final class JBossASNamingServiceInstaller {
    private static final String INITIAL_CONTEXT_FACTORY_NAME = "org.jboss.as.naming.InitialContextFactory";
    private static final CompositeName EMPTY_NAME = new CompositeName();
    
    private JBossASNamingServiceInstaller() {
    }
    
    /**
     * install.
     */
    public static void install() {
        String factoryName = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        if (factoryName != null && !factoryName.equals(INITIAL_CONTEXT_FACTORY_NAME)) {
            return;
        }
        
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY_NAME);
        NamingContext.initializeNamingManager();
        NamespaceContextSelector.setDefault(new NamespaceContextSelector() {
            public Context getContext(String identifier) {
                try {
                    return (Context)new InitialContext().lookup(EMPTY_NAME);
                } catch (NamingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        try {
            InitialContext initialContext = new InitialContext();
            try {
                Context.class.cast(initialContext.lookup("java:comp"));
            } catch (Exception e) {
                initialContext.createSubcontext("java:comp");
            }
        } catch (NamingException e) {
            e.printStackTrace();
            Assert.fail("Failed to create context : " + e.getMessage());
        }
    }

    /**
     * clear NamingStore.
     */
    public static void clear() {
        NamingContext.setActiveNamingStore(new InMemoryNamingStore());
    }
}
