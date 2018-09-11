package com.quorum.tessera.config;

import com.quorum.tessera.config.constraints.ValidSsl;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(factoryMethod = "create")
public class ServerConfig extends ConfigItem {

    @NotNull
    @XmlElement(required = true)
    private final String hostName;

    @NotNull
    @XmlElement
    private final Integer port;

    @XmlElement
    private final CommunicationType communicationType;

    @Valid
    @XmlElement
    @ValidSsl
    private final SslConfig sslConfig;

    @Valid
    @XmlElement
    private final InfluxConfig influxConfig;

    @XmlElement
    private final String bindingAddress;

    public ServerConfig(final String hostName,
                        final Integer port,
                        final CommunicationType communicationType,
                        final SslConfig sslConfig,
                        final InfluxConfig influxConfig,
                        final String bindingAddress) {
        this.hostName = hostName;
        this.port = port;
        this.communicationType = communicationType;
        this.sslConfig = sslConfig;
        this.influxConfig = influxConfig;
        this.bindingAddress = bindingAddress;
    }

    private static ServerConfig create() {
        return new ServerConfig(null, null, CommunicationType.REST, null, null, null);
    }

    public String getHostName() {
        return hostName;
    }

    public Integer getPort() {
        return port;
    }

    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    public SslConfig getSslConfig() {
        return sslConfig;
    }

    public InfluxConfig getInfluxConfig() {
        return influxConfig;
    }

    public URI getServerUri() {
        try {
            return new URI(hostName + ":" + port);
        } catch (URISyntaxException ex) {
            throw new ConfigException(ex);
        }
    }

    public boolean isSsl() {
        return Objects.nonNull(sslConfig) && sslConfig.getTls() == SslAuthenticationMode.STRICT;
    }

    public String getBindingAddress() {
        return this.bindingAddress==null ? this.getServerUri().toString() : this.bindingAddress;
    }

}
