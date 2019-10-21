package http.server;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.PatternsCS;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import http.actors.KafkaWriterActor;
import http.dto.Greeting;
import http.mail.Messages;
import http.router.GreeterActorRouter;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.PathMatchers.*;

class CustomerServer extends AllDirectives {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final ActorRef greeterActorRouter;
    private final Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));

    CustomerServer(ActorRef greeterActorRouter) {
        this.greeterActorRouter = greeterActorRouter;
    }

    public Route createRoute() {
        return path("writer", this::greetMe)
                .orElse(path(segment("check").slash("health"), () ->
                        route(complete((StatusCodes.OK)))));
    }

    private Route greetMe() {

        return route(post(() -> entity(Jackson.unmarshaller(objectMapper, Greeting.class), greeting -> {
            CompletionStage<String> greetingInvoke = PatternsCS.ask(greeterActorRouter, new Messages.Greeter(greeting), timeout)
                    .thenApply(obj -> (String) obj);

            return onSuccess(() -> greetingInvoke, response -> complete(StatusCodes.OK, response, Jackson.marshaller()));
        })));
    }

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create("server");
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Http http = Http.get(system);
        ActorRef kafkaWriterActor = system.actorOf(KafkaWriterActor.props(), "kafkaWriter"); // creating new actor in the ActorSystem
        ActorRef greeterActorRouter = system.actorOf(GreeterActorRouter.props(), "greeterActorRouter");

        //In order to access all directives we need an instance where the routes are define.
        CustomerServer customerServer = new CustomerServer(greeterActorRouter);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = customerServer.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("0.0.0.0", 8080), materializer);
    }
}