package com.nepxion.discovery.platform.server.ldap.configuration;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 *
 * @author Ning Zhang
 * @version 1.0
 */

import com.nepxion.discovery.platform.server.adapter.PlatformLoginAdapter;
import com.nepxion.discovery.platform.server.ldap.adapter.PlatformLdapLoginAdapter;
import com.nepxion.discovery.platform.server.ldap.constant.PlatformLdapConstant;
import com.nepxion.discovery.platform.server.ldap.properties.PlatformLdapProperties;
import com.nepxion.discovery.platform.server.ldap.service.LdapService;
import com.nepxion.banner.BannerConstant;
import com.nepxion.banner.Description;
import com.nepxion.banner.LogoBanner;
import com.nepxion.banner.NepxionBanner;
import com.taobao.text.Color;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties({PlatformLdapProperties.class})
public class PlatformLdapAutoConfiguration {
    static {
        LogoBanner logoBanner = new LogoBanner(PlatformLdapAutoConfiguration.class, "/com/nepxion/ldap/resource/logo.txt", "Welcome to Nepxion", 4, 5, new Color[] { Color.red, Color.green, Color.cyan, Color.blue }, true);
        NepxionBanner.show(logoBanner, new Description("Plugin:", PlatformLdapConstant.LDAP_TYPE, 0, 1), new Description(BannerConstant.GITHUB + ":", BannerConstant.NEPXION_GITHUB + "/Discovery", 0, 1));
    }

    private final LdapProperties ldapProperties;
    private final Environment environment;
    private final PlatformLdapProperties platformLdapProperties;

    public PlatformLdapAutoConfiguration(final LdapProperties ldapProperties,
                                         final Environment environment,
                                         final PlatformLdapProperties platformLdapProperties) {
        this.ldapProperties = ldapProperties;
        this.environment = environment;
        this.platformLdapProperties = platformLdapProperties;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public LdapContextSource contextSource() {
        final LdapContextSource contextSource = new LdapContextSource();
        final Map<String, Object> config = new HashMap<>();
        config.put("java.naming.ldap.attributes.binary", "objectGUID");
        contextSource.setCacheEnvironmentProperties(false);
        contextSource.setUrls(this.ldapProperties.determineUrls(this.environment));
        contextSource.setUserDn(this.ldapProperties.getUsername());
        contextSource.setPassword(this.ldapProperties.getPassword());
        contextSource.setPooled(true);
        contextSource.setBaseEnvironmentProperties(config);
        return contextSource;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public LdapTemplate ldapTemplate(final ContextSource contextSource) {
        final LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        ldapTemplate.setIgnorePartialResultException(true);
        return ldapTemplate;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public LdapService ldapService(final LdapTemplate ldapTemplate,
                                   final LdapProperties ldapProperties) {
        return new LdapService(ldapTemplate,
                ldapProperties,
                this.platformLdapProperties);
    }

    @Bean
    public PlatformLoginAdapter platformLoginAdapter() {
        return new PlatformLdapLoginAdapter();
    }
}