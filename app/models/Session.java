package models;

import org.joda.time.DateTime;
import util.SitnetUtil;

import java.util.UUID;

public class Session {
    private Long userId;
    private DateTime since;
    private Boolean valid;
    private String xsrfToken;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DateTime getSince() {
        return since;
    }

    public void setSince(DateTime since) {
        this.since = since;
    }

    public Boolean isValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public void setXsrfToken() {
        xsrfToken = SitnetUtil.encodeMD5(UUID.randomUUID().toString());
    }

    public String getXsrfToken() {
        return xsrfToken;
    }
}
