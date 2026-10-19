package violation.application;

/** Deliberate violation: application layer must not depend on adapters. */
public class BadService {
    public void callOut() {
        new violation.adapters.AdapterSink().push();
    }
}
