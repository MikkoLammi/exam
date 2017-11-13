package sanitizers;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import play.Logger;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ExaminationDateSanitizer extends play.mvc.Action.Simple {

    public CompletionStage<Result> call(Http.Context ctx) {
        JsonNode body = ctx.request().body().asJson();
        try {
            return delegate.call(ctx.withRequest(sanitize(ctx, body)));
        } catch (SanitizingException e) {
            Logger.error("Sanitizing error: " + e.getMessage(), e);
            return CompletableFuture.supplyAsync(Results::badRequest);
        }
    }

    private Http.Request sanitize(Http.Context ctx, JsonNode body) throws SanitizingException {
        if (body.has("date")) {
            LocalDate date = LocalDate.parse(body.get("date").asText(), ISODateTimeFormat.dateTimeParser());
            return ctx.request().addAttr(Attrs.DATE, date);
        } else {
            throw new SanitizingException("no date");
        }
    }
}