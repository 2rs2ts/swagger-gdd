package io.swagger.gdd;

import java.util.Map;

/**
 * Models the "auth" object.
 */
public class Auth {
    public OAuth2 oauth2;

    public static class OAuth2 {
        public Map<String, Scope> scopes;

        public static class Scope {
            public String description;
        }
    }
}
