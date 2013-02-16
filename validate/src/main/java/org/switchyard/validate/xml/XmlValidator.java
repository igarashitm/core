/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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

package org.switchyard.validate.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.apache.xerces.util.XMLCatalogResolver;
import org.switchyard.Message;
import org.switchyard.common.type.Classes;
import org.switchyard.config.model.Scannable;
import org.switchyard.exception.SwitchYardException;
import org.switchyard.validate.BaseValidator;
import org.switchyard.validate.ValidationResult;
import org.switchyard.validate.ValidatorUtil;
import org.switchyard.validate.config.model.FileEntryModel;
import org.switchyard.validate.config.model.XmlSchemaType;
import org.switchyard.validate.config.model.XmlValidateModel;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML Validator {@link org.switchyard.validate.Validator}.
 * 
 * @author <a href="mailto:tm.igarashi@gmail.com">Tomohisa Igarashi</a>
 */
@Scannable(false)
public class XmlValidator extends BaseValidator<Message> {

    private static final Logger LOGGER = Logger.getLogger(XmlValidator.class);
    private XmlSchemaType _schemaType;
    private String _schemaTypeUri;
    private boolean _failOnWarning;
    private boolean _isNamespaceAware;
    private List<FileEntryModel> _schemaConfig;
    private List<FileEntryModel> _catalogConfig;
    private XMLReader _validatingParser;
    private List<String> _schemaFileNames = new ArrayList<String>();
    private List<String> _catalogFileNames = new ArrayList<String>();
    
    /**
     * constructor.
     * @param name name
     * @param model model
     */
    public XmlValidator(QName name, XmlValidateModel model) {
        super(name);

        _schemaType = model.getSchemaType();
        if (_schemaType == null) {
            throw new SwitchYardException("Could not instantiate XmlValidator: schemaType must be specified.");
        }
        
        switch(_schemaType) {
        case DTD:
            _schemaTypeUri = XMLConstants.XML_DTD_NS_URI;
            break;
        case XML_SCHEMA:
            _schemaTypeUri = XMLConstants.W3C_XML_SCHEMA_NS_URI;
            break;
        case RELAX_NG:
            _schemaTypeUri = XMLConstants.RELAXNG_NS_URI;
            break;
        default:
            throw new SwitchYardException("Could not instantiate XmlValidator: schemaType '" + _schemaType + "' is invalid."
                    + "It must be the one of " + XmlSchemaType.values() + ".");
        }
        
        _failOnWarning = model.failOnWarning();
        _isNamespaceAware = model.namespaceAware();
        if (model.getSchemaFiles() != null) {
            _schemaConfig = model.getSchemaFiles().getEntries();
        }
        if (model.getSchemaCatalogs() != null) {
            _catalogConfig = model.getSchemaCatalogs().getEntries();
        }
        
        setupValidatingParser();
    }

    protected void setupValidatingParser() {
        LOGGER.debug(new StringBuffer("Setting up XmlValidator:[").append(formatUnparsedConfigs()).append("]"));

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setXIncludeAware(true);
        parserFactory.setNamespaceAware(_isNamespaceAware);

        XMLCatalogResolver catalogResolver = null;
        if (_catalogConfig != null) {
            List<String> foundCatalogs = new ArrayList<String>();
            for (FileEntryModel entry : _catalogConfig) {
                URL located = locateFile(entry.getFile());
                if (located != null) {
                    foundCatalogs.add(located.toString());
                } else {
                    LOGGER.warn("schema catalog " + entry.getFile() + " could not be located. ingoring");
                }
            }
            if (foundCatalogs.size() > 0) {
                _catalogFileNames = foundCatalogs;
                catalogResolver = new XMLCatalogResolver();
                catalogResolver.setPreferPublic(true);
                catalogResolver.setCatalogList(foundCatalogs.toArray(new String[0]));
            }
        }
        
        if (XMLConstants.XML_DTD_NS_URI.equals(_schemaTypeUri)) {
            // set up for DTD validation - DTD file is located by DOCTYPE element in the Document itself
            parserFactory.setValidating(true);
            
            try {
                _validatingParser = parserFactory.newSAXParser().getXMLReader();
                if (catalogResolver != null) {
                    _validatingParser.setEntityResolver(catalogResolver);
                }
            } catch (SAXException se) {
                throw new SwitchYardException(se);
            } catch (ParserConfigurationException pce) {
                throw new SwitchYardException(pce);
            }
            
        } else {
            // setup for XML Schema or Relax NG validation
            if (_schemaConfig == null) {
                throw new SwitchYardException("schema file must be specified for " + _schemaType + " validation.");
            }
            
            SchemaFactory schemaFactory = SchemaFactory.newInstance(_schemaTypeUri);
            if (catalogResolver != null) {
                schemaFactory.setResourceResolver(catalogResolver);
            }
            
            List<Source> foundSchemas = new ArrayList<Source>();
            for (FileEntryModel entry : _schemaConfig) {
                URL located = locateFile(entry.getFile());
                if (located != null) {
                    _schemaFileNames.add(located.toString());
                    foundSchemas.add(new StreamSource(located.toExternalForm()));
                } else {
                    LOGGER.warn("schema file " + entry.getFile() + " could not be located. ignoring");
                }
            }

            if (foundSchemas.size() == 0) {
                throw new SwitchYardException("no valid schema file was found");
            }
            
            try {
                Schema schema = schemaFactory.newSchema(foundSchemas.toArray(new Source[0]));
                parserFactory.setSchema(schema);
                _validatingParser = parserFactory.newSAXParser().getXMLReader();
            } catch (SAXException e) {
                throw new SwitchYardException(e);
            } catch (ParserConfigurationException pce) {
                throw new SwitchYardException(pce);
            }
        }
    }
    
    @Override
    public ValidationResult validate(Message msg) {
        LOGGER.debug(new StringBuffer("Entering XML validation:[").append(formatParsedConfigs()).append("]"));
        
        try {
            XmlValidationErrorHandler errorHandler = new XmlValidationErrorHandler(_failOnWarning);
            _validatingParser.setErrorHandler(errorHandler);
            _validatingParser.parse(msg.getContent(InputSource.class));
            if (errorHandler.validationFailed()) {
                return ValidatorUtil.invalidResult(formatErrorMessage(errorHandler.getErrors()).toString());
            }
        } catch (SAXException e) {
            throw new SwitchYardException(e);
        } catch (IOException ioe) {
            throw new SwitchYardException(ioe);
        }
        return ValidatorUtil.validResult();
    }

    protected URL locateFile(String path) {
        if (path == null) {
            return null;
        }
        
        if (new File(path).exists()) {
            try {
                return new File(path).toURI().toURL();
            } catch (Exception e) {
                return null;
            }
        } else {
            try {
                URL res = Classes.getResource(path);
                if (res != null) {
                    return res;
                }
            } catch (IOException e) {
                e.getMessage();
            }
        }
        return null;
    }
    
    protected StringBuffer formatErrorMessage(List<Exception> errors) {
        String nl = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer().append(errors.size()).append(" validation error(s): ").append(nl);
        for (Exception e : errors) {
            buf.append(formatRootCause(e)).append(nl);
        }
        return buf;
    }

    protected StringBuffer formatRootCause(Throwable t) {
        Throwable cause = t;
        StringBuffer buf = new StringBuffer(cause.getClass().getName()).append(": ").append(cause.getMessage());
        while ((cause = cause.getCause()) != null) {
            buf.append(" --- Caused by ").append(cause.getClass().getName()).append(": ").append(cause.getMessage());
        }
        return buf;
    }
    
    protected StringBuffer formatUnparsedConfigs() {
        StringBuffer buf = new StringBuffer();
        buf.append("schema type=").append(_schemaType); 
        if (_schemaConfig != null && _schemaConfig.size() > 0) {
            buf.append(", schema files=").append(_schemaConfig.toString());
        }
        if (_catalogConfig != null && _catalogConfig.size() > 0) {
            buf.append(", catalogs=").append(_catalogConfig.toString());
        }
        return buf;
    }
    
    protected StringBuffer formatParsedConfigs() {
        StringBuffer buf = new StringBuffer();
        buf.append("schema type=").append(_schemaType); 
        if (_schemaFileNames.size() > 0) {
            buf.append(", schema files=").append(_schemaFileNames.toString());
        }
        if (_catalogFileNames.size() > 0) {
            buf.append(", catalogs=").append(_catalogFileNames.toString());
        }
        return buf;
    }
    
    protected class XmlValidationErrorHandler extends DefaultHandler {
        private boolean _validationFailed;
        private boolean _failOnWarning;
        private List<Exception> _errors = new ArrayList<Exception>();
        
        public XmlValidationErrorHandler(boolean failOnWarning) {
            _failOnWarning = failOnWarning;
            _validationFailed = false;
        }
        
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            _validationFailed = true;
            _errors.add(e);
        }
        
        @Override
        public void error(SAXParseException e) throws SAXException {
            _validationFailed = true;
            _errors.add(e);
        }
        
        @Override
        public void warning(SAXParseException e) throws SAXException {
            if (_failOnWarning) {
                _validationFailed = true;
                _errors.add(e);
            } else {
                StringBuffer buf = new StringBuffer("Warning during validation: ");
                LOGGER.warn(buf.append(formatParsedConfigs()).append(": ").append(e.getMessage()).toString());
            }
        }
        
        public boolean validationFailed() {
            return _validationFailed;
        }
        
        public List<Exception> getErrors() {
            return Collections.unmodifiableList(_errors);
        }
    }
    
}
