package org.apache.ofbiz.webapp.control;

class MethodNotAllowedException extends RequestHandlerException {
    private static final long serialVersionUID = -8765719278480440687L;

    MethodNotAllowedException(String str) {
        super(str);
    }
}
