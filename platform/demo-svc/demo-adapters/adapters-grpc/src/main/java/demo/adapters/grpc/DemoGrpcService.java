package demo.adapters.grpc;

import demo.v1.GreetRequest;
import demo.v1.GreetResponse;
import demo.v1.GreetingServiceGrpc;
import io.grpc.stub.StreamObserver;

/** gRPC adapter: translates proto messages to/from the application layer. */
public class DemoGrpcService extends GreetingServiceGrpc.GreetingServiceImplBase {

    private final demo.application.GreetingService greetingService;

    public DemoGrpcService(demo.application.GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @Override
    public void greet(GreetRequest request, StreamObserver<GreetResponse> responseObserver) {
        GreetResponse response = GreetResponse.newBuilder()
            .setMessage(greetingService.greet(request.getName()).message())
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
