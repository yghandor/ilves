package org.vaadin.addons.sitekit.util;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Link;
import org.apache.commons.codec.digest.DigestUtils;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class for accessing gravatar images.
 */
public class GravatarUtil {
    /** The gravatar URL. */
    private final static String GRAVATAR_URL = "http://www.gravatar.com/avatar/";

    public static Link getGravatarImageLink(final String email) {
        final URL gravatarUrl = GravatarUtil.getGravatarUrl(email);
        final Link link = new Link(null, new ExternalResource("http://www.gravatar.com/"));
        link.setStyleName("gravatar");
        link.setIcon(new ExternalResource(gravatarUrl));
        link.setWidth(32, Sizeable.Unit.PIXELS);
        link.setHeight(32, Sizeable.Unit.PIXELS);
        return link;
    }

    /**
     * Get the gravatar URL based on email address.
     * @param email the email address
     * @return the gravatar URL
     */
    public static URL getGravatarUrl(final String email) {
        if (Site.getCurrent().getSiteContext().getObject("gravatar-url") == null) {
            Site.getCurrent().getSiteContext().putObject("gravatar-url", constructGravatarUrl(email));
        }
        try {
            return new URL((String) Site.getCurrent().getSiteContext().getObject("gravatar-url"));
        } catch (MalformedURLException e) {
            throw new SiteException("Error in gravatar URL format: "
                    + Site.getCurrent().getSiteContext().getObject("gravatar-url"));
        }
    }

    /**
     * Constructs gravatar URL from email address.
     * @param email the email address
     * @return the gravatar URL.
     */
    private static String constructGravatarUrl(final String email) {
        return GRAVATAR_URL + DigestUtils.md5Hex(email.toLowerCase().trim()) + ".jpg?s=32&d=mm&r=g";
    }

}
