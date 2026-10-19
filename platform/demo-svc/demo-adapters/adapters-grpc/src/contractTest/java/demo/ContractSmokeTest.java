package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import demo.v1.GreetRequest;
import demo.v1.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;

/** P0-F4-S1 AC: gRPC codegen produces usable stubs. */
class ContractSmokeTest {

    @Test
    void stubsGenerateAndRoundtrip() {
        GreetingServiceGrpc.GreetingServiceImplBase stub =
            new GreetingServiceGrpc.GreetingServiceImplBase() {};
        assertNotNull(stub);

        GreetRequest request = GreetRequest.newBuilder().setName("kadir").build();
        assertEquals("kadir", request.getName());
    }
}
