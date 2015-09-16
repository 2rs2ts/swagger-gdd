package io.swagger.gdd;

import java.util.Map;

/**
 * Models the "oauth2" field of Auth.
 */
public class OAuth2 {
    private Map<String, Scope> scopes;

    public Map<String, Scope> getScopes() {
        return scopes;
    }

    public void setScopes(Map<String, Scope> scopes) {
        this.scopes = scopes;
    }
}
