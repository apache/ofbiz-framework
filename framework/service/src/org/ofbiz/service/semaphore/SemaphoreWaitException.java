package org.ofbiz.service.semaphore;

import org.ofbiz.service.GenericServiceException;

/**
 * SemaphoreWaitException
 */
public class SemaphoreWaitException extends GenericServiceException {

    public SemaphoreWaitException() {
        super();
    }

    public SemaphoreWaitException(String str) {
        super(str);
    }

    public SemaphoreWaitException(String str, Throwable nested) {
        super(str, nested);
    }

    public SemaphoreWaitException(Throwable nested) {
        super(nested);
    }
}
