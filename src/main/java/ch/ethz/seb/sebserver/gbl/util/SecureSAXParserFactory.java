/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/** Creates SAX parsers that are hardened against XML External Entity (XXE) attacks.
 * <p>
 * SEB settings are Apple property lists that usually carry a DOCTYPE declaration referring to Apple's plist DTD.
 * DOCTYPE declarations are therefore still accepted, but external DTDs are never loaded and external general and
 * parameter entities are never resolved. Expansion of internal entities is bounded by the JAXP secure processing
 * limits. Use this for every XML document that may come from a user. */
public final class SecureSAXParserFactory {

    private SecureSAXParserFactory() {
    }

    /** Creates a new non-validating SAX parser that neither loads external DTDs nor resolves external entities.
     *
     * @return a new hardened SAXParser
     * @throws ParserConfigurationException if the parser cannot be created
     * @throws SAXException if the underlying parser does not support one of the required features */
    public static SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setXIncludeAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        final SAXParser parser = factory.newSAXParser();
        // additional JAXP 1.5 safeguards, not supported by every parser implementation
        setPropertyIfSupported(parser, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setPropertyIfSupported(parser, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return parser;
    }

    private static void setPropertyIfSupported(final SAXParser parser, final String name, final Object value) {
        try {
            parser.setProperty(name, value);
        } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
            // external access is already disabled by the features set on the factory
        }
    }
}
