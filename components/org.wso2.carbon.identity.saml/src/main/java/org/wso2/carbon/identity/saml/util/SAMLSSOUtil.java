/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.saml.util;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.util.SecurityManager;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.ResponseBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.core.impl.StatusMessageBuilder;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.SignableXMLObject;
import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.wso2.carbon.identity.common.base.exception.IdentityException;
import org.wso2.carbon.identity.gateway.context.AuthenticationContext;
import org.wso2.carbon.identity.mgt.claim.Claim;
import org.wso2.carbon.identity.saml.builders.X509CredentialImpl;
import org.wso2.carbon.identity.saml.builders.signature.DefaultSSOSigner;
import org.wso2.carbon.identity.saml.builders.signature.SSOSigner;
import org.wso2.carbon.identity.saml.context.SAMLMessageContext;
import org.wso2.carbon.identity.saml.exception.SAMLRuntimeException;
import org.wso2.carbon.identity.saml.exception.SAMLServerException;
import org.wso2.carbon.identity.saml.internal.SAMLInboundServiceHolder;
import org.wso2.carbon.identity.saml.model.SAMLConfigurations;
import org.wso2.carbon.identity.saml.model.SAMLResponseHandlerConfig;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class SAMLSSOUtil {

    private static final String SECURITY_MANAGER_PROPERTY = Constants.XERCES_PROPERTY_PREFIX +
                                                            Constants.SECURITY_MANAGER_PROPERTY;
    private static final int ENTITY_EXPANSION_LIMIT = 0;
    private static int singleLogoutRetryCount = 5;
    private static long singleLogoutRetryInterval = 60000;
    private static Logger log = LoggerFactory.getLogger(SAMLSSOUtil.class);

    public static String getNotificationEndpoint() {
       return SAMLConfigurations.getInstance().getNotificationEndpoint();
    }

    public static boolean validateACS(String issuerName, String requestedACSUrl) {
        // TODO
        return true;
        //        SSOServiceProviderConfigManager stratosIdpConfigManager = SSOServiceProviderConfigManager
        // .getInstance();
        //        SAMLSSOServiceProviderDO serviceProvider = stratosIdpConfigManager.getServiceProvider(issuerName);
        //        if (serviceProvider != null) {
        //            return true;
        //        }
        //
        //        int tenantId;
        //        if (StringUtils.isBlank(tenantDomain)) {
        //            tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        //            tenantId = MultitenantConstants.SUPER_TENANT_ID;
        //        } else {
        //            try {
        //                tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        //            } catch (UserStoreException e) {
        //                throw new SAMLServerException("Error occurred while retrieving tenant id for the domain : " +
        //                        tenantDomain, e);
        //            }
        //        }
        //
        //        try {
        //            PrivilegedCarbonContext.startTenantFlow();
        //            PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext
        // .getThreadLocalCarbonContext();
        //            privilegedCarbonContext.setTenantId(tenantId);
        //            privilegedCarbonContext.setTenantDomain(tenantDomain);
        //
        //            ApplicationManagementService appInfo = ApplicationManagementService.getInstance();
        //            ServiceProvider application = appInfo.getServiceProviderByClientId(issuerName, SAMLSSOConstants
        //                    .SAMLFormFields.SAML_SSO, tenantDomain);
        //            Map<String, Property> properties = new HashMap();
        //            for (InboundAuthenticationRequestConfig authenticationRequestConfig : application
        //                    .getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs()) {
        //                if (StringUtils.equals(authenticationRequestConfig.getInboundAuthType(), SAMLSSOConstants
        //                        .SAMLFormFields.SAML_SSO) && StringUtils.equals(authenticationRequestConfig
        //                        .getInboundAuthKey(), issuerName)) {
        //                    for (Property property : authenticationRequestConfig.getProperties()) {
        //                        properties.put(property.getName(), property);
        //                    }
        //                }
        //            }
        //
        //            if (StringUtils.isBlank(requestedACSUrl) || properties.get(SAMLSSOConstants.SAMLFormFields
        // .ACS_URLS) ==
        //                    null || properties.get(SAMLSSOConstants.SAMLFormFields.ACS_URLS).getValue() == null ||
        // !Arrays
        //                    .asList(properties.get(SAMLSSOConstants.SAMLFormFields.ACS_URLS).getValue().split
        //                            (SAMLSSOConstants.SAMLFormFields.ACS_SEPERATE_CHAR)).contains(requestedACSUrl)) {
        //                String msg = "ALERT: Invalid Assertion Consumer URL value '" + requestedACSUrl + "' in the " +
        //                        "AuthnRequest message from  the issuer '" + issuerName + "'. Possibly " + "an
        // attempt for a " +
        //                        "spoofing attack";
        //                log.error(msg);
        //                return false;
        //            } else {
        //                return true;
        //            }
        //        } catch (IdentityApplicationManagementException e) {
        //            throw new SAMLServerException("Error occurred while validating existence of SAML service
        // provider " +
        //                    "'" + issuerName + "' in the tenant domain '" + tenantDomain + "'");
        //        } finally {
        //            PrivilegedCarbonContext.endTenantFlow();
        //        }

    }

    public static boolean isSAMLIssuerExists(String issuerName) throws SAMLServerException {
        return true;
        // TODO
        //        SSOServiceProviderConfigManager stratosIdpConfigManager = SSOServiceProviderConfigManager
        // .getInstance();
        //        SAMLSSOServiceProviderDO serviceProvider = stratosIdpConfigManager.getServiceProvider(issuerName);
        //        if (serviceProvider != null) {
        //            return true;
        //        }
        //
        //        int tenantId;
        //        if (StringUtils.isBlank(tenantDomain)) {
        //            tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        //            tenantId = MultitenantConstants.SUPER_TENANT_ID;
        //        } else {
        //            try {
        //                tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        //            } catch (UserStoreException e) {
        //                throw new SAMLServerException("Error occurred while retrieving tenant id for the domain : " +
        //                        tenantDomain, e);
        //            }
        //        }
        //
        //        try {
        //            PrivilegedCarbonContext.startTenantFlow();
        //            PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext
        // .getThreadLocalCarbonContext();
        //            privilegedCarbonContext.setTenantId(tenantId);
        //            privilegedCarbonContext.setTenantDomain(tenantDomain);
        //
        //            ApplicationManagementService appInfo = ApplicationManagementService.getInstance();
        //            ServiceProvider application = appInfo.getServiceProviderByClientId(issuerName, SAMLSSOConstants
        //                    .SAMLFormFields.SAML_SSO, tenantDomain);
        //            if (application != null) {
        //                for (InboundAuthenticationRequestConfig config : application.getInboundAuthenticationConfig()
        //                        .getInboundAuthenticationRequestConfigs()) {
        //                    if (StringUtils.equals(config.getInboundAuthKey(), issuerName) && StringUtils.equals
        // (config
        //                            .getInboundAuthType(), SAMLSSOConstants.SAMLFormFields.SAML_SSO)) {
        //                        return true;
        //                    }
        //                }
        //            }
        //            return false;
        //        } catch (IdentityApplicationManagementException e) {
        //            throw new SAMLServerException("Error occurred while validating existence of SAML service
        // provider " +
        //                    "'" + issuerName + "' in the tenant domain '" + tenantDomain + "'");
        //        } finally {
        //            PrivilegedCarbonContext.endTenantFlow();
        //        }
    }

    /**
     * Get the Issuer
     *
     * @return Issuer
     */
    public static Issuer getIssuer() {

        return getIssuerFromTenantDomain();
    }

    public static Issuer getIssuerFromTenantDomain() {

        Issuer issuer = new IssuerBuilder().buildObject();
        String idPEntityId = SAMLConfigurations.getInstance().getIdpEntityId();
        if (idPEntityId == null) {
            idPEntityId = "SSOService.EntityID";
        }
        issuer.setValue(idPEntityId);
        issuer.setFormat(SAMLSSOConstants.NAME_ID_POLICY_ENTITY);
        return issuer;
    }

    public static String createID() {

        try {
            SecureRandomIdentifierGenerator generator = new SecureRandomIdentifierGenerator();
            return generator.generateIdentifier();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error while building Secure Random ID", e);
            //TODO : throw exception and break the flow
        }
        return null;
    }

    /**
     * Sign the SAML Assertion
     *
     * @param response
     * @param signatureAlgorithm
     * @param digestAlgorithm
     * @param cred
     * @return
     * @throws IdentityException
     */
    public static Assertion setSignature(Assertion response, String signatureAlgorithm, String digestAlgorithm,
                                         X509Credential cred) throws IdentityException {

        return (Assertion) doSetSignature(response, signatureAlgorithm, digestAlgorithm, cred);
    }

    /**
     * Sign the SAML Response message
     *
     * @param response
     * @param signatureAlgorithm
     * @param digestAlgorithm
     * @param cred
     * @return
     * @throws IdentityException
     */

    public static Response setSignature(Response response, String signatureAlgorithm, String digestAlgorithm,
                                        X509Credential cred) throws IdentityException {

        return (Response) doSetSignature(response, signatureAlgorithm, digestAlgorithm, cred);
    }

    /**
     * Generic method to sign SAML Logout Request
     *
     * @param request
     * @param signatureAlgorithm
     * @param digestAlgorithm
     * @param cred
     * @return
     * @throws IdentityException
     */
    private static SignableXMLObject doSetSignature(SignableXMLObject request, String signatureAlgorithm, String
            digestAlgorithm, X509Credential cred) throws IdentityException {

        SSOSigner ssoSigner = new DefaultSSOSigner();

        return ssoSigner.setSignature(request, signatureAlgorithm, digestAlgorithm, cred);
    }

    /**
     * Build the StatusCode for Status of Response
     *
     * @param parentStatusCode
     * @param childStatusCode
     * @return
     */
    private static StatusCode buildStatusCode(String parentStatusCode, StatusCode childStatusCode)
            throws SAMLServerException {
        if (parentStatusCode == null) {
            throw new SAMLServerException("Invalid SAML Response Status Code");
        }

        StatusCode statusCode = new StatusCodeBuilder().buildObject();
        statusCode.setValue(parentStatusCode);

        //Set the status Message
        if (childStatusCode != null) {
            statusCode.setStatusCode(childStatusCode);
            return statusCode;
        } else {
            return statusCode;
        }
    }

    /**
     * Set the StatusMessage for Status of Response
     *
     * @param statusMsg
     * @return
     */
    private static Status buildStatusMsg(Status status, String statusMsg) {
        if (statusMsg != null) {
            StatusMessage statusMesssage = new StatusMessageBuilder().buildObject();
            statusMesssage.setMessage(statusMsg);
            status.setStatusMessage(statusMesssage);
        }
        return status;
    }

    /**
     * Get the X509CredentialImpl object for a particular tenant
     *
     * @param alias
     * @return X509CredentialImpl object containing the public certificate of that tenant
     * @throws SAMLServerException
     *         Error when creating X509CredentialImpl object
     */
    public static X509CredentialImpl getX509CredentialImplForTenant(String alias)
            throws SAMLServerException {


        KeyStoreManager keyStoreManager;
        // get an instance of the corresponding Key Store Manager instance
        try {
            keyStoreManager = KeyStoreManager.getInstance();
            X509CredentialImpl credentialImpl = null;
            KeyStore keyStore;
            keyStore = keyStoreManager.getKeyStore();

            java.security.cert.X509Certificate cert =
                    (java.security.cert.X509Certificate) keyStore.getCertificate(alias);
            credentialImpl = new X509CredentialImpl(cert);
            return credentialImpl;
        } catch (Exception e) {
            throw new SAMLServerException("Error while initializing keystore");
        }
    }

    /**
     * Return a Array of Claims containing requested attributes and values
     *
     * @param authenticationContext
     * @return Map with attributes and values
     * @throws IdentityException
     */
    public static Map<String, String> getAttributes(AuthenticationContext authenticationContext
    ) throws IdentityException {

        int index = 0;
        SAMLMessageContext samlMessageContext = (SAMLMessageContext) authenticationContext
                .getParameter(SAMLSSOConstants.SAMLContext);

        SAMLResponseHandlerConfig samlResponseHandlerConfig = samlMessageContext.getResponseHandlerConfig();
        if (!samlMessageContext.isIdpInitSSO()) {

            if (samlMessageContext.getAttributeConsumingServiceIndex() == 0) {
                //SP has not provide a AttributeConsumingServiceIndex in the authnReqDTO
                if (StringUtils.isNotBlank(samlResponseHandlerConfig.getAttributeConsumingServiceIndex()) &&
                    samlResponseHandlerConfig.isEnableAttributesByDefault()) {
                    index = Integer.parseInt(samlResponseHandlerConfig.getAttributeConsumingServiceIndex());
                } else {
                    return null;
                }
            } else {
                //SP has provide a AttributeConsumingServiceIndex in the authnReqDTO
                index = samlMessageContext.getAttributeConsumingServiceIndex();
            }
        } else {
            if (StringUtils.isNotBlank(samlResponseHandlerConfig.getAttributeConsumingServiceIndex()) &&
                samlResponseHandlerConfig.isEnableAttributesByDefault()) {
                index = Integer.parseInt(samlResponseHandlerConfig.getAttributeConsumingServiceIndex());
            } else {
                return null;
            }
        }


		/*
         * IMPORTANT : checking if the consumer index in the request matches the
		 * given id to the SP
		 */
        if (samlResponseHandlerConfig.getAttributeConsumingServiceIndex() == null ||
            "".equals(samlResponseHandlerConfig.getAttributeConsumingServiceIndex()) ||
            index != Integer.parseInt(samlResponseHandlerConfig.getAttributeConsumingServiceIndex())) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid AttributeConsumingServiceIndex in AuthnRequest");
            }
            return Collections.emptyMap();
        }

        Map<String, String> claimsMap = new HashMap<String, String>();
        Set<Claim> aggregatedClaims = authenticationContext.getSequenceContext().getAllClaims();
        String profileName = authenticationContext.getServiceProvider().getClaimConfig().getProfile();
        String dialect = authenticationContext.getServiceProvider().getClaimConfig().getDialectUri();

        if (StringUtils.isEmpty(dialect)) {
            dialect = "default";
        }

        aggregatedClaims = SAMLInboundServiceHolder.getInstance()
                .getGatewayClaimResolverService().transformToOtherDialect(aggregatedClaims, dialect, Optional
                        .ofNullable(profileName));

        aggregatedClaims.stream().forEach(claim -> claimsMap.put(claim.getClaimUri(), claim.getValue()));
        return claimsMap;
    }

    public static int getSingleLogoutRetryCount() {
        return singleLogoutRetryCount;
    }

    public static void setSingleLogoutRetryCount(int singleLogoutRetryCount) {
        SAMLSSOUtil.singleLogoutRetryCount = singleLogoutRetryCount;
    }

    public static long getSingleLogoutRetryInterval() {
        return singleLogoutRetryInterval;
    }

    public static void setSingleLogoutRetryInterval(long singleLogoutRetryInterval) {
        SAMLSSOUtil.singleLogoutRetryInterval = singleLogoutRetryInterval;
    }

    // TODO fix this to get proper subject
    public static String getSubject(AuthenticationContext authenticationContext) {
        if (authenticationContext.getSequenceContext() != null && authenticationContext.getSequenceContext()
                                                                          .getStepContext(1) != null
            && authenticationContext.getSequenceContext().getStepContext(1).getUser()
               != null) {
            return authenticationContext.getSequenceContext().getStepContext(1).getUser().getUserIdentifier();
        }
        return null;
    }

    public static class SAMLAssertion {

        /**
         * Constructing the AuthnRequest Object from a String
         *
         * @param authReqStr
         *         Decoded AuthReq String
         * @return AuthnRequest Object
         * @throws
         */
        public static XMLObject unmarshall(String authReqStr) {
            InputStream inputStream = null;
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setNamespaceAware(true);

                documentBuilderFactory.setExpandEntityReferences(false);
                documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                org.apache.xerces.util.SecurityManager securityManager = new SecurityManager();
                securityManager.setEntityExpansionLimit(ENTITY_EXPANSION_LIMIT);
                documentBuilderFactory.setAttribute(SECURITY_MANAGER_PROPERTY, securityManager);

                DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
                docBuilder.setEntityResolver(new CarbonEntityResolver());
                inputStream = new ByteArrayInputStream(authReqStr.trim().getBytes(StandardCharsets.UTF_8));
                Document document = docBuilder.parse(inputStream);
                Element element = document.getDocumentElement();
                UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
                Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
                return unmarshaller.unmarshall(element);
            } catch (Exception e) {
                log.error("Error in constructing AuthRequest from the encoded String", e);
                throw new SAMLRuntimeException(
                        "Error in constructing AuthRequest from the encoded String ",
                        e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing the stream", e);
                    }
                }
            }
        }

        /**
         * Serialize the Auth. Request
         *
         * @param xmlObject
         * @return serialized auth. req
         */
        public static String marshall(XMLObject xmlObject) throws SAMLServerException {

            ByteArrayOutputStream byteArrayOutputStrm = null;
            try {
                //            System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                //                    "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
                MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration.getMarshallerFactory();
                Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);
                Element element = marshaller.marshall(xmlObject);

                byteArrayOutputStrm = new ByteArrayOutputStream();
                DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
                DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
                LSSerializer writer = impl.createLSSerializer();
                LSOutput output = impl.createLSOutput();
                output.setByteStream(byteArrayOutputStrm);
                writer.write(element, output);
                return byteArrayOutputStrm.toString(StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                log.error("Error Serializing the SAML Response");
                throw new SAMLServerException("Error Serializing the SAML Response", e);
            } finally {
                if (byteArrayOutputStrm != null) {
                    try {
                        byteArrayOutputStrm.close();
                    } catch (IOException e) {
                        log.error("Error while closing the stream", e);
                    }
                }
            }
        }

        /**
         * Encoding the response
         *
         * @param xmlString
         *         String to be encoded
         * @return encoded String
         */
        public static String encode(String xmlString) {
            // Encoding the message
            String encodedRequestMessage =
                    Base64.encodeBytes(xmlString.getBytes(StandardCharsets.UTF_8),
                                       Base64.DONT_BREAK_LINES);
            return encodedRequestMessage.trim();
        }

        /**
         * Decoding and deflating the encoded AuthReq
         *
         * @param encodedStr
         *         encoded AuthReq
         * @return decoded AuthReq
         */
        public static String decode(String encodedStr) {
            try {
                org.apache.commons.codec.binary.Base64 base64Decoder =
                        new org.apache.commons.codec.binary.Base64();
                byte[] xmlBytes = encodedStr.getBytes(StandardCharsets.UTF_8.name());
                byte[] base64DecodedByteArray = base64Decoder.decode(xmlBytes);

                try {
                    Inflater inflater = new Inflater(true);
                    inflater.setInput(base64DecodedByteArray);
                    byte[] xmlMessageBytes = new byte[10000];
                    int resultLength = inflater.inflate(xmlMessageBytes);

                    if (!inflater.finished()) {
                        throw new RuntimeException("End of the compressed data stream has NOT been reached");
                    }

                    inflater.end();
                    String decodedString = new String(xmlMessageBytes, 0, resultLength, StandardCharsets.UTF_8.name());
                    if (log.isDebugEnabled()) {
                        log.debug("Request message " + decodedString);
                    }
                    return decodedString;
                } catch (DataFormatException e) {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(base64DecodedByteArray);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    InflaterInputStream iis = new InflaterInputStream(byteArrayInputStream);
                    byte[] buf = new byte[1024];
                    int count = iis.read(buf);
                    while (count != -1) {
                        byteArrayOutputStream.write(buf, 0, count);
                        count = iis.read(buf);
                    }
                    iis.close();
                    String decodedStr = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
                    if (log.isDebugEnabled()) {
                        log.debug("Request message " + decodedStr, e);
                    }
                    return decodedStr;
                }
            } catch (IOException e) {
                throw new SAMLRuntimeException("Error when decoding the SAML Request.", e);
            }
        }


        public static String decodeForPost(String encodedStr) {
            try {
                org.apache.commons.codec.binary.Base64 base64Decoder = new org.apache.commons.codec.binary.Base64();
                byte[] xmlBytes = encodedStr.getBytes(StandardCharsets.UTF_8.name());
                byte[] base64DecodedByteArray = base64Decoder.decode(xmlBytes);

                String decodedString = new String(base64DecodedByteArray, StandardCharsets.UTF_8.name());
                if (log.isDebugEnabled()) {
                    log.debug("Request message " + decodedString);
                }
                return decodedString;
            } catch (IOException e) {
                throw new SAMLRuntimeException("Error when decoding the SAML Request.", e);
            }
        }
    }

    public static class SAMLResponseUtil {

        /**
         * build the error response
         *
         * @param status
         * @param message
         * @return decoded response
         * @throws org.wso2.carbon.identity
         */
        public static String buildErrorResponse(String status, String message, String destination) {

            List<String> statusCodeList = new ArrayList<String>();
            statusCodeList.add(status);
            //Do below in the response builder
            String errorResp = null;
            try {
                Response response = buildResponse("asdfasd", statusCodeList, message, destination);
                errorResp = compressResponse(SAMLAssertion.marshall(response));
            } catch (SAMLServerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return errorResp;
        }

        public static String buildErrorResponse(String id,
                                                List<String> statusCodes,
                                                String statusMsg,
                                                String destination)
                throws IdentityException {
            Response response = buildResponse(id, statusCodes, statusMsg, destination);
            return SAMLAssertion.encode(SAMLAssertion.marshall(response));
        }

        /**
         * Build the error response
         *
         * @return
         */
        public static Response buildResponse(String inResponseToID, List<String> statusCodes, String statusMsg, String
                destination) throws SAMLServerException {

            Response response = new ResponseBuilder().buildObject();

            if (statusCodes == null || statusCodes.isEmpty()) {
                throw new SAMLServerException("No Status Values");
            }
            response.setIssuer(SAMLSSOUtil.getIssuer());
            Status status = new StatusBuilder().buildObject();
            StatusCode statusCode = null;
            for (String statCode : statusCodes) {
                statusCode = buildStatusCode(statCode, statusCode);
            }
            status.setStatusCode(statusCode);
            buildStatusMsg(status, statusMsg);
            response.setStatus(status);
            response.setVersion(SAMLVersion.VERSION_20);
            response.setID(SAMLSSOUtil.createID());
            if (inResponseToID != null) {
                response.setInResponseTo(inResponseToID);
            }
            if (destination != null) {
                response.setDestination(destination);
            }
            response.setIssueInstant(new DateTime());
            return response;
        }


        /**
         * Compresses the response String
         *
         * @param response
         * @return
         * @throws IOException
         */
        public static String compressResponse(String response) throws IOException {

            Deflater deflater = new Deflater(Deflater.DEFLATED, true);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
            try {
                deflaterOutputStream.write(response.getBytes(StandardCharsets.UTF_8));
            } finally {
                deflaterOutputStream.close();
            }
            return Base64.encodeBytes(byteArrayOutputStream.toByteArray(), Base64.DONT_BREAK_LINES);
        }
    }
}
