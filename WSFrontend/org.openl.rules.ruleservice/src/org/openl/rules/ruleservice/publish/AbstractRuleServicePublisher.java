package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Collection;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;

public abstract class AbstractRuleServicePublisher implements RuleServicePublisher {

    protected Collection<RuleServicePublisherListener> listeners = new ArrayList<RuleServicePublisherListener>();

    protected void fireDeployListeners(OpenLService service) {
        for (RuleServicePublisherListener listener : listeners) {
            listener.onDeploy(service);
        }
    }

    protected void fireUndeployListeners(String serviceName) {
        for (RuleServicePublisherListener listener : listeners) {
            listener.onUndeploy(serviceName);
        }
    }

    public void setListeners(Collection<RuleServicePublisherListener> listeners) {
        this.listeners = listeners;
    }
    
    public Collection<RuleServicePublisherListener> getListeners() {
        return listeners;
    }
    
    @Override
    public void addListener(RuleServicePublisherListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void removeListener(RuleServicePublisherListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    protected abstract void deployService(OpenLService service) throws RuleServiceDeployException;

    @Override
    public final void deploy(OpenLService service) throws RuleServiceDeployException {
        deployService(service);
        fireDeployListeners(service);
    }

    protected abstract void undeployService(String serviceName) throws RuleServiceUndeployException;

    @Override
    public final void undeploy(String serviceName) throws RuleServiceUndeployException {
        undeployService(serviceName);
        fireUndeployListeners(serviceName);
    }
}
