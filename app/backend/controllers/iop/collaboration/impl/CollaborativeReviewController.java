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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.inject.Inject;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigFactory;
import io.ebean.Ebean;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Result;

import backend.controllers.base.BaseController;
import backend.controllers.iop.collaboration.api.CollaborativeExamLoader;
import backend.models.json.CollaborativeExam;

public class CollaborativeReviewController extends BaseController {

    @Inject
    CollaborativeExamLoader examLoader;

    @Inject
    WSClient wsClient;

    private Optional<URL> parseUrl(String examRef, String assessmentRef) {
        String url = String.format("%s/api/exams/%s/assessments",
                ConfigFactory.load().getString("sitnet.integration.iop.host"), examRef);
        if (assessmentRef != null) {
            url += String.format("/%s", assessmentRef);
        }
        try {
            return Optional.of(new URL(url));
        } catch (MalformedURLException e) {
            Logger.error("Malformed URL {}", e);
            return Optional.empty();
        }
    }

    private Result handleRemoteResponse(WSResponse response) {
        JsonNode root = response.asJson();
        if (response.getStatus() != OK) {
            return internalServerError(root.get("message").asText("Connection refused"));
        }
        return ok(root);
    }

    private Stream<JsonNode> stream(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false);
    }

    private void scoreAnswer(JsonNode examNode, Long qid, Double score) {
        stream(examNode.get("examSections"))
                .flatMap(es -> stream(es.get("sectionQuestions")))
                .filter(esq -> esq.get("id").asLong() == qid)
                .findAny()
                .ifPresent(esq -> {
                    JsonNode essayAnswer = esq.get("essayAnswer");
                    if (essayAnswer.isObject()) {
                        ((ObjectNode) essayAnswer).put("evaluatedScore", score);
                    } else {
                        ((ObjectNode) essayAnswer).set("essayAnswer",
                                Json.newObject().put("evaluatedScore", score));
                    }
                });
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public CompletionStage<Result> listAssessments(Long id) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        Optional<URL> url = parseUrl(ce.getExternalRef(), null);
        if (!url.isPresent()) {
            return wrapAsPromise(internalServerError());
        }
        WSRequest request = wsClient.url(url.get().toString());
        return request.get().thenApplyAsync(this::handleRemoteResponse);
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public CompletionStage<Result> getAssessment(Long id, String ref) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        Optional<URL> url = parseUrl(ce.getExternalRef(), ref);
        if (!url.isPresent()) {
            return wrapAsPromise(internalServerError());
        }
        WSRequest request = wsClient.url(url.get().toString());
        return request.get().thenApplyAsync(this::handleRemoteResponse);
    }

    private CompletionStage<Result> upload(URL url, JsonNode payload) {
        WSRequest request = wsClient.url(url.toString());
        Function<WSResponse, Result> onSuccess = response -> {
            if (response.getStatus() != OK) {
                JsonNode root = response.asJson();
                return internalServerError(root.get("message").asText());
            }
            return ok(Json.newObject().put("rev", response.asJson().get("rev").textValue()));
        };
        return request.put(payload).thenApplyAsync(onSuccess);
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public CompletionStage<Result> updateAnswerScore(Long id, String ref, Long qid) {
        CollaborativeExam ce = Ebean.find(CollaborativeExam.class, id);
        if (ce == null) {
            return wrapAsPromise(notFound("sitnet_error_exam_not_found"));
        }
        Optional<URL> url = parseUrl(ce.getExternalRef(), ref);
        if (!url.isPresent()) {
            return wrapAsPromise(internalServerError());
        }
        JsonNode body = request().body().asJson();
        String input = body.get("evaluatedScore").asText();
        Double score = input == null ? null : Double.parseDouble(input);
        String revision = body.get("rev").asText();
        WSRequest request = wsClient.url(url.get().toString());
        Function<WSResponse, CompletionStage<Result>> onSuccess = (response) -> {
            JsonNode root = response.asJson();
            if (response.getStatus() != OK) {
                return wrapAsPromise(internalServerError(root.get("message").asText("Connection refused")));
            }
            scoreAnswer(root.get("exam"), qid, score);
            ((ObjectNode) root).put("rev", revision);
            return upload(url.get(), root);
        };
        return request.get().thenComposeAsync(onSuccess);


    }

}
