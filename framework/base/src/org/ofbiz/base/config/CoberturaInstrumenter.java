package org.ofbiz.base.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;

import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.ProjectData;

import org.ofbiz.base.start.Instrumenter;

public final class CoberturaInstrumenter implements Instrumenter {
    private static final Constructor INSTRUMENTER_CONSTRUCTOR;
    private static final Method IS_INSTRUMENTED_METHOD;
    static {
        try {
            Class<?> clz = CoberturaInstrumenter.class.getClassLoader().loadClass("net.sourceforge.cobertura.instrument.ClassInstrumenter");
            INSTRUMENTER_CONSTRUCTOR = clz.getConstructor(ProjectData.class, ClassVisitor.class, Collection.class, Collection.class);
            INSTRUMENTER_CONSTRUCTOR.setAccessible(true);
            IS_INSTRUMENTED_METHOD = clz.getDeclaredMethod("isInstrumented");
            IS_INSTRUMENTED_METHOD.setAccessible(true);
        } catch (Throwable t) {
            throw (InternalError) new InternalError(t.getMessage()).initCause(t);
        }
    }

    protected File dataFile;
    protected ProjectData projectData;
    protected boolean forInstrumenting;

    public File getDefaultFile() throws IOException {
        return CoverageDataFileHandler.getDefaultDataFile();
    }

    public void open(File dataFile, boolean forInstrumenting) throws IOException {
        System.setProperty("net.sourceforge.cobertura.datafile", dataFile.toString());
        this.forInstrumenting = forInstrumenting;
        this.dataFile = dataFile;
        if (forInstrumenting) {
            if (dataFile.exists()) {
                projectData = CoverageDataFileHandler.loadCoverageData(dataFile);
            } else {
                projectData = new ProjectData();
            }
        }
    }

    public void close() throws IOException {
        if (forInstrumenting) {
            CoverageDataFileHandler.saveCoverageData(projectData, dataFile);
        }
    }

    public byte[] instrumentClass(byte[] bytes) throws IOException {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS/* | ClassWriter.COMPUTE_FRAMES*/);
        try {
            ClassVisitor ci = (ClassVisitor) INSTRUMENTER_CONSTRUCTOR.newInstance(projectData != null ? projectData : ProjectData.getGlobalProjectData(), cw, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
            cr.accept(ci, 0);
            if (((Boolean) IS_INSTRUMENTED_METHOD.invoke(ci)).booleanValue()) return cw.toByteArray();
        } catch (Throwable t) {
            throw (IOException) new IOException(t.getMessage()).initCause(t);
        }
        return bytes;
    }
}
