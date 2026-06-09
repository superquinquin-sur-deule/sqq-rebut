package org.superquinquin.odoo;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "odoo")
public interface OdooConfig {

    String url();

    String database();

    String login();

    String password();

    BasicAuth basicAuth();

    Rebut rebut();

    Scrap scrap();

    interface BasicAuth {
        @WithDefault("")
        String user();

        @WithDefault("")
        String password();
    }

    interface Rebut {
        @WithDefault("true")
        boolean dryRun();
    }

    interface Scrap {
        @WithDefault("7")
        int originId();

        @WithDefault("12")
        int locationId();

        @WithDefault("4")
        int scrapLocationId();
    }
}
