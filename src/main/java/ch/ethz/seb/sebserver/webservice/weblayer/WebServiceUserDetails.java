/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer;

import java.util.Collections;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class WebServiceUserDetails implements UserDetailsService {

//    private final UserDao userDao;
//
//    public InternalUserDetailsService(final UserDao userDao) {
//        this.userDao = userDao;
//    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return new User(
                username,
                "$2a$04$btj5PkII8IIHLE7zbQOd3u7YghHeClG7k1ZzYbtybRnd5h1YqwTf.",
                Collections.emptyList());

//        try {
//            final org.eth.demo.sebserver.domain.rest.admin.User byUserName = this.userDao.byUserName(username);
//            if (byUserName == null) {
//                throw new UsernameNotFoundException("No User with name: " + username + " found");
//            }
//            return byUserName;
//        } catch (final Exception e) {
//            throw new UsernameNotFoundException("No User with name: " + username + " found");
//        }
    }

}
