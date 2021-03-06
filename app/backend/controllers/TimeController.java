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

package backend.controllers;

import java.io.IOException;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import io.ebean.Ebean;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import play.mvc.Result;

import backend.controllers.base.BaseController;
import backend.models.Exam;
import backend.models.ExamEnrolment;
import backend.models.User;
import backend.util.DateTimeUtils;


public class TimeController extends BaseController {

    @Restrict({@Group("STUDENT")})
    public Result getExamRemainingTime(String hash) throws IOException {

        User user = getLoggedUser();
        ExamEnrolment enrolment = Ebean.find(ExamEnrolment.class)
                .fetch("reservation")
                .fetch("reservation.machine")
                .fetch("reservation.machine.room")
                .fetch("exam")
                .fetch("externalExam")
                .where()
                .disjunction()
                .eq("exam.hash", hash)
                .eq("externalExam.hash", hash)
                .endJunction()
                .eq("user.id", user.getId())
                .findOne();

        if (enrolment == null) {
            return notFound();
        }

        final DateTime reservationStart = new DateTime(enrolment.getReservation().getStartAt());
        final int durationMinutes = getDuration(enrolment);
        DateTime now = DateTimeUtils.adjustDST(DateTime.now(), enrolment.getReservation());
        final Seconds timeLeft = Seconds.secondsBetween(now, reservationStart.plusMinutes(durationMinutes));

        return ok(String.valueOf(timeLeft.getSeconds()));
    }

    private int getDuration(ExamEnrolment enrolment) throws IOException {
        if (enrolment.getExam() != null) {
            return enrolment.getExam().getDuration();
        }
        Exam exam = enrolment.getExternalExam().deserialize();
        return exam.getDuration();
    }

}
