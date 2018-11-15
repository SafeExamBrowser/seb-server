/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@RestController
@RequestMapping("/webservice")
@WebServiceProfile
public class WsTestController {

    public WsTestController() {
        System.out.println("************** TestController webservice");
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String helloFromWebService() {
        return "Hello From Web-Service";
    }

}
