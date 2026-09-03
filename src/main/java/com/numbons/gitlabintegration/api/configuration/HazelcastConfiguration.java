package com.numbons.gitlabintegration.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;

/**
 * Explicit Hazelcast configuration.
 *
 * This instance is only ever used as a local, single-node cache
 * (see CacheServiceImpl) - it does not need to cluster with other
 * members. Previously no Config bean was registered anywhere, so
 * Hazelcast fell back to Spring Boot's default auto-configuration
 * with multicast discovery enabled. In this Kubernetes environment
 * that let Hazelcast's member protocol end up exposed on/near the
 * same network surface as the app's HTTP port (8080), so plain HTTP
 * requests intended for the REST API were occasionally landing on
 * Hazelcast's socket, which does not understand HTTP and rejects the
 * connection ("Unknown protocol: HTT").
 *
 * Fix: pin Hazelcast to its own explicit port, disable every discovery
 * mechanism (multicast / Kubernetes / TCP-IP), and bind only to the
 * loopback interface so it can never be reached from outside the pod.
 *
 * IMPORTANT: this bean must have no dependency (directly or
 * transitively) on CacheService/CacheServiceImpl. HazelcastInstance is
 * built from this Config bean, and CacheServiceImpl depends on
 * HazelcastInstance - so if this class also depended on CacheService
 * (e.g. by living inside CacheConfiguration), it would create a
 * circular bean dependency at startup. Keep this class standalone.
 */
@Configuration
public class HazelcastConfiguration {

    @Value("${hazelcast.port:5701}")
    private int hazelcastPort;

    @Bean
    public Config hazelcastConfig() {
        Config config = new Config();
        config.setInstanceName("service-gitlabintegration-instance");

        // Never let Hazelcast try to discover or join other members -
        // this is a single, local, in-memory cache only.
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getKubernetesConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(false);
        joinConfig.getAutoDetectionConfig().setEnabled(false);

        NetworkConfig networkConfig = config.getNetworkConfig();
        // Explicit, non-8080 port. Disable auto-increment so that if this
        // port is ever unavailable the app fails fast at startup instead
        // of silently drifting onto a different (possibly conflicting) port.
        networkConfig.setPort(hazelcastPort);
        networkConfig.setPortAutoIncrement(false);

        // Restrict Hazelcast to the loopback interface only - it should
        // never be reachable from outside this pod/container.
        networkConfig.getInterfaces().setEnabled(true).addInterface("127.0.0.1");

        return config;
    }

}