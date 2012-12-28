package org.openl.rules.ruleservice.core;

import java.util.UUID;

import org.openl.rules.common.CommonVersion;

public class DeploymentDescription {
    private final String name;
    private final CommonVersion version;
    private final UUID uuid;

    public DeploymentDescription(String name, CommonVersion version) {
        this.name = name;
        this.version = version;
        this.uuid = UUID.randomUUID();
    }

    public String getName() {
        return name;
    }

    public CommonVersion getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeploymentDescription other = (DeploymentDescription) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }
 }
