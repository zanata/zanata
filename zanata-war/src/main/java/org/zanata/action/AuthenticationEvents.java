/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
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
package org.zanata.action;

import java.io.Serializable;

import lombok.extern.slf4j.Slf4j;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.zanata.events.LoginSuccessfulEvent;
import org.zanata.events.UserCreatedEvent;

@Name("authenticationEvents")
@Scope(ScopeType.STATELESS)
@Slf4j
public class AuthenticationEvents implements Serializable {
    private static final long serialVersionUID = 1L;

    @Observer(UserCreatedEvent.EVENT_NAME)
    public void createSuccessful(UserCreatedEvent userCreatedEvent) {
        log.info("Account {} created", userCreatedEvent.getUser().getUsername());
    }

    @Observer(LoginSuccessfulEvent.EVENT_NAME)
    public void loginInSuccessful() {
        log.debug("Account logged in successfully");
    }

}
