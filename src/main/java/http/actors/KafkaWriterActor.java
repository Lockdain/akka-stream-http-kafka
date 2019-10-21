package http.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.FI;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.*;
import com.typesafe.config.Config;
import http.mail.Messages;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaWriterActor extends AbstractActor {
    private final static String ACTOR_NAME = "KafkaWriterActor";
    private final ActorSystem ACTOR_SYSTEM;
    private SourceQueueWithComplete<String> SOURCE;
    private final Materializer materializer;
    private final ProducerSettings<String, String> PRODUCER_SETTINGS;
    private final Config CONFIG;

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(KafkaWriterActor.class);
    }

    public KafkaWriterActor() {
        ACTOR_SYSTEM = context().system();
        materializer = ActorMaterializer.create(ACTOR_SYSTEM);
        CONFIG = ACTOR_SYSTEM.settings().config().getConfig("akka.kafka.producer");
        PRODUCER_SETTINGS = ProducerSettings
                .create(CONFIG, new StringSerializer(), new StringSerializer()).withBootstrapServers("localhost:9092");

        SOURCE = Source.<String>queue(500, OverflowStrategy.backpressure())
                .map(event -> {
                    ProducerMessage.Envelope<String, String, String> msg =
                            ProducerMessage.single(new ProducerRecord<>("greetings_topic", event), event);
                    return msg;
                })
                .log("Kafka writer log")
                .via(Producer.flexiFlow(PRODUCER_SETTINGS))
                .to(Sink.foreach(x -> log.debug("Event was processed to Kafka!")))
                .run(materializer);
    }

    @Override
    public void preStart() {
        log.debug("Starting actor: " + ACTOR_NAME);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Messages.WriteGreeting.class, offerEventToKafkaQueue())
                .build();
    }

    private FI.UnitApply<Messages.WriteGreeting> offerEventToKafkaQueue() {
        return writeEvent -> {
            SOURCE.offer(writeEvent.getGreeting());
        };
    }
}
