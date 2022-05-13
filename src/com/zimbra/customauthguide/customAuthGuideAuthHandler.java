package com.zimbra.customauthguide;

import java.util.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.auth.ZimbraCustomAuth;
import com.zimbra.cs.listeners.AuthListener;

public class customAuthGuideAuthHandler extends ZimbraCustomAuth {
    public void register(String id) {
        ZimbraCustomAuth.register(id, this);
    }

    /*
     * Method invoked by the framework to handle authentication requests.
     * A custom auth implementation must implement this abstract method.
     *
     * @param account: The account object of the principal to be authenticated
     *                 all attributes of the account can be retrieved from this object.
     *
     * @param password: Clear-text password.
     *
     * @param context: Map containing context information.
     *                 A list of context data is defined in com.zimbra.cs.account.AuthContext
     * https://github.com/Zimbra/zm-mailbox/blob/develop/store/src/java/com/zimbra/cs/account/auth/AuthContext.java
     *
     * @return Returning from this function indicating the authentication has succeeded.
     *
     * @throws Exception.  If authentication failed, an Exception should be thrown.
     *
     */
    @Override
    public void authenticate(Account account, String password, Map<String, Object> context, List<String> args) throws ServiceException {
        if (!isAuthenticated(account, password, context)) {
            AccountServiceException.AuthFailedServiceException afse = AccountServiceException.AuthFailedServiceException.AUTH_FAILED("customAuthGuide Authentication failed");
            AuthListener.invokeOnException(afse);
            throw afse;
        }
    }

    /*
    * Implement log-in validation here
    * */
    protected boolean isAuthenticated(Account account, String password, Map<String, Object> context) {
        //THIS IS JUST AN EXAMPLE, NEVER HARDCODE USERNAMES, PASSWORDS AND IP'S THIS WAY!!
        if (("testuser@example.com".equals(account.getName())) && ("test123".equals(password))) {
            if ("imap".equals(context.get("proto").toString())) {
                ZimbraLog.account.warn("customAuthGuide Authentication failed, IMAP is not permitted for this user %s", account.getName());
                return false;
            }

            //to make this work make sure to read https://wiki.zimbra.com/wiki/Secopstips#Log_the_correct_origination_IP
            if ("54.83.74.191".equals(context.get("ocip"))) {
                ZimbraLog.account.warn("customAuthGuide Authentication failed, IP is not permitted for this user %s", account.getName());
                return false;
            }
            ZimbraLog.account.info("customAuthGuide Authentication success %s", account.getName());
            return true; //only return true if authentication has succeeded!
        }
        return false;
    }
}
