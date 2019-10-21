package http.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.FI;
import http.dto.Greeting;
import http.mail.Messages;

public class GreeterActor extends AbstractActor {

    // defining actor name
    private final static String ACTOR_NAME = "GreetingActor";
    // obtaining the actor system
    private final ActorSystem ACTOR_SYSTEM = context().system();

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    // Lifecycle method
    public void preStart() throws Exception {
        log.debug("Starting actor: " + ACTOR_NAME);
    }

    public static Props props() {
        return Props.create(GreeterActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Messages.Greeter.class, handleMakeDecision())
                .build();
    }

    private FI.UnitApply<Messages.Greeter> handleMakeDecision() {
        return greeter -> {
            sender().tell(process(greeter.getGreeting()), getSelf());
        };
    }

    public String process(Greeting greeting) {
        String greetingStr = "Hello, " + greeting.getName() + " " + greeting.getSurname();
        ACTOR_SYSTEM.actorFor("/user/kafkaWriter").tell(new Messages.WriteGreeting(greetingStr), ActorRef.noSender());

        return greetingStr;
    }
}