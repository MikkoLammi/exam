package models;


import io.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ExamExecutionType extends Model {

    public enum Type { PRIVATE, PUBLIC, MATURITY, PRINTOUT }

    @Id
    private Integer id;
    private String type;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
