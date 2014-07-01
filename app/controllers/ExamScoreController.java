package controllers;

import Exceptions.MalformedDataException;
import com.fasterxml.jackson.databind.JsonNode;
import models.dto.ExamScore;
import models.dto.InfoMessage;
import play.Play;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by avainik on 30.6.2014.
 */
public class ExamScoreController extends SitnetController {

    private final static String PATH_PREFIX = Play.application().path().getAbsolutePath() + "/app/models/test/";

    public static Result examScoreStatic(String code) throws MalformedDataException {

//        CourseUnitInfo info = bindForm(CourseUnitInfo.class);

        String info = null;
        try {
            info = readFile(PATH_PREFIX + code +".json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        JsonNode node = Json.parse(info);

/*        {
                "creditType": "osasuoritus",
                "credits": 5,
                "identifier": "xxxx",
                "courseImplementation": "2/2014",
                "courseUnitCode": "KANS2532",
                "courseUnitLevel": "syventävä",
                "courseUnitType": "viestintä",
                "creditLanguage": "fi",
                "examDate": 1401267695935,
                "ExamScore": {
                    "examScore": 25
                },
                "gradeScale": "viisiportainen",
                "lecturer": "Maika Opettaja",
                "lecturerId": "maikaope@funet.fi",
                "student": "Olli Opiskelija",
                "studentGrade": "4",
                "studentId": "olliop@funet.fi",
                "date": 1401267695935
        }*/


        ExamScore score = new ExamScore();
        score.setCreditType("osasuoritus");
        score.setCredits(node.findValue("credits").asText());
        score.setIdentifier("xxxxxxx");        // TODO: should be identifier
        score.setCourseImplementation(node.findValue("courseImplementation").asText());
        score.setCourseUnitCode(node.findValue("courseUnitCode").asText());
        score.setCourseUnitLevel(node.findValue("courseUnitLevel").asText());
        score.setCourseUnitType(node.findValue("courseUnitType").asText());
        score.setCreditLanguage(node.findValue("creditsLanguage").findValue("name").asText());
        score.setExamDate(new Timestamp(new Date().getTime()).toString());

        List<String> examScore = new ArrayList<String>();
        examScore.add(new String("25"));
        examScore.add(new String("13"));
        score.setExamScore(examScore);

        score.setGradeScale(node.findValue("gradeScale").asText());
        score.setLecturer(node.findValue("lecturer").findValue("name").asText());
        score.setLecturerId("maikaope@funet.fi");
        score.setStudent("Sauli Student");
        score.setStudentGrade("4");
        score.setStudentId("1961863");       // TODO: should be username
        score.setStudent("saulistu@funet.fi");
        score.setDate(new Timestamp(new Date().getTime()).toString());


        return ok(Json.toJson(score));
    }


    public static Result examScoreDynamic() throws MalformedDataException {

        DynamicForm info = Form.form().bindFromRequest();
        InfoMessage infox = bindForm(InfoMessage.class);

        String asd1 = info.get("status");
        String asd2 = info.get("description");



        ExamScore score = new ExamScore();
        score.setCreditType(info.get("courseUnitType"));
        score.setCredits(info.get("credits"));
        score.setIdentifier(info.get("identifier"));
        score.setCourseImplementation(info.get("courseImplementation"));
        score.setCourseUnitCode(info.get("courseUnitCode"));
        score.setCourseUnitLevel(info.get("courseUnitLevel"));
        score.setCourseUnitType(info.get("courseUnitType"));
        score.setCreditLanguage(info.field("creditsLanguage").sub("name").toString());
        score.setExamDate(new Timestamp(new Date().getTime()).toString());

        List<String> examScore = new ArrayList<String>();
        examScore.add(new String("25"));
        examScore.add(new String("13"));
        score.setExamScore(examScore);

        score.setGradeScale(info.get("gradeScale"));
        score.setLecturer(info.field("lecturer").sub("name").toString());
        score.setLecturerId("maikaope@funet.fi");
        score.setStudent("Sauli Student");
        score.setStudentGrade("4");
        score.setStudentId("1961863");       // TODO: should be username
        score.setStudent("saulistu@funet.fi");
        score.setDate(new Timestamp(new Date().getTime()).toString());

/*    {
"courseImplementation" : {
  },
   "credits" : "5",
   "creditsLanguage" : {
    "name" : "fi"
  },
   "gradeScale" : {
    "name" : "0-5"
  },
   "lecturer" : {
    "name" : "Piiparinen Senni Annika"
  },
   "lecturerResponsible" : {
  },
   "institutionName" : "Itä-Suomen yliopisto",
   "department" : [
   {
     "name" : "Metsätieteet (K)"
   },
   {
     "name" : "Metsätieteet (J)"
   }
  ],
   "degreeProgramme" : {
    "name" : "Metsätiede"
  },
   "campus" : [
   {
     "name" : "Kuopio"
   },
   {
     "name" : "Joensuu"
   }
  ],
   "startDate" : "30012013",
   "courseMaterial" : {
    "name" : "Sustainable use of forest biomass for energy a synthesis with focus on the Baltic and Nordic region"
  }
 },
  "status" : "OK",
  "description" : "

    }*/
        return ok(Json.toJson(score));
    }
    static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }
}
