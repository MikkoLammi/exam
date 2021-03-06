package backend.controllers.iop.collaboration.impl;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import com.fasterxml.jackson.databind.JsonNode;
import io.ebean.Ebean;
import io.ebean.text.PathProperties;
import org.joda.time.DateTime;
import play.Logger;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Result;

import backend.impl.EmailComposer;
import backend.models.Exam;
import backend.models.ExamEnrolment;
import backend.models.ExamExecutionType;
import backend.models.User;
import backend.models.json.CollaborativeExam;
import backend.util.DateTimeUtils;

public class CollaborativeEnrolmentController extends CollaborationController {

    @Inject
    private EmailComposer composer;

    private boolean isEnrollable(Exam exam) {
        return exam.getState() == Exam.State.PUBLISHED &&
                exam.getExecutionType().getType().equals(ExamExecutionType.Type.PUBLIC.toString()) &&
                exam.getExamActiveEndDate().isAfterNow() &&
                exam.getExamActiveStartDate().isBeforeNow();
    }


    @Restrict({@Group("STUDENT")})
    public CompletionStage<Result> listExams() {
        Optional<URL> url = parseUrl();
        if (!url.isPresent()) {
            return wrapAsPromise(internalServerError());
        }

        WSRequest request = wsClient.url(url.get().toString());
        Function<WSResponse, Result> onSuccess = response -> {
            JsonNode root = response.asJson();
            if (response.getStatus() != OK) {
                return internalServerError(root.get("message").asText("Connection refused"));
            }

            Map<String, CollaborativeExam> locals = Ebean.find(CollaborativeExam.class).findSet().stream()
                    .collect(Collectors.toMap(CollaborativeExam::getExternalRef, Function.identity()));

            updateLocalReferences(root, locals);

            Map<CollaborativeExam, JsonNode> localToExternal = StreamSupport.stream(root.spliterator(), false)
                    .collect(Collectors.toMap(node -> locals.get(node.get("_id").asText()), Function.identity()));

            List<Exam> exams = localToExternal.entrySet().stream().map(e -> e.getKey().getExam(e.getValue()))
                    .filter(this::isEnrollable).collect(Collectors.toList());

            return ok(exams, PathProperties.parse(
                    "(examOwners(firstName, lastName), examInspections(user(firstName, lastName))" +
                            "examLanguages(code, name), id, name, examActiveStartDate, examActiveEndDate, " +
                            "enrollInstruction)"));
        };
        return request.get().thenApplyAsync(onSuccess);
    }

    @Restrict({@Group("STUDENT")})
    public CompletionStage<Result> checkIfEnrolled(Long id) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        User user = getLoggedUser();
        return downloadExam(ce).thenApplyAsync(
                result -> {
                    if (!result.isPresent()) {
                        return notFound("sitnet_error_exam_not_found");
                    }
                    Exam exam = result.get();
                    if (!isEnrollable(exam)) {
                        return notFound("sitnet_error_exam_not_found");
                    }
                    if (isAllowedToParticipate(exam, user, composer)) {
                        DateTime now = DateTimeUtils.adjustDST(new DateTime());
                        List<ExamEnrolment> enrolments = Ebean.find(ExamEnrolment.class)
                                .where()
                                .eq("user", user)
                                .eq("collaborativeExam.id", id)
                                .disjunction()
                                .gt("reservation.endAt", now.toDate())
                                .isNull("reservation")
                                .endJunction()
                                .or()
                                .isNull("exam")
                                .eq("exam.state", Exam.State.STUDENT_STARTED)
                                .endOr()
                                .findList();

                        if (enrolments.isEmpty()) {
                            return notFound("error not found");
                        }
                        return ok(enrolments);
                    }
                    return forbidden("sitnet_no_trials_left");
                }
        );
    }

    private static ExamEnrolment makeEnrolment(CollaborativeExam exam, User user) {
        ExamEnrolment enrolment = new ExamEnrolment();
        enrolment.setEnrolledOn(DateTime.now());
        enrolment.setUser(user);
        enrolment.setCollaborativeExam(exam);
        enrolment.save();
        return enrolment;
    }

    private Optional<Result> handleFutureReservations(List<ExamEnrolment> enrolments, User user, CollaborativeExam ce) {
        List<ExamEnrolment> enrolmentsWithFutureReservations = enrolments.stream()
                .filter(ee -> ee.getReservation().toInterval().isAfterNow())
                .collect(Collectors.toList());
        if (enrolmentsWithFutureReservations.size() > 1) {
            Logger.error("Several enrolments with future reservations found for user {} and collab exam {}",
                    user, ce.getId());
            return Optional.of(internalServerError()); // Lets fail right here
        }
        // reservation in the future, replace it
        if (!enrolmentsWithFutureReservations.isEmpty()) {
            enrolmentsWithFutureReservations.get(0).delete();
            ExamEnrolment newEnrolment = makeEnrolment(ce, user);
            return Optional.of(ok(newEnrolment));
        }
        return Optional.empty();
    }

    private Result doCreateEnrolment(CollaborativeExam ce, Exam exam, User user) {
        // Begin manual transaction
        Ebean.beginTransaction();
        try {
            // Take pessimistic lock for user to prevent multiple enrolments creating.
            Ebean.find(User.class).forUpdate().where().eq("id", user.getId()).findOne();

            List<ExamEnrolment> enrolments = Ebean.find(ExamEnrolment.class)
                    .fetch("reservation")
                    .where()
                    .eq("user.id", user.getId())
                    .eq("collaborativeExam.id", ce.getId())
                    .findList();
            // already enrolled
            if (enrolments.stream().anyMatch(e -> e.getReservation() == null)) {
                return forbidden("sitnet_error_enrolment_exists");
            }
            // reservation in effect
            if (enrolments.stream().map(ExamEnrolment::getReservation).anyMatch(r ->
                    r.toInterval().contains(DateTimeUtils.adjustDST(DateTime.now(), r)))) {
                return forbidden("sitnet_reservation_in_effect");
            }
            return handleFutureReservations(enrolments, user, ce).orElseGet(() -> {
                ExamEnrolment newEnrolment = makeEnrolment(ce, user);
                Ebean.commitTransaction();
                return ok(newEnrolment);
            });
        } finally {
            // End transaction to release lock.
            Ebean.endTransaction();
        }
    }

    @Restrict({@Group("ADMIN"), @Group("STUDENT")})
    public CompletionStage<Result> createEnrolment(Long id) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        User user = getLoggedUser();
        return downloadExam(ce).thenApplyAsync(
                result -> {
                    if (!result.isPresent()) {
                        return notFound("sitnet_error_exam_not_found");
                    }
                    Exam exam = result.get();
                    if (!isEnrollable(exam)) {
                        return notFound("sitnet_error_exam_not_found");
                    }
                    if (isAllowedToParticipate(exam, user, composer)) {
                        return doCreateEnrolment(ce, exam, user);
                    }
                    return forbidden();
                });

    }

}
