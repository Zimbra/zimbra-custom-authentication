# Implementing Custom Authentication using a Zimbra extension

How to create a custom authentication extension for Zimbra

In this article you will learn how to implement Custom Authentication using a Zimbra extension. The Java project and source code can be found at https://github.com/Zimbra/zm-custom-auth-guide.

Take a look at https://github.com/Zimbra/zm-extension-guide if you are new to Java or building Zimbra extensions. The zm-extension-guide covers all the things needed to build the Custom Authentication extension.

Zimbra by default supports authenticating to LDAP, Active Directory, SAML and Pre-Auth (see further reading section below). In some cases you may want to implement a Custom Authentication extension for Zimbra. For example if you want to authenticate using an external REST API or if you want to implement additional restrictions for log-in such as restrictions per user, protocol (imap, soap, etc) and IP address.

## Custom Authentication in Java 

To implement the Custom Authentication extension you need to structure it as follows:

````
src
└── com
    └── zimbra
        └── customauthguide
            ├── customAuthGuideAuthHandler.java
            └── customAuthGuideExtension.java
META-INF
└── MANIFEST.MF
````

The file customAuthGuideExtension.java implements a Zimbra Extension as explained in https://github.com/Zimbra/zm-extension-guide. The customAuthGuideAuthHandler is instantiated in the line: `new customAuthGuideAuthHandler().register(ID);`

````java
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
````
The file customAuthGuideAuthHandler.java is where the actual custom authentication needs to be implemented. Specifically the `authenticate` method is called each time a user tries to log-in.

````java
package com.zimbra.customauthguide;

import java.util.*;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.auth.ZimbraCustomAuth;

public class customAuthGuideAuthHandler extends ZimbraCustomAuth {
    public void register(String id) {
        ZimbraCustomAuth.register(id, this);
    }

    @Override
    public void authenticate(Account account, String password, Map<String, Object> context, List<String> args) throws Exception {
        if (("testuser@example.com".equals(account.getName())) && ("test123".equals(password))) {
            if ("imap".equals(context.get("proto").toString())) {
                throw new Exception("customAuthGuide Authentication failed, IMAP is not permitted for this user");
            }

            //to make this work make sure to read https://wiki.zimbra.com/wiki/Secopstips#Log_the_correct_origination_IP
            if ("54.83.74.191".equals(context.get("ocip"))) {
                throw new Exception("customAuthGuide Authentication failed, IP is not permitted for this user");
            }

            ZimbraLog.account.info("customAuthGuide Authentication success %s", account.getName());
            return;
        }
        throw new Exception("customAuthGuide Authentication failed");
    }
}

````

In this example implementation everything is hard-coded for sake of simplicity. Instead of hard-coding you can make REST API calls using normal Java code or implement database look-ups.

**If the authenticate method returns without an exception it means the authentication was successful. Or in other words you MUST make sure to always throw an exception for authentication failures!**

A number of useful parameters are passed to the `authenticate` method that you can use to implement the authentication:

| Parameter | Description |
|---|---|
| account | the account object of the user that want to authenticate, account.getName() returns the primary email address of the user |
| password | the plain text password typed by the user |
| context | see description below |

The context parameter holds meta data from the request that you can use to refine your custom authentication:

| Variable | Description |
|---|---|
| ocip | IP address from the Originating IP header |
| ua | User Agent of the client |
| proto | Protocol being logged into (http_basic, http_dav, im, imap, pop3, soap, spnego, zsync) |

For the originating IP to work, the Zimbra server needs to be configured correctly see https://wiki.zimbra.com/wiki/Secopstips#Log_the_correct_origination_IP for more information.

The extension needs to be build as a jar and then configured as below.

## Configuring custom authentication on Zimbra

Install the jar file on Zimbra as follows:

````
mkdir /opt/zimbra/lib/ext/customAuthGuide
cp /tmp/customAuthGuide.jar /opt/zimbra/lib/ext/customAuthGuide
````

Create a test domain and user and enable this custom authentication as follows:

````
sudo su zimbra -
zmprov cd example.com
zmprov ca testuser@example.com thispassworddoesnotwork
zmprov md example.com zimbraAuthMech custom:customAuthGuide
zmprov md example.com zimbraAuthFallbackToLocal FALSE
zmmailboxdctl restart
````

You are now ready to test your extension by logging into the Web-UI using user `testuser@example.com` and the password `test123`, confirm that the Zimbra fallback password `thispassworddoesnotwork` does not work.

You can run `tail -f /opt/zimbra/log/mailbox.log` while testing to see the logs. An example of successful authentication:

````
2022-05-12 08:48:43,178 INFO  [qtp1335505684-15://localhost:8080/service/soap/BatchRequest] [name=testuser@example.com;oip=192.168.1.114;ua=zclient/9.0.0_GA_4258;soapId=7ab4b771;] account - customAuthGuide Authentication success testuser@example.com
2022-05-12 08:48:43,180 INFO  [qtp1335505684-15://localhost:8080/service/soap/BatchRequest] [name=testuser@example.com;oip=192.168.1.114;ua=zclient/9.0.0_GA_4258;soapId=7ab4b771;] account - Authentication successful for user: testuser@example.com
````

And various examples of failed authentication:

````
2022-05-12 08:50:21,072 INFO  [qtp1335505684-125://localhost:8080/service/soap/BatchRequest] [name=testuser@example.com;oip=192.168.1.114;ua=zclient/9.0.0_GA_4258;soapId=7ab4b781;] account - Error occurred during authentication: authentication failed for [testuser@example.com (customAuthGuide Authentication failed)]. Reason:  (customAuthGuide Authentication failed).
2022-05-12 08:50:21,073 INFO  [qtp1335505684-125://localhost:8080/service/soap/BatchRequest] [name=testuser@example.com;oip=192.168.1.114;ua=zclient/9.0.0_GA_4258;soapId=7ab4b781;] SoapEngine - handler exception: authentication failed for [testuser@example.com (customAuthGuide Authentication failed)],  (customAuthGuide Authentication failed)

...

2022-05-12 08:54:50,081 INFO  [qtp1335505684-116://localhost:8080/service/soap/BatchRequest] [name=testuser@example.com;oip=192.168.1.114;ua=zclient/9.0.0_GA_4258;soapId=72aefed5;] account - Error occurred during authentication: authentication failed for [testuser@example.com (customAuthGuide Authentication failed, IP is not permitted for this user)]. Reason:  (customAuthGuide Authentication failed, IP is not permitted for this user).
2022-05-12 08:54:50,082 INFO  [qtp1335505684-116://localhost:8080/service/soap/BatchRequest] [name=testuser@example.com;oip=192.168.1.114;ua=zclient/9.0.0_GA_4258;soapId=72aefed5;] SoapEngine - handler exception: authentication failed for [testuser@example.com (customAuthGuide Authentication failed, IP is not permitted for this user)],  (customAuthGuide Authentication failed, IP is not permitted for this user)
````
## Further reading

- https://wiki.zimbra.com/wiki/Secopstips#Pre-authentication 
- https://wiki.zimbra.com/wiki/Preauth
- https://github.com/Zimbra-Community/zimbra-tools/blob/master/pre-auth-soap-saml.php
