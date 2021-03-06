/*
 * Copyright (c) 2018 Exam Consortium
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
 *
 */

package backend.controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public interface LocalAttachmentInterface extends BaseAttachmentInterface<Long> {

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    Result deleteQuestionAttachment(Long id);

    @Restrict({@Group("TEACHER"), @Group("ADMIN"), @Group("STUDENT")})
    CompletionStage<Result> downloadQuestionAttachment(Long id);

    @Restrict({@Group("ADMIN"), @Group("STUDENT")})
    CompletionStage<Result> deleteQuestionAnswerAttachment(Long qid);

    @Restrict({@Group("TEACHER"), @Group("ADMIN"), @Group("STUDENT")})
    CompletionStage<Result> downloadQuestionAnswerAttachment(Long qid);
}
