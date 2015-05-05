package models.calendar;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import models.ExamRoom;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

@Entity
@DiscriminatorValue("ExceptionWorkingHours")
public class ExceptionWorkingHours extends Model {

    @Version
    @Temporal(TemporalType.TIMESTAMP)
    protected Date ebeanTimestamp;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mmZ")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mmZ")
    private Date endDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mmZ")
    private Date startTime;

    private int startTimeTimezoneOffset;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mmZ")
    private Date endTime;

    private int endTimeTimezoneOffset;

    @ManyToOne
    @JsonBackReference
    private ExamRoom room;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public ExamRoom getRoom() {
        return room;
    }

    public void setRoom(ExamRoom room) {
        this.room = room;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public int getStartTimeTimezoneOffset() {
        return startTimeTimezoneOffset;
    }

    public void setStartTimeTimezoneOffset(int startTimeTimezoneOffset) {
        this.startTimeTimezoneOffset = startTimeTimezoneOffset;
    }

    public int getEndTimeTimezoneOffset() {
        return endTimeTimezoneOffset;
    }

    public void setEndTimeTimezoneOffset(int endTimeTimezoneOffset) {
        this.endTimeTimezoneOffset = endTimeTimezoneOffset;
    }
}
