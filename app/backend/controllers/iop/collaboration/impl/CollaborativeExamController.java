/*
 * Copyright (c) 2018 The members of the EXAM Consortium (https://confluence.csc.fi/display/EXAM/Konsortio-organisaatio)
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed
 * on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package backend.controllers.iop.collaboration.impl;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import com.fasterxml.jackson.databind.JsonNode;
import io.ebean.Ebean;
import org.joda.time.DateTime;
import play.libs.Json;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import backend.models.Exam;
import backend.models.ExamExecutionType;
import backend.models.ExamSection;
import backend.models.ExamType;
import backend.models.GradeScale;
import backend.models.Language;
import backend.models.Role;
import backend.models.User;
import backend.models.json.CollaborativeExam;
import backend.sanitizers.Attrs;
import backend.sanitizers.EmailSanitizer;
import backend.sanitizers.ExamUpdateSanitizer;
import backend.util.AppUtil;
import backend.util.ConfigUtil;

public class CollaborativeExamController extends CollaborationController {

    private Exam prepareDraft() {
        ExamExecutionType examExecutionType = Ebean.find(ExamExecutionType.class)
                .where()
                .eq("type", ExamExecutionType.Type.PUBLIC.toString())
                .findOne();
        User user = getLoggedUser();
        Exam exam = new Exam();
        exam.generateHash();
        exam.setState(Exam.State.DRAFT);
        exam.setExecutionType(examExecutionType);
        AppUtil.setCreator(exam, user);

        ExamSection examSection = new ExamSection();
        AppUtil.setCreator(examSection, user);

        examSection.setId(newId());
        examSection.setExam(exam);
        examSection.setExpanded(true);
        examSection.setSequenceNumber(0);

        exam.getExamSections().add(examSection);
        exam.getExamLanguages().add(Ebean.find(Language.class, "fi"));
        exam.setExamType(Ebean.find(ExamType.class, 2)); // Final

        DateTime start = DateTime.now().withTimeAtStartOfDay();
        exam.setExamActiveStartDate(start);
        exam.setExamActiveEndDate(start.plusDays(1));
        exam.setDuration(ConfigUtil.getExamDurations().get(0)); // check
        exam.setGradeScale(Ebean.find(GradeScale.class).findList().get(0)); // check

        exam.setTrialCount(1);
        exam.setExpanded(true);
        exam.setAnonymous(true);

        return exam;
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public CompletionStage<Result> listExams() {
        Optional<URL> url = parseUrl();
        if (!url.isPresent()) {
            return wrapAsPromise(internalServerError());
        }
        User user = getLoggedUser();
        Role.Name loginRole = Role.Name.valueOf(getSession().getLoginRole());

        WSRequest request = wsClient.url(url.get().toString());
        Function<WSResponse, Result> onSuccess = response -> {
            JsonNode root = response.asJson();
            if (response.getStatus() != OK) {
                return internalServerError(root.get("message").asText("Connection refused"));
            }

            Map<String, CollaborativeExam> locals = Ebean.find(CollaborativeExam.class).findSet().stream()
                    .collect(Collectors.toMap(CollaborativeExam::getExternalRef, Function.identity()));

            // Save references to documents that we don't have locally yet
            updateLocalReferences(root, locals);

            Map<CollaborativeExam, JsonNode> localToExternal = StreamSupport.stream(root.spliterator(), false)
                    .collect(Collectors.toMap(node -> locals.get(node.get("_id").asText()), Function.identity()));
            List<JsonNode> exams = localToExternal.entrySet().stream()
                    .map(e -> e.getKey().getExam(e.getValue()))
                    .filter(e -> isAuthorizedToView(e, user, loginRole))
                    .map(this::serialize)
                    .collect(Collectors.toList());

            return ok(Json.newArray().addAll(exams));
        };
        return request.get().thenApplyAsync(onSuccess);
    }

    private CompletionStage<Result> getExam(Long id, Consumer<Exam> postProcessor) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        User user = getLoggedUser();
        Role.Name loginRole = Role.Name.valueOf(getSession().getLoginRole());
        return downloadExam(ce).thenApplyAsync(
                result -> {
                    if (!result.isPresent()) {
                        return notFound("sitnet_error_exam_not_found");
                    }
                    Exam exam = result.get();
                    if (!isAuthorizedToView(exam, user, loginRole)) {
                        return notFound("sitnet_error_exam_not_found");
                    }
                    postProcessor.accept(exam);
                    return ok(serialize(exam));
                }
        );
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public CompletionStage<Result> getExam(Long id) {
        return getExam(id, exam -> {});
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public CompletionStage<Result> getExamPreview(Long id) {
        return getExam(id, exam -> examUpdater.preparePreview(exam));
    }

    @Restrict({@Group("ADMIN")})
    public CompletionStage<Result> createExam() {
        Optional<URL> url = parseUrl();
        if (!url.isPresent()) {
            return wrapAsPromise(internalServerError());
        }
        WSRequest request = wsClient.url(url.get().toString());
        Function<WSResponse, Result> onSuccess = response -> {
            JsonNode root = response.asJson();
            if (response.getStatus() != CREATED) {
                return internalServerError(root.get("message").asText("Connection refused"));
            }
            String externalRef = root.get("id").asText();
            String revision = root.get("rev").asText();
            CollaborativeExam ce = new CollaborativeExam();
            ce.setExternalRef(externalRef);
            ce.setRevision(revision);
            ce.setCreated(DateTime.now());
            ce.setAnonymous(true);
            ce.save();
            return created(Json.newObject().put("id", ce.getId()));
        };
        Exam body = prepareDraft();
        return request.post(serialize(body)).thenApplyAsync(onSuccess);
    }

    @Restrict({@Group("ADMIN")})
    public CompletionStage<Result> deleteExam(Long id) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        if (!ce.getState().equals(Exam.State.DRAFT) && !ce.getState().equals(Exam.State.PRE_PUBLISHED)) {
            return wrapAsPromise(forbidden("sitnet_exam_removal_not_possible"));
        }
        return examLoader.deleteExam(ce)
                .thenApplyAsync(result -> {
                    if (result.status() == Http.Status.OK) {
                        ce.delete();
                    }
                    return result;
                });
    }

    @With(ExamUpdateSanitizer.class)
    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public CompletionStage<Result> updateExam(Long id) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        User user = getLoggedUser();
        Role.Name loginRole = Role.Name.valueOf(getSession().getLoginRole());
        return downloadExam(ce).thenComposeAsync(result -> {
            if (result.isPresent()) {
                Exam exam = result.get();
                if (isAuthorizedToView(exam, user, loginRole)) {
                    Exam.State previousState = exam.getState();
                    Optional<Result> error = Stream.of(
                            examUpdater.updateTemporalFieldsAndValidate(exam, user, request(), getSession()),
                            examUpdater.updateStateAndValidate(exam, user, request()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .findFirst();
                    if (error.isPresent()) {
                        return wrapAsPromise(error.get());
                    }
                    Exam.State nextState = exam.getState();
                    boolean isPrePublication =
                            previousState != Exam.State.PRE_PUBLISHED && nextState == Exam.State.PRE_PUBLISHED;
                    examUpdater.update(exam, request(), loginRole);
                    return uploadExam(ce, exam, isPrePublication, null, user);
                }
                return wrapAsPromise(forbidden("sitnet_error_access_forbidden"));
            }
            return wrapAsPromise(notFound());
        }, ec.current());
    }

    @Restrict({@Group("ADMIN")})
    public CompletionStage<Result> updateLanguage(Long id, String code) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        final Role.Name loginRole = Role.Name.valueOf(getSession().getLoginRole());
        return downloadExam(ce).thenComposeAsync(result -> {
            if (result.isPresent()) {
                Exam exam = result.get();
                Optional<Result> error = examUpdater.updateLanguage(exam, code, getLoggedUser(), getSession());
                examUpdater.update(exam, request(), loginRole);
                return error.isPresent() ? wrapAsPromise(error.get()) : uploadExam(ce, exam, false,
                        null, getLoggedUser());
            }
            return wrapAsPromise(notFound());
        }, ec.current());
    }

    @With(EmailSanitizer.class)
    @Restrict({@Group("ADMIN")})
    public CompletionStage<Result> addOwner(Long id) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        final Role.Name loginRole = Role.Name.valueOf(getSession().getLoginRole());
        return downloadExam(ce).thenComposeAsync(result -> {
            if (result.isPresent()) {
                Exam exam = result.get();
                User user = createOwner(request().attrs().get(Attrs.EMAIL));
                exam.getExamOwners().add(user);
                examUpdater.update(exam, request(), loginRole);
                return uploadExam(ce, exam, false, user, getLoggedUser());
            }
            return wrapAsPromise(notFound());
        }, ec.current());
    }

    @Restrict({@Group("ADMIN")})
    public CompletionStage<Result> removeOwner(Long id, Long oid) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        final Role.Name loginRole = Role.Name.valueOf(getSession().getLoginRole());
        return downloadExam(ce).thenComposeAsync(result -> {
            if (result.isPresent()) {
                Exam exam = result.get();
                User user = new User();
                user.setId(oid);
                exam.getExamOwners().remove(user);
                examUpdater.update(exam, request(), loginRole);
                return uploadExam(ce, exam, false, null, getLoggedUser());
            }
            return wrapAsPromise(notFound());
        }, ec.current());
    }

    private User createOwner(String email) {
        User user = new User();
        user.setId(newId());
        user.setEmail(email);
        return user;
    }
}
