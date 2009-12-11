package org.ofbiz.base.start;

import java.io.File;
import java.io.IOException;

public interface Instrumenter {
    File getDefaultFile() throws IOException;
    void open(File dataFile, boolean forInstrumenting) throws IOException;
    byte[] instrumentClass(byte[] bytes) throws IOException;
    void close() throws IOException;
}
