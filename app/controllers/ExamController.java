package controllers;

import Exceptions.MalformedDataException;
import Exceptions.SitnetException;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.Query;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonWriteOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;
import models.questions.AbstractQuestion;
import models.questions.MultipleChoiceQuestion;
import models.questions.MultipleChoiseOption;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import util.SitnetUtil;

import java.sql.Timestamp;
import java.util.List;

public class ExamController extends SitnetController {


	//    @Authenticate
	//    @Restrict(@Group({"TEACHER"}))
    public static Result getExams() {
        Logger.debug("getExams()");

        String oql = 
                "  find  exam "
                +" fetch examSections "
                +" fetch course "
                +" where state=:published or state=:saved or state=:draft";
        
        Query<Exam> query = Ebean.createQuery(Exam.class, oql);
        query.setParameter("published", "PUBLISHED");
        query.setParameter("saved", "SAVED");
        query.setParameter("draft", "DRAFT");

        List<Exam> exams = query.findList(); 

        return ok(Json.toJson(exams));
    }

    public static Result getExamsByState(String state) {
        Logger.debug("getExamsByState()");

//        User user = UserController.getLoggedUser();

//        List<Exam> exams = Ebean.find(Exam.class)
//            .fetch("examEvent")
//            .fetch("course")
//            .fetch("examSections")
//            .where()
//                .eq("state", state)
//                .eq("student.id", user.getId())
//            .findList();

        String oql =
            "  find  exam "
                +" fetch examSections "
                +" fetch course "
                +" where state=:examstate";

        Query<Exam> query = Ebean.createQuery(Exam.class, oql);
        query.setParameter("examstate", state);

        // Todo: Uncomment when student_id gets set to the EXAM table
        //query.setParameter("userid", user.getId());

        List<Exam> exams = query.findList();

        return ok(Json.toJson(exams));
    }

    public static Result getExam(Long id) {
    	Logger.debug("getExam(:id)");

        Query<Exam> query = Ebean.createQuery(Exam.class);
        query.fetch("course");
        query.fetch("examSections");
        query.setId(id);

        Exam exam = query.findUnique();
   	
    	return ok(Json.toJson(exam));
    }

    public static Result reviewExam(Long id) {
        Logger.debug("reviewExam(:id)");

//        Query<Exam> query = Ebean.createQuery(Exam.class);
//        query.fetch("examEvent");
//        query.fetch("course");
//        query.fetch("examSections");
//        query.setId(id);
//
//        Exam exam = query.findUnique();
//
//        return ok(Json.toJson(exam));

        return ok("testi toimii");
    }

    public static Result updateExam(Long id) throws MalformedDataException {

        DynamicForm df = Form.form().bindFromRequest();

        Long start = new Long(df.get("examActiveStartDate"));
        Long end = new Long(df.get("examActiveEndDate"));

    	Exam ex = Form.form(Exam.class).bindFromRequest(
    	"id",
    	"instruction",
    	"name",
    	"shared",
    	"state",
        "room",
        "duration",
        "grading",
        "examLanguage",
        "answerLanguage").get();

        if(SitnetUtil.isOwner(ex))
        {
            ex.setExamActiveStartDate(new Timestamp(start));
            ex.setExamActiveEndDate(new Timestamp(end));

            try {
                SitnetUtil.setModifier(ex);
            } catch (SitnetException e) {
                e.printStackTrace();
            }
            ex.generateHash();
            ex.update();

            return ok(Json.toJson(ex));
        }
        else
            return notFound("Sinulla ei oikeuksia muokata tätä objektia");
    }
    
    public static Result createExamDraft() throws MalformedDataException {
        Logger.debug("createExamDraft()");

        Exam exam = new Exam();
        exam.setName("Kirjoita tentin nimi tähän");
        exam.setState("DRAFT");
        try {
			SitnetUtil.setCreator(exam);
		} catch (SitnetException e) {
			e.printStackTrace();
	        return ok(e.getMessage());
		}
        exam.save();
        
        ExamSection examSection = new ExamSection();
        examSection.setName("Osio");
        try {
			SitnetUtil.setCreator(examSection);
		} catch (SitnetException e) {
			e.printStackTrace();
			return ok(e.getMessage());
		}
        examSection.setExam(exam);
        examSection.save();
        exam.getExamSections().add(examSection);
        exam.setDuration(new Double(3));
        exam.setExamLanguage("fi");
        
        exam.save();

        ObjectNode part = Json.newObject();
    	part.put("id", exam.getId());
    	
        return ok(Json.toJson(part));
    }

    public static Result insertSection(Long id) throws MalformedDataException {
        Logger.debug("insertSection()");

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

    public static Result insertQuestion(Long eid, Long sid) throws MalformedDataException {
        Logger.debug("insertQuestion()");

//        AbstractQuestion question = Ebean.find(AbstractQuestion.class, id);

        return ok();
    }

    public static Result insertAQuestion(Long eid, Long sid, Long qid) throws MalformedDataException {
    	Logger.debug("insertQuestion()");
    	
    	// TODO: Create a clone of the question and add it to section
    	// TODO: should implement AbstractQuestion.clone
        AbstractQuestion question = Ebean.find(AbstractQuestion.class, qid);
        ExamSection section = Ebean.find(ExamSection.class, sid);
        section.getQuestions().add(question);
        section.save();
    	
    	return ok(Json.toJson(section));
    }
    
    public static Result removeQuestion(Long eid, Long sid, Long qid) throws MalformedDataException {
    	Logger.debug("insertQuestion()");

    	AbstractQuestion question = Ebean.find(AbstractQuestion.class, qid);
    	ExamSection section = Ebean.find(ExamSection.class, sid);
    	section.getQuestions().remove(question);
    	section.save();
    	
    	return ok(Json.toJson(section));
    }
    
    public static Result removeSection(Long eid, Long sid) {
    	Logger.debug("insertQuestion()");

    	Ebean.delete(ExamSection.class, sid);
    	
    	return ok();
    }
    
    public static Result updateSection(Long eid, Long sid) {
    	Logger.debug("insertQuestion()");
    	
    	ExamSection section = Form.form(ExamSection.class).bindFromRequest(
    	"id",
    	"name").get();

    	section.update();

    	return ok();
    }
    

    //  @Authenticate
//    @Restrict(@Group({"TEACHER"}))
    public static Result createExam() throws MalformedDataException {
        Logger.debug("createExam()");

        Exam ex = bindForm(Exam.class);

        switch (ex.getState()) {
            case "DRAFT":
            {
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
//            		es.save();

                    List<AbstractQuestion> questions = es.getQuestions();
                    for (AbstractQuestion q : questions) {
                        q.setId(null);
//                		q.save();

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

                Logger.debug(ex.toString());
                ex.save();

                return ok();
            }

            default:

        }



/**
 *
 * play.api.Application$$anon$1: Execution exception[[PersistenceException: ERROR executing DML bindLog[] error[NULL not allowed for column
 * "QUESTION_TYPE"; SQL statement:\n insert into question
 * (id, question_type, created, modified, type, question, shared, instruction, hash, creator_id, modifier_id, derived_from_question_id)
 * values (?,?,?,?,?,?,?,?,?,?,?,?) [23502-172]]]]

 Ebean fails to insert Discriminator
 This is a bug

 Discussion:
 *  http://eclipse.1072660.n5.nabble.com/Value-of-DiscriminatorValue-not-persisted-td162195.html
 *
 *  Bug:
 *  https://bugs.eclipse.org/bugs/show_bug.cgi?id=415526
 *
 *  Possible solution:
 *  Update Ebean to v.2.5.1
 *
 *
 *
 */


        return badRequest("Jokin meni pieleen");
    }

    //  @Authenticate
    public static Result getExamSections(Long examid) {
        List<ExamSection> sections = Ebean.find(ExamSection.class).where()
                .eq("id", examid)
                .findList();

        return ok(Json.toJson(sections));
    }

 //  @Authenticate
    public static Result deleteSection(Long sectionId) {
        Ebean.delete(ExamSection.class, sectionId);

        return ok("removed");
    }

    //    @Authenticate
//    @Restrict(@Group({"TEACHER"}))
    public static Result addSection() {

        DynamicForm df = Form.form().bindFromRequest();

        Logger.debug("course Code: " + df.get("courseCode"));
        Logger.debug("course Name: " + df.get("courseName"));
        Logger.debug("course Scope: " + df.get("courseScope"));
        Logger.debug("Faculty Name: " + df.get("facultyName"));
        Logger.debug("Exam Instructor Name: " + df.get("instructorName"));

        User user = UserController.getLoggedUser();

        return ok("section created");
    }

	public static Result getEnrolments() {
		List<ExamEnrolment> enrolments = Ebean.find(ExamEnrolment.class).findList();


//        return ok(Json.toJson(enrolments));

 
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

    public static Result getInspections() {
    	List<ExamInspection> inspections = Ebean.find(ExamInspection.class)
    			.fetch("exam", "name", new FetchConfig().query())
    			.findList();

    	// select only exam.name  works
    	// but Json reflection will invoke lazy loading 
    	// https://groups.google.com/forum/#!topic/play-framework/xVDmm4hQqfY
    	
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
    
}
