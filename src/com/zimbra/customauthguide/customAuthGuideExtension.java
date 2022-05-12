package com.zimbra.customauthguide;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.extension.ZimbraExtension;

/**
 * This extension registers a custom authentication handler which is implemented in customAuthGuideAuthHandler.java
 *
 * To enable it, put the jar in /opt/zimbra/lib/ext/customauthguide restart Zimbra mailbox and enable it on a domain using:
 * zmprov md example.com zimbraAuthMech custom:customAuthGuide
 * zmprov md example.com zimbraAuthFallbackToLocal FALSE
 *
 * to switch back to normal Zimbra authentication:
 * zmprov md example.com zimbraAuthMech "zimbra"
 * zmprov md example.com zimbraAuthFallbackToLocal TRUE
 *
 * Please be advised: At this time, Zimbra will still allow using only a password for admin accounts, this is a bug. See https://github.com/Zimbra/zm-mailbox/pull/448 and https://bugzilla.zimbra.com/show_bug.cgi?id=80485 this means, you need to create a separate admin account, put a long password on it, and don't use it for day-to-day work.
 *
 * @author Barry de Graaff
 */
public class customAuthGuideExtension implements ZimbraExtension {
    // This string is used to refer to this extension
    public static final String ID = "customAuthGuide";
    /**
     * Defines a name for the extension. It must be an identifier.
     *
     * @return extension name
     */
    public String getName() {
        return ID;
    }

    /**
     * Initializes the extension. Called when the extension is loaded.
     *
     * @throws com.zimbra.common.service.ServiceException
     */
    public void init() throws ServiceException {

        //Custom Authentication Handler
        new customAuthGuideAuthHandler().register(ID);
    }

    /**
     * Terminates the extension. Called when the server is shut down.
     */
    public void destroy() {

    }
}
