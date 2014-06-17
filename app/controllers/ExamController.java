package controllers;

import Exceptions.MalformedDataException;
import Exceptions.SitnetException;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.Query;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonWriteOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;
import models.questions.AbstractQuestion;
import models.questions.EssayQuestion;
import models.questions.MultipleChoiceQuestion;
import models.questions.MultipleChoiseOption;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import util.SitnetUtil;
import util.java.EmailComposer;

import java.sql.Timestamp;
import java.util.*;

public class ExamController extends SitnetController {

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getExams() {

        String oql = null;
        Query<Exam> query = null;

        User user = UserController.getLoggedUser();
        if(user.hasRole("TEACHER")) {
            oql = "find exam " +
                  "fetch examSections " +
                  "fetch course " +
                  "where (state=:published or state=:saved) " +
                  "and (shared=:is_shared or creator.id=:myid)";
//                  "and (shared:shared or creator.id:myid)";

            query = Ebean.createQuery(Exam.class, oql);
            query.setParameter("published", "PUBLISHED");
            query.setParameter("saved", "SAVED");
            query.setParameter("is_shared", true);
            query.setParameter("myid", user.getId());
        }
        else if (user.hasRole("ADMIN")) {

            oql = "find exam "
                    + " fetch examSections "
                    + " fetch course "
                    + " where state=:published or state=:saved";

            query = Ebean.createQuery(Exam.class, oql);
            query.setParameter("published", "PUBLISHED");
            query.setParameter("saved", "SAVED");
        }


        List<Exam> exams = query.findList();

        return ok(Json.toJson(exams));
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getExamsByState(String state) {

        String oql =
                "  find  exam "
                        + " fetch examSections "
                        + " fetch course "
                        + " where state=:examstate";

        Query<Exam> query = Ebean.createQuery(Exam.class, oql);
        query.setParameter("examstate", state);

        // Todo: Uncomment when student_id gets set to the EXAM table
        //query.setParameter("userid", user.getId());

        List<Exam> exams = query.findList();

        return ok(Json.toJson(exams));
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getActiveExams() {

        User user = UserController.getLoggedUser();

        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        //Get list of exams that user is assigned to inspect
        List<ExamInspection> examInspections = Ebean.find(ExamInspection.class)
                .fetch("exam")
                .where()
                .eq("user.id", user.getId())
                .findList();

        // Todo: Oletetaan että tentin luoja on automaattisesti tentin tarkastaja
        List<Exam> activeExams = Ebean.find(Exam.class)
                .fetch("course")
                .where()
                .eq("creator.id", user.getId())
                .ne("state", "SAVED")
                .betweenProperties("examActiveStartDate", "examActiveEndDate", timestamp)
                .findList();

        // Todo: Hae tentit joissa tämä käyttäjä on tarkastaja, palauta JSON propertiesilla

//        List<ExamEnrolment> enrolments =


        JsonContext jsonContext = Ebean.createJsonContext();
        JsonWriteOptions options = new JsonWriteOptions();
        options.setRootPathProperties("id, name, course, examActiveStartDate, examActiveEndDate");
        options.setPathProperties("course", "code");
//        options.setPathProperties("reservation", "startAt, machine");
//        options.setPathProperties("reservation.machine", "name");

        return ok(jsonContext.toJsonString(activeExams, true, options)).as("application/json");

        // For some reason conjuction didn't do AND to the lt and gt expressions. Individually both works
//        Query q = Ebean.createQuery(Exam.class);
//        q.where().conjunction()
//                .add(Expr.lt("examActiveStartDate", timestamp))
//                .add(Expr.gt("examActiveEndDate", timestamp));

//        List<Exam> activeExams = q.findList();

//        return ok(Json.toJson(activeExams));
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getFinishedExams() {

        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        List<Exam> finishedExams = Ebean.find(Exam.class)
                .where()
                .lt("examActiveEndDate", timestamp)
                .findList();

        return ok(Json.toJson(finishedExams));
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result deleteExam(Long id) {

        Exam exam = Ebean.find(Exam.class, id);
        if(UserController.getLoggedUser().hasRole("ADMIN") || SitnetUtil.isOwner(exam))
        {

            // check if exam has children
            // if true, we cannot delete it because children exams reference this exam
            // so we just set it ARCHIVED

            int count = Ebean.find(Exam.class)
                    .where()
                    .eq("parent.id", id)
                    .findRowCount();

            if(count > 0) {
                exam.setState(Exam.State.ARCHIVED.name());
                try {
                    exam = (Exam) SitnetUtil.setModifier(exam);
                } catch (SitnetException e) {
                    e.printStackTrace();
                }
                exam.save();
                return ok("Exam archived");
            }
            else {

                // If we're here it means, this exam does not have any children.
                // e.g. this exam has never been cloned
                // we can safely delete it completely from DB

                // 1. remove enrolments. Though there shouldn't be any
                List<ExamEnrolment> examEnrolments = Ebean.find(ExamEnrolment.class)
                        .where()
                        .eq("exam.id", id)
                        .findList();

                for(ExamEnrolment e : examEnrolments) {
                    e.delete();
                }

                List<ExamInspection> examInspections = Ebean.find(ExamInspection.class)
                        .where()
                        .eq("exam.id", id)
                        .findList();

                // 2. remove inspections
                for(ExamInspection e : examInspections) {
                    e.getUser().getInspections().remove(e);
                    e.delete();
                }

                for(ExamSection es : exam.getExamSections())
                {
                    es.getQuestions().clear();
                    es.saveManyToManyAssociations("questions");
                    es.save();
                }

                exam.getExamSections().clear();

                // yes yes, its weird, but Ebean won't delete relations with ManyToMany on enchaced classes
                // so we just tell everyone its "deleted"
                exam.setState("DELETED");
                exam.save();

//                exam.delete();
            }


            return ok("Exam deleted");
        }
        else
            return forbidden("You don't have the permission to modify this object");

    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getExam(Long id) {

        Exam exam = Ebean.find(Exam.class)
                .fetch("course")
                .fetch("examSections")
                .fetch("examSections.questions")
                .where()
                .eq("id", id)
                .findUnique();

        if (exam == null)
        {
            return notFound();
        }
        else if(exam.isShared() || SitnetUtil.isOwner(exam) || exam.getState().equals("REVIEW") || exam.getState().equals("REVIEWED") || exam.getState().equals("IN_REVIEW"))
        {
            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, name, course, examType, instruction, shared, examSections, examActiveStartDate, examActiveEndDate, room, " +
                    "duration, grading, otherGrading, totalScore, examLanguage, answerLanguage, state, examFeedback, creditType, expanded, attachment");
            options.setPathProperties("course", "id, organisation, code, name, level, type, credits");
            options.setPathProperties("room", "id, name");
            options.setPathProperties("attachment", "id, fileName");
            options.setPathProperties("course.organisation", "id, code, name, nameAbbreviation, courseUnitInfoUrl, recordsWhitelistIp, vatIdNumber");
            options.setPathProperties("examType", "id, type");
            options.setPathProperties("examSections", "id, name, questions, exam, totalScore, expanded, lotteryOn, lotteryItemCount");
            options.setPathProperties("examSections.questions", "id, type, question, shared, instruction, maxScore, evaluationType, evaluatedScore, options, answer");
            options.setPathProperties("examSections.questions.answer", "type, option, answer");
            options.setPathProperties("examSections.questions.answer.option", "id, option, correctOption, score");
            options.setPathProperties("examSections.questions.options", "id, option" );
            options.setPathProperties("examSections.questions.comments", "id, comment");
            options.setPathProperties("examFeedback", "id, comment");

            return ok(jsonContext.toJsonString(exam, true, options)).as("application/json");
        }
        else {
            return forbidden("You are not allowed to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result reviewExam(Long id) {
        DynamicForm df = Form.form().bindFromRequest();

        Exam ex = Form.form(Exam.class).bindFromRequest(
                "id",
                "instruction",
                "name",
                "shared",
                "state",
                "room",
                "duration",
                "grading",
                "otherGrading",
                "totalScore",
                "examLanguage",
                "answerLanguage",
                "grade",
                "creditType",
                "expanded")
                .get();

        ex.generateHash();
        ex.update();

        return ok(Json.toJson(ex));
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getExamReviews(Long eid) {

        // Todo: Assume that exam creator is also exam inspector
        List<Exam> examReviews = Ebean.find(Exam.class)
                .fetch("course")
                .where()
                .eq("parent.id", eid)
                .findList();

        if (examReviews == null) {
            return notFound();
        } else {
            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, name, course, examActiveStartDate, examActiveEndDate");
            options.setPathProperties("course", "code");

            return ok(jsonContext.toJsonString(examReviews, true, options)).as("application/json");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result insertComment(Long eid, Long cid) throws MalformedDataException {

        Comment comment = bindForm(Comment.class);
        comment.save();

        Exam exam = Ebean.find(Exam.class, eid);
        exam.setExamFeedback(comment);
        exam.save();

        JsonContext jsonContext = Ebean.createJsonContext();
        JsonWriteOptions options = new JsonWriteOptions();
        options.setRootPathProperties("id, comment, creator");
        options.setPathProperties("creator", "id, firstName, lastName");

        return ok(jsonContext.toJsonString(comment, true, options)).as("application/json");
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result updateComment(Long eid, Long cid) throws MalformedDataException {

        Comment bindComment = bindForm(Comment.class);

        Comment comment = Ebean.find(Comment.class, cid);

        try {
            comment = (Comment) SitnetUtil.setCreator(comment);
        } catch (SitnetException e) {
            e.printStackTrace();
        }
        comment.setComment(bindComment.getComment());
        comment.save();

        Exam exam = Ebean.find(Exam.class, eid);

        exam.setExamFeedback(comment);
        exam.save();

        if (comment == null) {
            return notFound();
        } else {
            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, comment, creator");
            options.setPathProperties("creator", "id, firstName, lastName");

            return ok(jsonContext.toJsonString(comment, true, options)).as("application/json");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result updateExam(Long id) throws MalformedDataException {

        DynamicForm df = Form.form().bindFromRequest();

        Exam exam = Ebean.find(Exam.class)
                .fetch("course")
                .fetch("examSections")
                .fetch("room")
                .where()
                .eq("id", id)
                .findUnique();

        if (exam == null) {
            return notFound();
        }
        else if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {

            String examName = df.get("name");
            boolean shared = Boolean.parseBoolean(df.get("shared"));
            String duration = df.get("duration");
            String grading = df.get("grading");
            String answerLanguage = df.get("answerLanguage");
            String examLanguage = df.get("examLanguage");
            String instruction = df.get("instruction");
            String state = df.get("state");
            boolean expanded = Boolean.parseBoolean(df.get("expanded"));

            if (examName != null) {
                exam.setName(examName);
            }

            if (state!= null) {
                exam.setState(state);
            }

            if (shared) {
                exam.setShared(shared);
            }

            Long start = new Long(df.get("examActiveStartDate"));
            Long end = new Long(df.get("examActiveEndDate"));

            if (start != 0) {
                exam.setExamActiveStartDate(new Timestamp(start));
            }
            if (end != 0) {
                exam.setExamActiveEndDate(new Timestamp(end));
            }

            if (duration != null) {
                exam.setDuration(Double.valueOf(duration));
            }

            if (grading != null) {
                exam.setGrading(grading);
            }

            if (answerLanguage != null) {
                exam.setAnswerLanguage(answerLanguage);
            }

            if (examLanguage != null) {
                exam.setExamLanguage(examLanguage);
            }

            if (instruction != null) {
                exam.setInstruction(instruction);
            }

            if(df.get("course.credits") != null) {
                Double credits = new Double(df.get("course.credits"));

                // TODO: this is not right, we cant set credits to Course,
                // TODO: move this to Exam
                exam.getCourse().setCredits(credits);
                exam.getCourse().save();
            }

            if(df.get("examType.type") != null) {
                String examType = df.get("examType.type");

                ExamType eType = Ebean.find(ExamType.class)
                        .where()
                        .eq("type", examType)
                        .findUnique();

                if (eType == null) {
                    ExamType newType = new ExamType(examType);
                    newType.save();
                    exam.setExamType(newType);
                }
                else {
                    exam.setExamType(eType);
                }
            }
            exam.generateHash();

            if (state != null) {
                exam.setInstruction(instruction);
            }

            if (df.get("expanded") != null) {
                exam.setExpanded(expanded);
            }

            exam.save();

            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, name, course, examType, instruction, shared, examSections, examActiveStartDate, examActiveEndDate, room, " +
                    "duration, grading, otherGrading, totalScore, examLanguage, answerLanguage, state, examFeedback, creditType, expanded");
            options.setPathProperties("course", "id, organisation, code, name, level, type, credits");
            options.setPathProperties("course.organisation", "id, code, name, nameAbbreviation, courseUnitInfoUrl, recordsWhitelistIp, vatIdNumber");
            options.setPathProperties("examType", "id, type");
            options.setPathProperties("examSections", "id, name, questions, exam, totalScore, expanded");
            options.setPathProperties("examSections.questions", "id, type, question, shared, instruction, maxScore, evaluatedScore, options");
            options.setPathProperties("examSections.questions.options", "id, option" );
            options.setPathProperties("examSections.questions.comments", "id, comment");
            options.setPathProperties("examFeedback", "id, comment");

            return ok(jsonContext.toJsonString(exam, true, options)).as("application/json");
        }
        else {
            return forbidden("You are not allowed to modify this object");
        }
   }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result createExamDraft() throws MalformedDataException {

        Exam exam = new Exam();
        exam.setName("Kirjoita tentin nimi tähän");
        // Decided to drop the DRAFT state from the whole project
        exam.setState("SAVED");
        try {
            SitnetUtil.setCreator(exam);
        } catch (SitnetException e) {
            e.printStackTrace();
            return ok(e.getMessage());
        }
        exam.save();

        ExamSection examSection = new ExamSection();
        examSection.setName("Aihealue");
        try {
            SitnetUtil.setCreator(examSection);
        } catch (SitnetException e) {
            e.printStackTrace();
            return ok(e.getMessage());
        }
        examSection.setExam(exam);
        examSection.setExpanded(true);
        examSection.save();

        exam.getExamSections().add(examSection);
        exam.setExamLanguage("fi");
        exam.save();

        // add creator to inspector list (SITNET-178)
        ExamInspection inspection = new ExamInspection();
        inspection.setExam(exam);
        inspection.setUser(UserController.getLoggedUser());
        inspection.save();

        exam.save();

        // return only id, its all we need at this point
        ObjectNode part = Json.newObject();
        part.put("id", exam.getId());

        return ok(Json.toJson(part));
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result insertSection(Long id) throws MalformedDataException {

        Exam exam = Ebean.find(Exam.class, id);
        if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {

            ExamSection section = bindForm(ExamSection.class);
            section.setExam(Ebean.find(Exam.class, id));
            try {
                section = (ExamSection) SitnetUtil.setCreator(section);
            } catch (SitnetException e) {
                e.printStackTrace();
                return ok(e.getMessage());
            }

            section.save();
            return ok(Json.toJson(section));
        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result updateCourse(Long eid, Long cid) throws MalformedDataException {

        Exam exam = Ebean.find(Exam.class, eid);
        if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {
            Course course = Ebean.find(Course.class, cid);
            exam.setCourse(course);
            exam.save();
            return ok(Json.toJson(exam));
        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result updateRoom(Long eid, Long rid) throws MalformedDataException {
    	
    	Exam exam = Ebean.find(Exam.class, eid);
    	if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {
    		ExamRoom room = Ebean.find(ExamRoom.class, rid);
    		exam.setRoom(room);
    		exam.save();
    		return ok(Json.toJson(exam));
    	}
    	else {
    		return forbidden("You don't have the permission to modify this object");
    	}
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result insertExamType(Long eid, Long etid) throws MalformedDataException {

        Exam exam = Ebean.find(Exam.class, eid);
        if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {

            ExamType examType = Ebean.find(ExamType.class, etid);
            exam.setExamType(examType);
            exam.save();

            return ok(Json.toJson(exam));
        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result insertQuestion(Long eid, Long sid, Long qid) throws MalformedDataException {

        Exam exam = Ebean.find(Exam.class, eid);
        if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {
            AbstractQuestion question = Ebean.find(AbstractQuestion.class, qid);

            switch (question.getType()) {
                case "MultipleChoiceQuestion": {
                    MultipleChoiceQuestion multiQuestion = Ebean.find(MultipleChoiceQuestion.class, qid);
                    MultipleChoiceQuestion clonedQuestion;
                    clonedQuestion = (MultipleChoiceQuestion)multiQuestion.clone();
                    clonedQuestion.setParent(multiQuestion);
                    try {
                        clonedQuestion = (MultipleChoiceQuestion) SitnetUtil.setCreator(clonedQuestion);
                    } catch (SitnetException e) {
                        e.printStackTrace();
                    }
                    clonedQuestion.setOptions(new ArrayList<MultipleChoiseOption>());
                    clonedQuestion.save();
                    List<MultipleChoiseOption> options = multiQuestion.getOptions();
                    for (MultipleChoiseOption o : options) {
                        MultipleChoiseOption clonedOpt = (MultipleChoiseOption) o.clone();
                        clonedOpt.setQuestion(clonedQuestion);
                        clonedOpt.save();
                        clonedQuestion.getOptions().add(clonedOpt);
                    }

                    ExamSection section = Ebean.find(ExamSection.class, sid);
                    section.getQuestions().add(clonedQuestion);
                    section.save();

                    return ok(Json.toJson(section));
                }
                case "EssayQuestion": {
                    EssayQuestion essayQuestion = Ebean.find(EssayQuestion.class, qid);
                    EssayQuestion clonedQuestion;
                    clonedQuestion = (EssayQuestion)essayQuestion.clone();
                    clonedQuestion.save();

                    ExamSection section = Ebean.find(ExamSection.class, sid);
                    section.getQuestions().add(clonedQuestion);
                    section.save();

                    return ok(Json.toJson(section));
                }
                default:
                    return ok("Question type not specified");
            }

        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result removeQuestion(Long eid, Long sid, Long qid) throws MalformedDataException {
        Exam exam = Ebean.find(Exam.class, eid);
        if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {
            AbstractQuestion question = Ebean.find(AbstractQuestion.class, qid);
            ExamSection section = Ebean.find(ExamSection.class, sid);
            section.getQuestions().remove(question);
            section.save();
            question.delete();
            return ok(Json.toJson(section));
        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result clearQuestions(Long sid) throws MalformedDataException {
        ExamSection section = Ebean.find(ExamSection.class, sid);

        if(SitnetUtil.isOwner(section) || UserController.getLoggedUser().hasRole("ADMIN")) {

            for(AbstractQuestion q : section.getQuestions())
            {
                section.getQuestions().remove(q);
                q.delete();
            }
            Ebean.deleteManyToManyAssociations(section, "questions");
            section.save();
            return ok(Json.toJson(section));
        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result removeSection(Long eid, Long sid) {
        Exam exam = Ebean.find(Exam.class, eid);
        if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {
            ExamSection section = Ebean.find(ExamSection.class, sid);
            exam.getExamSections().remove(section);
            exam.save();
            section.delete();
            return ok();
        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result updateSection(Long eid, Long sid) {

        // TODO: should check is user is owner ?
        ExamSection section = Form.form(ExamSection.class).bindFromRequest(
                "id",
                "name",
                "expanded",
                "lotteryOn",
                "lotteryItemCount"
        ).get();

        section.update();
        return ok();
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result createExam() throws MalformedDataException {

        Exam ex = bindForm(Exam.class);

        switch (ex.getState()) {
            case "SAVED": {
                ex.setId(null);
                try {
                    SitnetUtil.setCreator(ex);
                } catch (SitnetException e) {
                    e.printStackTrace();
                    return ok(e.getMessage());
                }
                ex.save();

                return ok(Json.toJson(ex));
            }
            case "PUBLISHED": {

                List<ExamSection> examSections = ex.getExamSections();
                for (ExamSection es : examSections) {
                    es.setId(null);

                    List<AbstractQuestion> questions = es.getQuestions();
                    for (AbstractQuestion q : questions) {
                        q.setId(null);

                        switch (q.getType()) {
                            case "MultipleChoiceQuestion": {
                                List<MultipleChoiseOption> options = ((MultipleChoiceQuestion) q).getOptions();
                                for (MultipleChoiseOption o : options) {
                                    o.setId(null);
                                }
                            }
                            break;
                        }
                    }
                }
                ex.save();
                return ok();
            }
            default:
        }

        return badRequest("Jokin meni pieleen");
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getExamSections(Long examid) {
        Exam exam = Ebean.find(Exam.class, examid);
        if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {
            List<ExamSection> sections = Ebean.find(ExamSection.class).where()
                    .eq("id", examid)
                    .findList();

            return ok(Json.toJson(sections));
        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
        public static Result deleteSection(Long sectionId) {

        Exam exam = Ebean.find(Exam.class)
                .where()
                .eq("examSections.id", sectionId)
                .findUnique();

        if(SitnetUtil.isOwner(exam) || UserController.getLoggedUser().hasRole("ADMIN")) {

            ExamSection section = Ebean.find(ExamSection.class, sectionId);
            exam.getExamSections().remove(section);
            exam.save();
            section.delete();

            return ok("removed");
        }
        else {
            return forbidden("You don't have the permission to modify this object");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getEnrolments() {
        List<ExamEnrolment> enrolments = Ebean.find(ExamEnrolment.class).findList();

        if (enrolments == null) {
            return notFound();
        } else {
            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, enrolledOn, user, exam");
            options.setPathProperties("user", "id");
            options.setPathProperties("exam", "id");

            return ok(jsonContext.toJsonString(enrolments, true, options)).as("application/json");
        }
    }
    
    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getEnrolmentsForUser(Long uid) {
        List<ExamEnrolment> enrolments = Ebean.find(ExamEnrolment.class)
                .fetch("exam")
                .fetch("reservation")
                .where()
                .eq("user.id", uid)
                .findList();

        if (enrolments == null) {
            return notFound();
        } else {
            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, enrolledOn, user, exam, reservation");
            options.setPathProperties("user", "id");
            options.setPathProperties("exam", "id, name, course");
            options.setPathProperties("exam.course", "code");
            options.setPathProperties("reservation", "startAt, machine");
            options.setPathProperties("reservation.machine", "name");

            return ok(jsonContext.toJsonString(enrolments, true, options)).as("application/json");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN"), @Group("STUDENT")})
    public static Result getEnrolmentsForExam(Long eid) {
        List<ExamEnrolment> enrolments = Ebean.find(ExamEnrolment.class)
                .fetch("exam")
                .fetch("reservation")
                .where()
                .eq("exam.id", eid)
                .findList();

        if (enrolments == null) {
            return notFound();
        } else {
            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, enrolledOn, user, exam, reservation");
            options.setPathProperties("user", "id");
            options.setPathProperties("exam", "id, name, course");
            options.setPathProperties("exam.course", "code");
            options.setPathProperties("reservation", "startAt, machine");
            options.setPathProperties("reservation.machine", "name");

            return ok(jsonContext.toJsonString(enrolments, true, options)).as("application/json");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getParticipationsForExam(Long eid) {
        List<ExamParticipation> participations = Ebean.find(ExamParticipation.class)
                .fetch("exam")
                .fetch("exam.parent")
                .where()
                .eq("exam.parent.id", eid)
                .findList();

        if (participations == null) {
            return notFound();
        } else {
            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, user, exam, started, ended");
            options.setPathProperties("user", "id");
            options.setPathProperties("exam", "id, name, course");
            options.setPathProperties("exam.course", "code");

            return ok(jsonContext.toJsonString(participations, true, options)).as("application/json");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getExamInspections(Long id) {

        Exam exam = Ebean.find(Exam.class, id);

        List<ExamInspection> inspections = null;
        if(exam.getParent() == null) {
            inspections = Ebean.find(ExamInspection.class).where().eq("exam.id", id).findList();
        } else {
            inspections = Ebean.find(ExamInspection.class).where().eq("exam.id", exam.getParent().getId()).findList();
        }

        Map<String, User> results = new HashMap<>();

        if(inspections != null) {
            for(ExamInspection i : inspections) {
                results.put("" + i.getId(), i.getUser());
            }
        }
        return ok(Json.toJson(results));
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result getInspections() {
        List<ExamInspection> inspections = Ebean.find(ExamInspection.class)
                .fetch("exam", "name", new FetchConfig().query())
                .findList();

        if (inspections == null) {
            return notFound();
        } else {
            JsonContext jsonContext = Ebean.createJsonContext();
            JsonWriteOptions options = new JsonWriteOptions();
            options.setRootPathProperties("id, user, exam");
            options.setPathProperties("user", "id");
            options.setPathProperties("exam", "id");

            return ok(jsonContext.toJsonString(inspections, true, options)).as("application/json");
        }
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result insertInspection(Long eid, Long uid) throws SitnetException {

        ExamInspection inspection = bindForm(ExamInspection.class);
        User recipient = Ebean.find(User.class, uid);
        Exam exam = Ebean.find(Exam.class, eid);

        if(exam.getParent() == null) {
            inspection.setExam(exam);
        } else {
            inspection.setExam(exam.getParent());
        }
        inspection.setUser(recipient);

        inspection.setComment((Comment) SitnetUtil.setCreator(inspection.getComment()));
        inspection.getComment().save();
        inspection.save();

        // SITNET-295
        if(exam.getParent() == null) {
            EmailComposer.composeSimpleInspectionReadyNotification(recipient, exam.getCreator(), exam, inspection.getComment().getComment());
        }else {
            EmailComposer.composeSimpleInspectionReadyNotification(recipient, exam.getCreator(), exam.getParent(), inspection.getComment().getComment());
        }

        return ok(Json.toJson(inspection));
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN")})
    public static Result deleteInspection(Long id) {

        Ebean.delete(ExamInspection.class, id);

        return ok();
    }
}
