/*
 * Copyright (c) 2017 The members of the EXAM Consortium (https://confluence.csc.fi/display/EXAM/Konsortio-organisaatio)
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

package sanitizers;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class EnrolmentInformationSanitizer extends play.mvc.Action.Simple {

    public CompletionStage<Result> call(Http.Context ctx) {
        JsonNode body = ctx.request().body().asJson();
        return delegate.call(ctx.withRequest(sanitize(ctx, body)));
    }

    private Http.Request sanitize(Http.Context ctx, JsonNode body) {
        return ctx.request().addAttr(Attrs.ENROLMENT_INFORMATION, body.get("information").asText());
    }
}
