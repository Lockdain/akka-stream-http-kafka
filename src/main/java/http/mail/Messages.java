package http.mail;

import http.dto.Greeting;

import java.io.Serializable;

public interface Messages {

    class Greeter implements Serializable {

        private static final long serialVersionUID = 1L;
        private Greeting greeting;

        public Greeter(Greeting greeting) {
            this.greeting = greeting;
        }

        public Greeting getGreeting() {
            return greeting;
        }
    }

    class WriteGreeting implements Serializable {

        private static final long serialVersionUID = 1L;
        private String greeting;

        public WriteGreeting(String greeting) {
            this.greeting = greeting;
        }

        public String getGreeting() {
            return greeting;
        }
    }
}
