package models.dto;

public class Record {

    private String StudentId;
    private String Student;
    private String Identifier;
    private String CourseUnitCode;
    private String ExamDate;
    private String Credits;
    private String CreditLanguage;
    private String StudentCredit;
    private String GradeScale;
    private String ExamScore;
    private String CourseUnitLevel;
    private String CourseUnitType;
    private String CreditType;
    private String Lecturer;
    private String LecturerId;
    private String Date;
    private String CourseImplementation;

    public String getCourseImplementation() {
        return CourseImplementation;
    }

    public void setCourseImplementation(String courseImplementation) {
        CourseImplementation = courseImplementation;
    }

    public String getCourseUnitCode() {
        return CourseUnitCode;
    }

    public void setCourseUnitCode(String courseUnitCode) {
        CourseUnitCode = courseUnitCode;
    }

    public String getCourseUnitLevel() {
        return CourseUnitLevel;
    }

    public void setCourseUnitLevel(String courseUnitLevel) {
        CourseUnitLevel = courseUnitLevel;
    }

    public String getCourseUnitType() {
        return CourseUnitType;
    }

    public void setCourseUnitType(String courseUnitType) {
        CourseUnitType = courseUnitType;
    }

    public String getCreditLanguage() {
        return CreditLanguage;
    }

    public void setCreditLanguage(String creditLanguage) {
        CreditLanguage = creditLanguage;
    }

    public String getCredits() {
        return Credits;
    }

    public void setCredits(String credits) {
        Credits = credits;
    }

    public String getCreditType() {
        return CreditType;
    }

    public void setCreditType(String creditType) {
        CreditType = creditType;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public String getExamDate() {
        return ExamDate;
    }

    public void setExamDate(String examDate) {
        ExamDate = examDate;
    }

    public String getExamScore() {
        return ExamScore;
    }

    public void setExamScore(String examScore) {
        ExamScore = examScore;
    }

    public String getGradeScale() {
        return GradeScale;
    }

    public void setGradeScale(String gradeScale) {
        GradeScale = gradeScale;
    }

    public String getIdentifier() {
        return Identifier;
    }

    public void setIdentifier(String identifier) {
        Identifier = identifier;
    }

    public String getLecturer() {
        return Lecturer;
    }

    public void setLecturer(String lecturer) {
        Lecturer = lecturer;
    }

    public String getLecturerId() {
        return LecturerId;
    }

    public void setLecturerId(String lecturerId) {
        LecturerId = lecturerId;
    }

    public String getStudent() {
        return Student;
    }

    public void setStudent(String student) {
        Student = student;
    }

    public String getStudentCredit() {
        return StudentCredit;
    }

    public void setStudentCredit(String studentCredit) {
        StudentCredit = studentCredit;
    }

    public String getStudentId() {
        return StudentId;
    }

    public void setStudentId(String studentId) {
        StudentId = studentId;
    }
}