package io.swagger.gdd;

import java.util.Map;

/**
 * Models the "auth" object.
 */
public class Auth {
    private OAuth2 oauth2;

    public static class OAuth2 {
        private Map<String, Scope> scopes;

        public static class Scope {
            private String description;

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }
        }

        public Map<String, Scope> getScopes() {
            return scopes;
        }

        public void setScopes(Map<String, Scope> scopes) {
            this.scopes = scopes;
        }
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2 oauth2) {
        this.oauth2 = oauth2;
    }
}
