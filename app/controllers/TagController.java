package controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.ExpressionList;
import models.Tag;
import models.User;
import models.questions.Question;
import play.libs.F;
import play.libs.Json;
import play.mvc.Result;
import util.AppUtil;

import java.util.List;

public class TagController extends BaseController {

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public Result listTags(F.Option<String> filter, F.Option<List<Long>> examIds, F.Option<List<Long>> courseIds, F.Option<List<Long>> sectionIds) {
        User user = getLoggedUser();
        ExpressionList<Tag> query = Ebean.find(Tag.class).where();
        if (!user.hasRole("ADMIN")) {
            query = query.where().eq("creator.id", user.getId());
        }
        if (filter.isDefined()) {
            String condition = String.format("%%%s%%", filter.get());
            query = query.ilike("name", condition);
        }
        if (examIds.isDefined() && !examIds.get().isEmpty()) {
            query = query.in("questions.children.examSectionQuestion.examSection.exam.id", examIds.get());
        }
        if (courseIds.isDefined() && !courseIds.get().isEmpty()) {
            query = query.in("questions.children.examSectionQuestion.examSection.exam.course.id", courseIds.get());
        }
        if (sectionIds.isDefined() && !sectionIds.get().isEmpty()) {
            query = query.in("questions.children.examSectionQuestion.examSection.id", sectionIds.get());
        }
        List<Tag> tags = query.orderBy("name").findList();
        return ok(Json.toJson(tags));
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public Result createTag() {
        Tag tag = bindForm(Tag.class);
        User user = getLoggedUser();
        AppUtil.setCreator(tag, user);
        AppUtil.setModifier(tag, user);
        // Save only if not already exists
        Tag existing = Ebean.find(Tag.class).where().eq("name", tag.getName())
                .eq("creator.id", tag.getCreator().getId()).findUnique();
        if (existing == null) {
            tag.save();
            return created(Json.toJson(tag));
        } else {
            return ok(Json.toJson(existing));
        }
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public Result deleteTag(Long tagId) {
        Tag tag = Ebean.find(Tag.class, tagId);
        if (tag == null) {
            return notFound();
        }
        tag.delete();
        return ok();
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public Result tagQuestion(Long tagId, Long questionId) {
        Tag tag = Ebean.find(Tag.class, tagId);
        Question question = Ebean.find(Question.class, questionId);
        if (tag == null || question == null) {
            return notFound();
        }
        if (question.getParent() != null) {
            return forbidden("Tagging is available only for prototype questions");
        }
        question.getTags().add(tag);
        question.update();
        return ok();
    }

    @Restrict({@Group("ADMIN"), @Group("TEACHER")})
    public Result untagQuestion(Long tagId, Long questionId) {
        Tag tag = Ebean.find(Tag.class, tagId);
        Question question = Ebean.find(Question.class, questionId);
        if (tag == null || question == null) {
            return notFound();
        }
        if (question.getParent() != null) {
            return forbidden("Tagging is available only for prototype questions");
        }
        question.getTags().remove(tag);
        question.update();

        return ok();
    }


}