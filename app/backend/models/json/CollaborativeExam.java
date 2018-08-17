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

package backend.models.json;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;

import backend.models.Exam;
import backend.models.ExamEnrolment;
import backend.models.base.GeneratedIdentityModel;
import backend.util.JsonDeserializer;

@Entity
public class CollaborativeExam extends GeneratedIdentityModel {

    @Column
    private String externalRef; // REFERENCE TO EXAM ELSEWHERE

    @Column
    private String revision; // REFERENCE TO EXAM REVISION ELSEWHERE

    @Temporal(TemporalType.TIMESTAMP)
    private DateTime created;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "collaborativeExam")
    @JsonManagedReference
    private List<ExamEnrolment> examEnrolments;

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public List<ExamEnrolment> getExamEnrolments() {
        return examEnrolments;
    }

    public void setExamEnrolments(List<ExamEnrolment> examEnrolments) {
        this.examEnrolments = examEnrolments;
    }

    @Transient
    public Exam getExam(JsonNode node) {
        JsonNode formatted = ((ObjectNode) node).put("id", id);
        return JsonDeserializer.deserialize(Exam.class, formatted);
    }


}
