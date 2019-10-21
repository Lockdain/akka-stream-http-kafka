package http.router;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import http.actors.GreeterActor;
import http.mail.Messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes requests with CustomerProfile to the instances
 * of CustomerProfileActor.
 */
public class GreeterActorRouter extends AbstractActor {
    private final static String ACTOR_NAME = "CustomerProfileRouter";
    private Router router;

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(GreeterActorRouter.class);
    }

    {
        List<Routee> routees = new ArrayList<Routee>(10);
        for(int i = 0; i < 5; i++) {
            ActorRef actorRef = getContext().actorOf(Props.create(GreeterActor.class));
            getContext().watch(actorRef); // watch the routee to replace it in case of termination
            routees.add(new ActorRefRoutee(actorRef));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public void preStart() throws Exception {
        log.debug("Starting actor: " + ACTOR_NAME);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Messages.Greeter.class, message -> {
                    router.route(message, getSender());
                })
                .build();
    }
}
