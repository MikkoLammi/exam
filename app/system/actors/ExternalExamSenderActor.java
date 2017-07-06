package system.actors;

import akka.actor.UntypedActor;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.text.PathProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.ConfigFactory;
import models.ExamEnrolment;
import models.json.ExternalExam;
import org.joda.time.DateTime;
import play.Logger;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

public class ExternalExamSenderActor extends UntypedActor {

    private WSClient wsClient;

    @Inject
    public ExternalExamSenderActor(WSClient wsClient) {
        this.wsClient = wsClient;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        Logger.debug("{}: Running external exam sender ...", getClass().getCanonicalName());
        List<ExamEnrolment> enrolments = Ebean.find(ExamEnrolment.class)
                .where()
                .isNotNull("externalExam")
                .isNull("externalExam.sent")
                .isNotNull("externalExam.started")
                .isNotNull("externalExam.finished")
                .isNotNull("reservation.externalRef")
                .findList();
        enrolments.forEach(e -> {
            try {
                send(e);
            } catch (IOException ex) {
                Logger.error("I/O failure while sending exam to XM");
            }
        });
    }

    private void send(ExamEnrolment enrolment) throws IOException {
        URL url = parseUrl(enrolment.getReservation().getExternalRef());
        WSRequest request = wsClient.url(url.toString());
        Function<WSResponse, Void> onSuccess = response -> {
            if (response.getStatus() != 201) {
                Logger.error("Failed in sending the exam to XM");
            } else {
                ExternalExam ee = enrolment.getExternalExam();
                ee.setSent(DateTime.now());
                ee.update();
            }
            return null;
        };
        String json = Ebean.json().toJson(enrolment.getExternalExam(), PathProperties.parse("(*, creator(id))"));
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.readTree(json);
        request.post(node).thenApplyAsync(onSuccess);
    }

    private static URL parseUrl(String reservationRef) throws MalformedURLException {
        return new URL(ConfigFactory.load().getString("sitnet.integration.iop.host") +
                String.format("/api/enrolments/%s/assessment", reservationRef));
    }

}