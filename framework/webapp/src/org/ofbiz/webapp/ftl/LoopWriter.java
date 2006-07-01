package org.ofbiz.webapp.ftl;

import java.io.IOException;
import java.io.Writer;

import freemarker.template.TemplateModelException;
import freemarker.template.TransformControl;

public class LoopWriter extends Writer implements TransformControl {

    public LoopWriter(Writer out) {
    }

    public int onStart() throws TemplateModelException, IOException {  
        return TransformControl.EVALUATE_BODY;
    }

    public int afterBody() throws TemplateModelException, IOException {  
        return TransformControl.END_EVALUATION;
    }

    public void onError(Throwable t) throws Throwable {
        throw t;
    }

    public void close() throws IOException {  
    }

    public void write(char cbuf[], int off, int len) {
    }

    public void flush() throws IOException {
    }

}
