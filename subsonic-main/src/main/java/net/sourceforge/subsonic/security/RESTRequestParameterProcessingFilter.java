/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.controller.JAXBWriter;
import net.sourceforge.subsonic.controller.RESTController;
import net.sourceforge.subsonic.domain.LicenseInfo;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.domain.Version;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.util.StringUtil;

/**
 * Performs authentication based on credentials being present in the HTTP request parameters. Also checks
 * API versions and license information.
 * <p/>
 * The username should be set in parameter "u", and the password should be set in parameter "p".
 * The REST protocol version should be set in parameter "v".
 * <p/>
 * The password can either be in plain text or be UTF-8 hexencoded preceded by "enc:".
 *
 * @author Sindre Mehus
 */
public class RESTRequestParameterProcessingFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(RESTRequestParameterProcessingFilter.class);

    private final JAXBWriter jaxbWriter = new JAXBWriter();
    private ProviderManager authenticationManager;
    private SettingsService settingsService;
    private SecurityService securityService;
    private LoginFailureLogger loginFailureLogger;

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            throw new ServletException("Can only process HttpServletRequest");
        }
        if (!(response instanceof HttpServletResponse)) {
            throw new ServletException("Can only process HttpServletResponse");
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String username = StringUtils.trimToNull(httpRequest.getParameter("u"));
        String password = decrypt(StringUtils.trimToNull(httpRequest.getParameter("p")));
        String salt = StringUtils.trimToNull(httpRequest.getParameter("s"));
        String token = StringUtils.trimToNull(httpRequest.getParameter("t"));
        String version = StringUtils.trimToNull(httpRequest.getParameter("v"));
        String client = StringUtils.trimToNull(httpRequest.getParameter("c"));

        RESTController.ErrorCode errorCode = null;

        // The username and credentials parameters are not required if the user
        // was previously authenticated, for example using Basic Auth.
        boolean passwordOrTokenPresent = password != null || (salt != null && token != null);
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        boolean missingCredentials = previousAuth == null && (username == null || !passwordOrTokenPresent);
        if (missingCredentials || version == null || client == null) {
            errorCode = RESTController.ErrorCode.MISSING_PARAMETER;
        }

        if (errorCode == null) {
            errorCode = checkAPIVersion(version);
        }

        if (errorCode == null) {
            errorCode = authenticate(username, password, salt, token, previousAuth);
        }

        if (errorCode == null) {
            errorCode = checkLicense(client);
        }

        if (errorCode == null) {
            chain.doFilter(request, response);
        } else {
            if (errorCode == RESTController.ErrorCode.NOT_AUTHENTICATED) {
                loginFailureLogger.log(request.getRemoteAddr(), username);
            }
            SecurityContextHolder.getContext().setAuthentication(null);
            sendErrorXml(httpRequest, httpResponse, errorCode);
        }
    }

    private RESTController.ErrorCode checkAPIVersion(String version) {
        Version serverVersion = new Version(jaxbWriter.getRestProtocolVersion());
        Version clientVersion = new Version(version);

        if (serverVersion.getMajor() > clientVersion.getMajor()) {
            return RESTController.ErrorCode.PROTOCOL_MISMATCH_CLIENT_TOO_OLD;
        } else if (serverVersion.getMajor() < clientVersion.getMajor()) {
            return RESTController.ErrorCode.PROTOCOL_MISMATCH_SERVER_TOO_OLD;
        } else if (serverVersion.getMinor() < clientVersion.getMinor()) {
            return RESTController.ErrorCode.PROTOCOL_MISMATCH_SERVER_TOO_OLD;
        }
        return null;
    }

    private RESTController.ErrorCode authenticate(String username, String password, String salt, String token, Authentication previousAuth) {

        // Previously authenticated and username not overridden?
        if (username == null && previousAuth != null) {
            return null;
        }

        if (salt != null && token != null) {
            User user = securityService.getUserByName(username);
            if (user == null) {
                return RESTController.ErrorCode.NOT_AUTHENTICATED;
            }
            String expectedToken = DigestUtils.md5Hex(user.getPassword() + salt);
            if (!expectedToken.equals(token)) {
                return RESTController.ErrorCode.NOT_AUTHENTICATED;
            }

            password = user.getPassword();
        }

        if (password != null) {
            try {
                UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
                Authentication authResult = authenticationManager.authenticate(authRequest);
                SecurityContextHolder.getContext().setAuthentication(authResult);
                return null;
            } catch (AuthenticationException x) {
                return RESTController.ErrorCode.NOT_AUTHENTICATED;
            }
        }

        return RESTController.ErrorCode.MISSING_PARAMETER;
    }

    private RESTController.ErrorCode checkLicense(String client) {
        LicenseInfo licenseInfo = settingsService.getLicenseInfo();
        if (licenseInfo.isLicenseOrTrialValid()) {
            return null;
        }
        LOG.info("REST access for client '" + client + "' has expired.");
        return RESTController.ErrorCode.NOT_LICENSED;
    }

    public static String decrypt(String s) {
        if (s == null) {
            return null;
        }
        if (!s.startsWith("enc:")) {
            return s;
        }
        try {
            return StringUtil.utf8HexDecode(s.substring(4));
        } catch (Exception e) {
            return s;
        }
    }

    private void sendErrorXml(HttpServletRequest request, HttpServletResponse response, RESTController.ErrorCode errorCode) throws IOException {
        try {
            jaxbWriter.writeErrorResponse(request, response, errorCode, errorCode.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to send error response.", e);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    public void setAuthenticationManager(ProviderManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setLoginFailureLogger(LoginFailureLogger loginFailureLogger) {
        this.loginFailureLogger = loginFailureLogger;
    }
}
