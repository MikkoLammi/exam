package controllers.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.typesafe.config.ConfigFactory;
import exceptions.MalformedDataException;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import io.ebean.text.PathProperties;
import models.Exam;
import models.ExamEnrolment;
import models.ExamParticipation;
import models.Reservation;
import models.Session;
import models.User;
import models.api.CountsAsTrial;
import play.cache.SyncCacheApi;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import impl.EmailComposer;
import impl.NoShowHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class BaseController extends Controller {

    public static final String SITNET_CACHE_KEY = "user.session.";

    protected static final int SITNET_TIMEOUT_MINUTES = 30;
    protected static final String LOGIN_TYPE = ConfigFactory.load().getString("sitnet.login");

    private static final int KB = 1024;
    private static final String SITNET_TOKEN_HEADER_KEY = "x-exam-authentication";

    @Inject
    protected SyncCacheApi cache;
    @Inject
    protected FormFactory formFactory;
    @Inject
    private NoShowHandler noShowHandler;

    protected <T> T bindForm(final Class<T> clazz) {
        final Form<T> form = formFactory.form(clazz);
        if (form.hasErrors()) {
            throw new MalformedDataException(form.errorsAsJson().asText());
        }
        return form.bindFromRequest().get();
    }

    protected <E extends Enum<E>> E parseEnum(String fieldName, JsonNode node, Class<E> type) {
        JsonNode field = node.get(fieldName);
        if (field != null && field.isTextual()) {
            return Enum.valueOf(type, node.get(fieldName).asText());
        }
        return null;
    }

    protected <T> T parse(String fieldName, JsonNode node, Class<T> type) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            if (type.equals(Long.class)) {
                return type.cast(field.asLong());
            }
            if (type.equals(Integer.class)) {
                return type.cast(field.asInt());
            }
            if (field.isTextual()) {
                return type.cast(field.asText());
            }
            if (field.isDouble() || type.equals(Double.class)) {
                return type.cast(field.asDouble());
            }
            if (field.isBoolean()) {
                return type.cast(field.asBoolean());
            }
        }
        return null;
    }

    protected <T> T parse(String fieldName, JsonNode node, Class<T> type, T defaultValue) {
        T value = parse(fieldName, node, type);
        return value == null ? defaultValue : value;
    }

    protected Result ok(Object object) {
        String body = Ebean.json().toJson(object);
        return ok(body).as("application/json");
    }

    protected Result ok(Object object, PathProperties props) {
        String body = Ebean.json().toJson(object, props);
        return ok(body).as("application/json");
    }

    protected Result created(Object object, PathProperties props) {
        String body = Ebean.json().toJson(object, props);
        return created(body).as("application/json");
    }

    private Optional<String> createToken() {
        return LOGIN_TYPE.equals("HAKA") ?
                request().header("Shib-Session-ID") :
                Optional.of(UUID.randomUUID().toString());
    }

    private Optional<String> getToken() {
        return request().header(LOGIN_TYPE.equals("HAKA") ? "Shib-Session-ID" : SITNET_TOKEN_HEADER_KEY);
    }

    public static Optional<String> getToken(Http.RequestHeader request) {
        return request.header(LOGIN_TYPE.equals("HAKA") ? "Shib-Session-ID" : SITNET_TOKEN_HEADER_KEY);
    }

    public static Optional<String> getToken(Http.Context context) {
        return getToken(context.request());
    }

    protected User getLoggedUser() {
        Session session = cache.get(SITNET_CACHE_KEY + getToken().orElse(""));
        return Ebean.find(User.class, session.getUserId());
    }

    protected Session getSession() {
        return cache.get(SITNET_CACHE_KEY + getToken().orElse(""));
    }

    protected void updateSession(Session session) {
        cache.set(SITNET_CACHE_KEY + getToken().orElse(""), session);
    }

    protected String createSession(Session session) {
        String token = createToken().orElse("INVALID");
        cache.set(SITNET_CACHE_KEY + token, session);
        return token;
    }

    protected List<String> parseArrayFieldFromBody(String field) {
        DynamicForm df = formFactory.form().bindFromRequest();
        String args = df.get(field);
        String[] array;
        if (args == null || args.isEmpty()) {
            array = new String[]{};
        } else {
            array = args.split(",");
        }
        return Arrays.asList(array);
    }

    protected <T> ExpressionList<T> applyUserFilter(String prefix, ExpressionList<T> query, String filter) {
        ExpressionList<T> result = query;
        String rawFilter = filter.replaceAll(" +", " ").trim();
        String condition = String.format("%%%s%%", rawFilter);
        String fnField = prefix == null ? "firstName" : String.format("%s.firstName", prefix);
        String lnField = prefix == null ? "lastName" : String.format("%s.lastName", prefix);
        if (rawFilter.contains(" ")) {
            // Possible that user provided us two names. Lets try out some combinations of first and last names
            String name1 = rawFilter.split(" ")[0];
            String name2 = rawFilter.split(" ")[1];
            result = result.disjunction().conjunction()
                    .ilike(fnField, String.format("%%%s%%", name1))
                    .ilike(lnField, String.format("%%%s%%", name2))
                    .endJunction().conjunction()
                    .ilike(fnField, String.format("%%%s%%", name2))
                    .ilike(lnField, String.format("%%%s%%", name1))
                    .endJunction().endJunction();
        } else {
            result = result.ilike(fnField, condition)
                    .ilike(lnField, condition);
        }
        return result;
    }

    protected ByteArrayOutputStream setData(File file) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            InputStream fis = new FileInputStream(file);

            byte[] buf = new byte[KB];

            for (int readNum; (readNum = fis.read(buf)) != -1; ) {
                bos.write(buf, 0, readNum);
            }

            fis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return bos;
    }

    private void handleNoShow(User user, Long examId, EmailComposer composer) {

        List<Reservation> reservations = Ebean.find(Reservation.class)
                .fetch("enrolment")
                .fetch("enrolment.exam")
                .where()
                .eq("user", user)
                .eq("noShow", false)
                .lt("endAt", new Date())
                .eq("enrolment.exam.id", examId)
                .eq("enrolment.exam.state", Exam.State.PUBLISHED)
                .isNull("externalReservation")
                .findList();
        noShowHandler.handleNoShows(reservations);
    }

    protected boolean isAllowedToParticipate(Exam exam, User user, EmailComposer composer) {
        handleNoShow(user, exam.getId(), composer);
        Integer trialCount = exam.getTrialCount();
        if (trialCount == null) {
            return true;
        }
        List<ExamParticipation> participations = Ebean.find(ExamParticipation.class)
                .fetch("exam")
                .where()
                .eq("user", user)
                .eq("exam.parent.id", exam.getId())
                .ne("exam.state", Exam.State.DELETED)
                .ne("reservation.retrialPermitted", true)
                .findList();
        List<ExamEnrolment> noShows = Ebean.find(ExamEnrolment.class)
                .fetch("reservation")
                .where()
                .eq("user", user)
                .eq("exam.id", exam.getId())
                .eq("reservation.noShow", true)
                .ne("reservation.retrialPermitted", true)
                .findList();
        List<CountsAsTrial> trials = new ArrayList<>(participations);
        trials.addAll(noShows);
        // Sort by trial time desc
        trials.sort((o1, o2) -> o1.getTrialTime().isAfter(o2.getTrialTime()) ? -1 : 1);

        if (trials.size() >= trialCount) {
            List<CountsAsTrial> subset = trials.subList(0, trialCount);
            return subset.stream().anyMatch(CountsAsTrial::isProcessed);
        }
        return true;
    }

    protected CompletionStage<Result> wrapAsPromise(final Result result) {
        return CompletableFuture.supplyAsync(() -> result);
    }

    protected Double round(Double src) {
        final Double C = 100d;
        return src == null ? null : Math.round(src * C) / C;
    }

}
