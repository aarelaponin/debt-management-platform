package com.fiscaladmin.mtca.cmbb;

import java.util.ArrayList;
import java.util.Collection;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * OSGi Bundle Activator for the CMBB plugin bundle.
 * Registers every CMBB engine plugin as an OSGi service (GAM Activator
 * pattern — proven on Joget 9.0.7). AllocationEngine / DeadlineEngine
 * join this list in CMBB-S2.
 */
public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();
        registrationList.add(context.registerService(
                TransitionGuard.class.getName(), new TransitionGuard(), null));
        registrationList.add(context.registerService(
                AllocationEngine.class.getName(), new AllocationEngine(), null));
        registrationList.add(context.registerService(
                DeadlineEngine.class.getName(), new DeadlineEngine(), null));
        registrationList.add(context.registerService(
                NotificationDispatcher.class.getName(), new NotificationDispatcher(), null));
        registrationList.add(context.registerService(
                MayanConnector.class.getName(), new MayanConnector(), null));
        registrationList.add(context.registerService(
                HoldConnector.class.getName(), new HoldConnector(), null));
        registrationList.add(context.registerService(
                DecisionEngine.class.getName(), new DecisionEngine(), null));
        registrationList.add(context.registerService(
                EventEmitter.class.getName(), new EventEmitter(), null));
        registrationList.add(context.registerService(
                OutcomeWriteback.class.getName(), new OutcomeWriteback(), null));
        registrationList.add(context.registerService(
                DebtIdentificationJob.class.getName(), new DebtIdentificationJob(), null));
        registrationList.add(context.registerService(
                EscalationEngine.class.getName(), new EscalationEngine(), null));
        registrationList.add(context.registerService(
                ReliefProductInterpreter.class.getName(), new ReliefProductInterpreter(), null));
        registrationList.add(context.registerService(
                PaymentEngine.class.getName(), new PaymentEngine(), null));
        // CaseContextLoadBinder retired — replaced by the project-neutral
        // com.fiscaladmin.joget.formprefill.FormPrefillLoadBinder (separate bundle).
        registrationList.add(context.registerService(
                EnforcementActionEngine.class.getName(), new EnforcementActionEngine(), null));
        registrationList.add(context.registerService(
                EnforcementConfigEngine.class.getName(), new EnforcementConfigEngine(), null));
        registrationList.add(context.registerService(
                WriteOffEngine.class.getName(), new WriteOffEngine(), null));
        registrationList.add(context.registerService(
                DefaultAssessmentEngine.class.getName(), new DefaultAssessmentEngine(), null));
        registrationList.add(context.registerService(
                DebtorsListEngine.class.getName(), new DebtorsListEngine(), null));
        registrationList.add(context.registerService(
                CollectionMiEngine.class.getName(), new CollectionMiEngine(), null));
        registrationList.add(context.registerService(
                ApprovalGateEngine.class.getName(), new ApprovalGateEngine(), null));
        registrationList.add(context.registerService(
                ApprovalSweepEngine.class.getName(), new ApprovalSweepEngine(), null));
        registrationList.add(context.registerService(
                ApprovalDelegateEngine.class.getName(), new ApprovalDelegateEngine(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}
