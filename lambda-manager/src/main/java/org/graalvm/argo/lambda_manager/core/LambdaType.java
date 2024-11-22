package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.utils.Messages;

/**
 * This enum describes the type of the lambda virtualization technology.
 */
public enum LambdaType {
    // Lambda to be deployed as Firecracker VM.
    VM_FIRECRACKER("VM_FIRECRACKER"),
    // Lambda to be deployed as Firecracker VM with snapshotting.
    VM_FIRECRACKER_SNAPSHOT("VM_FIRECRACKER_SNAPSHOT"),
    // Lambda to be deployed as container.
    CONTAINER("CONTAINER"),
    // Lambda to be deployed as container; in Graalvisor mode users will be collocated.
    CONTAINER_DEBUG("CONTAINER_DEBUG"),
    // Lambda to be deployed as a normal process. Exclusive to GraalOS. Uses the same networking scheme as containers (see NetworkConfigurationUtils#prepareContainerConnectionPool).
    GRAALOS_NATIVE("GRAALOS_NATIVE");

    private final String type;

    LambdaType(String type) {
        this.type = type;
    }

    public boolean isVM() {
        return this.equals(VM_FIRECRACKER) || this.equals(VM_FIRECRACKER_SNAPSHOT);
    }

    public boolean isContainer() {
        return !isVM() && !this.equals(GRAALOS_NATIVE);
    }

    public static LambdaType fromString(String text) throws RuntimeException {
        for (LambdaType b : LambdaType.values()) {
            if (b.type.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new RuntimeException(String.format(Messages.ERROR_LAMBDA_TYPE, text));
    }

    @Override
    public String toString() {
        return this.type;
    }
}
