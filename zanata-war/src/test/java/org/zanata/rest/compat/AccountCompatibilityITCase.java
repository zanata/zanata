/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.rest.compat;

import javax.ws.rs.core.Response.Status;

import org.dbunit.operation.DatabaseOperation;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;
import org.zanata.RestTest;
import org.zanata.apicompat.rest.client.IAccountResource;
import org.zanata.apicompat.rest.dto.Account;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.zanata.provider.DBUnitProvider.DataSetOperation;

public class AccountCompatibilityITCase extends RestTest
{

   @Override
   protected void prepareDBUnitOperations()
   {
      addBeforeTestOperation(new DataSetOperation("org/zanata/test/model/AccountData.dbunit.xml", DatabaseOperation.CLEAN_INSERT));
   }   

   @Test
   @RunAsClient
   public void getAccountXml() throws Exception
   {
      IAccountResource accountClient = super.createProxy(createClientProxyFactory(TRANSLATOR, TRANSLATOR_KEY),
            IAccountResource.class, "/accounts/u/demo");
      ClientResponse<Account> accountResponse = accountClient.get();
      Account account = accountResponse.getEntity();
      
      // Assert correct parsing of all properties
      assertThat(account.getUsername(), is("demo"));
      assertThat(account.getApiKey(), is("23456789012345678901234567890123"));
      assertThat(account.getEmail(), is("user1@localhost"));
      assertThat(account.getName(), is("Sample User"));
      assertThat(account.getPasswordHash(), is("/9Se/pfHeUH8FJ4asBD6jQ=="));
      assertThat(account.getRoles().size(), is(1));
      //assertThat(account.getTribes().size(), is(1)); // Language teams are not being returned
   }
   
   @Test
   @RunAsClient
   public void putAccountXml() throws Exception
   {
      // New Account
      Account a = new Account("aacount2@localhost.com", "Sample Account", "sampleaccount", "/9Se/pfHeUH8FJ4asBD6jQ==");
      
      IAccountResource accountClient = super.createProxy( createClientProxyFactory(ADMIN, ADMIN_KEY),
            IAccountResource.class, "/accounts/u/sampleaccount");
      ClientResponse putResponse = accountClient.put( a );
      
      // Assert initial put
      assertThat(putResponse.getStatus(), is(Status.CREATED.getStatusCode()));
      putResponse.releaseConnection();
      
      // Modified Account
      a.setName("New Account Name");
      putResponse = accountClient.put( a );
      putResponse.releaseConnection();
      
      // Assert modification
      assertThat(putResponse.getStatus(), is(Status.OK.getStatusCode()));
      
      // Retrieve again
      Account a2 = accountClient.get().getEntity();
      assertThat(a2.getUsername(), is(a.getUsername()));
      assertThat(a2.getApiKey(), is(a.getApiKey()));
      assertThat(a2.getEmail(), is(a.getEmail()));
      assertThat(a2.getName(), is(a.getName()));
      assertThat(a2.getPasswordHash(), is(a.getPasswordHash()));
      assertThat(a2.getRoles().size(), is(0));
      // assertThat(a2.getTribes().size(), is(1)); // Language teams are not being returned
   }
   
}
