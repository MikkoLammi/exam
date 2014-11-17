package controllers;

import Exceptions.MalformedDataException;
import Exceptions.UnauthorizedAccessException;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigFactory;
import models.*;
import org.joda.time.DateTime;
import play.Logger;
import play.cache.Cache;
import play.libs.Json;
import play.mvc.Result;
import util.SitnetUtil;

import java.nio.charset.Charset;
import java.util.*;

public class SessionController extends SitnetController {

    public static Result login() throws MalformedDataException, UnauthorizedAccessException {

        User user;

        String loginType = ConfigFactory.load().getString("sitnet.login");
        if (loginType.equals("DEBUG")) {
            Credentials credentials = bindForm(Credentials.class);
            Logger.debug("User login with username: {} and password: ***", credentials.getUsername() + "@funet.fi");
            if (credentials.getPassword() == null || credentials.getUsername() == null) {
                return unauthorized("sitnet_error_unauthenticated");
            }

            String md5psswd = SitnetUtil.encodeMD5(credentials.getPassword());
            user = Ebean.find(User.class)
                    .select("id, eppn, email, firstName, lastName, userLanguage")
                    .where().eq("eppn", credentials.getUsername() + "@funet.fi")
                    .eq("password", md5psswd).findUnique();

            if (user == null) {
                return unauthorized("sitnet_error_unauthenticated");
            }
        } else {
            if (loginType.equals("HAKA")) {
                String eppn = request().getHeader("eppn");

                user = Ebean.find(User.class)
                        .where()
                        .eq("eppn", eppn)
                        .findUnique();


                if (user != null) {

                    // User already exist, but we still need to update some information (all of it=)
                    if (request().getHeader("schacPersonalUniqueCode") == null) {
                        user.setUserIdentifier("");
                    } else {
                        user.setUserIdentifier(request().getHeader("schacPersonalUniqueCode"));
                    }

                    String email = request().getHeader("mail");
                    user.setEmail(email);

                    user.save();

                    // TODO: should save MOST IMPORTANT attributes only, not all
//                List<HakaAttribute> attrs = new ArrayList<HakaAttribute>();
//
//                for (Map.Entry<String,String[]> entry : attributes.entrySet()) {
//
//                    String key = entry.getKey();
//                    String[] value = entry.getValue();
//
//                    HakaAttribute attr = new HakaAttribute();
//                    attr.setKey(key);
//                    if (value.length > 1) {
//
//                        // TODO This might bite our arse at some point.
//                        attr.setValue(Arrays.toString(value));
//                    } else {
//                        attr.setValue(value[0]);
//                    }
//                    attrs.add(attr);
//                }
//
//                user.setAttributes(attrs);
//                user.save();

                } else {
                    // First login -> create it
                    user = new User();

//                List<HakaAttribute> attrs = new ArrayList<HakaAttribute>();
//
//                for (Map.Entry<String,String[]> entry : attributes.entrySet()) {
//
//                    String key = entry.getKey();
//                    String[] value = entry.getValue();
//
//                    HakaAttribute attr = new HakaAttribute();
//                    attr.setKey(key);
//                    if (value.length > 1) {
//
//                        // TODO This might bite our arse at some point.
//                        attr.setValue(Arrays.toString(value));
//                    } else {
//                        attr.setValue(value[0]);
//                    }
//                    attrs.add(attr);
//                }
//                user.setAttributes(attrs);

                    user.setEppn(eppn);

                    if (request().getHeader("schacPersonalUniqueCode") == null) {
                        user.setUserIdentifier("");
                    } else {
                        user.setUserIdentifier(request().getHeader("schacPersonalUniqueCode"));
                    }

                    String email = request().getHeader("mail");
                    user.setEmail(email);
                    user.setLastName(toUtf8(request().getHeader("sn")));
                    user.setFirstName(toUtf8(request().getHeader("displayName")));

                    String language = request().getHeader("preferredLanguage");
                    if (language != null && !language.isEmpty()) {
                        user.getUserLanguage().setNativeLanguageCode(language);
                        user.getUserLanguage().setUILanguageCode(language);
                    } else {
                        UserLanguage lang = Ebean.find(UserLanguage.class)
                                .where()
                                .eq("nativeLanguageCode", "en")
                                .findUnique();

                        user.setUserLanguage(lang);
                    }

                    String shibRole = request().getHeader("unscoped-affiliation");
                    Logger.debug("unscoped-affiliation: " + shibRole);
                    SitnetRole role = (SitnetRole) getRole(shibRole);
                    if (role == null) {
                        return notFound("sitnet_error_role_not_found " + shibRole);
                    } else {
                        user.getRoles().add(role);
                    }

                    user.save();
                }
            } else {
                // Login type not supported
                return badRequest();
            }
        }

        // User exists in the system -> log in

        String token;

        if (loginType.equals("HAKA")) {
            token = request().getHeader("Shib-Session-ID");
        } else {
            token = UUID.randomUUID().toString();
        }

        Session session = new Session();
        session.setSince(DateTime.now());
        session.setUserId(user.getId());
        session.setValid(true);
        Cache.set(SITNET_CACHE_KEY + token, session);
        ObjectNode result = Json.newObject();
        result.put("id", user.getId());
        result.put("token", token);
        result.put("firstname", user.getFirstName());
        result.put("lastname", user.getLastName());
        result.put("roles", Json.toJson(user.getRoles()));
        result.put("hasAcceptedUserAgreament", user.isHasAcceptedUserAgreament());
        response().setCookie("XSRF-TOKEN", SitnetUtil.encodeMD5(user.getUserIdentifier()));
        return ok(result);
    }

    // prints HAKA attributes, used for debugging
    @Restrict({@Group("TEACHER"), @Group("ADMIN"), @Group("STUDENT")})
    public static Result getAttributes() {

        Map<String, String[]> attributes = request().headers();
        String output = "";

        for (Map.Entry<String, String[]> entry : attributes.entrySet()) {

            String key = entry.getKey();
            String[] value = entry.getValue();

            output += key + "\t";
            output += Arrays.toString(value);
            output += "\n";
        }

        return ok(output);
    }

    static private Role getRole(String affiliation) {

        Map<String, List<String>> roles = getRoles();
        String roleName = null;
        if (roles.get("STUDENT").contains(affiliation)) {
            roleName = "STUDENT";
        } else if (roles.get("ADMIN").contains(affiliation)) {
            roleName = "ADMIN";
        } else if (roles.get("TEACHER").contains(affiliation)) {
            roleName = "TEACHER";
        }
        return roleName == null ? null : Ebean.find(SitnetRole.class)
                .where()
                .eq("name", roleName)
                .findUnique();
    }

    static private Map<String, List<String>> getRoles() {
        String[] students = ConfigFactory.load().getString("sitnet.roles.student").split(",");
        String[] teachers = ConfigFactory.load().getString("sitnet.roles.teacher").split(",");
        String[] admins = ConfigFactory.load().getString("sitnet.roles.admin").split(",");

        Map<String, List<String>> roles = new HashMap<>();

        List<String> studentRoles = new ArrayList<>();
        for (int i = 0; i < students.length; i++) {
            studentRoles.add(students[i].trim());
        }
        roles.put("STUDENT", studentRoles);

        List<String> teacherRoles = new ArrayList<>();
        for (int i = 0; i < teachers.length; i++) {
            teacherRoles.add(teachers[i].trim());
        }
        roles.put("TEACHER", teacherRoles);

        List<String> adminRoles = new ArrayList<>();
        for (int i = 0; i < admins.length; i++) {
            adminRoles.add(admins[i].trim());
        }
        roles.put("ADMIN", adminRoles);

        return roles;
    }

    public static Result logout() {
        String token = request().getHeader(SITNET_TOKEN_HEADER_KEY);
        String key = SITNET_CACHE_KEY + token;
        String loginType = ConfigFactory.load().getString("sitnet.login");
        if (loginType.equals("HAKA")) {
            Session session = (Session) Cache.get(key);
            if (session == null) {
                return ok();
            }
            session.setValid(false);
            Cache.set(key, session);
        } else {
            Cache.remove(key);
        }
        response().discardCookie("XSRF-TOKEN");
        return ok();
    }

    @Restrict({@Group("TEACHER"), @Group("ADMIN"), @Group("STUDENT")})
    public static Result ping() {
        return ok("pong");
    }

    private static String toUtf8(String src) {
        String latin = new String(src.getBytes(), Charset.forName("ISO-8859-1"));
        return new String(latin.getBytes(), Charset.forName("UTF-8"));
    }


}
