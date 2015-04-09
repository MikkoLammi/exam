(function () {
    'use strict';
    angular.module("sitnet.resources")
        .factory("ExamRes", ['$resource', function ($resource) {
            return {
                exams: $resource("/exams/:id", 
                {
                	id: "@id"
                },
                {
                    "update": {method: "PUT"},
                    "remove": {method: "DELETE"}
                }),

                examowner: $resource("/exam/:eid/owner/:uid",
                {
                    eid: "@eid", uid: "@uid"
                },
                {
                    "insert": {method: "PUT"},
                    "remove": {method: "DELETE"}
                }),

                questions: $resource("/exams/:eid/section/:sid/question/:qid",
                {
                	eid: "@eid", sid: "@sid", qid: "@qid"
                },
                {
                    "remove": {method: "DELETE", params: { eid: "@eid" , sid: "@sid", qid: "@qid"}}
                }),
                sections: $resource("/exams/:eid/section/:sid",
                {
                    eid: "@eid", sid: "@sid"
                },
                {
                    "insert": {method: "POST", params: { eid: "@eid" , sid: "@sid"}},
                	"remove": {method: "DELETE", params: { eid: "@eid" , sid: "@sid"}},
                    "update": {method: "PUT", params: { eid: "@eid" , sid: "@sid"}}

                }),
                sectionquestions: $resource("/exams/:eid/section/:sid/:seq/question/:qid",
                {
                    eid: "@eid", sid: "@sid", seq: "@seq", qid: "@qid"
                },
                {
                    "insert": {method: "POST", params: { eid: "@eid" , sid: "@sid", seq: "@seq", qid: "@qid"}}

                }),
                sectionquestionsmultiple: $resource("/exams/:eid/section/:sid/:seq/questions",
                {
                    eid: "@eid", sid: "@sid", seq: "@seq", questions: "@questions"
                },
                {
                    "insert": {method: "POST", params: { eid: "@eid" , sid: "@sid", seq: "@seq", questions: "@questions"}}

                }),
                reordersection: $resource("/exams/:eid/section/:sid/:from/:to",
                {
                    eid: "@eid", sid: "@sid", from: "@from", to: "@to"
                },
                {
                    "update": {method: "PUT", params: { eid: "@eid" , sid: "@sid", from: "@from", to: "@to"}}
                }),
                clearsection: $resource("/clearsection/:sid",
                {
                    sid: "@sid"
                },
                {
                	"clear": {method: "DELETE", params: {sid: "@sid"}}
                }),

                course: $resource("/exams/:eid/course/:cid",
                {
                    eid: "@eid", sid: "@cid"
                },
                {
                    "update": {method: "PUT", params: { eid: "@eid" , cid: "@cid"}}
                }),

                courses: $resource("/courses/insert/:code",
                {
                    code: "@code"
                },
                {
                    "insert": {method: "POST", params: { code: "@code"}}
                }),
                reviewerExams: $resource("/reviewerexams"),
                reviewerExam: $resource("/reviewerexams/:eid", {eid: "@eid"}),
                finishedExams: $resource("/finishedexams"),
                draft: $resource("/draft"),
                review: $resource("/review/:id", {id: "@id"}, {"update": {method: "PUT"}}),
                examReviews: $resource("/reviews/:eid",{eid: "@eid", statuses: "@statuses"},
                    {"get": {method: "GET", params: { eid: "@eid", statuses: "@statuses" }}
                }),
                comment: $resource("/review/:eid/comment/:cid",
                {
                    id: "@eid", cid: "@cid"
                },
                {
                    "insert": {method: "POST", params: { eid: "@eid" }},
                    "update": {method: "PUT", params: { eid: "@eid" , sid: "@cid"}}
                }),
                inspections: $resource("/exam/:id/inspections",
                {
                    id: "@id"
                },
                {
                    "get": {method: "GET", isArray: true, params: { id: "@id" }}
                }),

                owners: $resource("/exam/:id/owners",
                    {
                        id: "@id"
                    },
                    {
                        "get": {method: "GET", isArray: true, params: { id: "@id" }}
                    }),

                inspection: $resource("/exams/:eid/inspector/:uid",
                {
                    eid: "@eid", uid: "@uid"
                },
                {
                    "insert": {method: "POST", params: { eid: "@eid" , uid: "@uid"}}
                }),

                inspectionReady: $resource("/exams/inspection/:id/:ready",
                {
                    id: "@id", ready: "@ready"
                },
                {
                    "update": {method: "PUT", params: { id: "@id" , ready: "@ready"}}
                }),


                localInspection: $resource("/exams/localInspection/:eid/:uid",
                {
                    eid: "@eid", uid: "@uid"
                },
                {
                    "insert": {method: "POST", params: { eid: "@eid" , uid: "@uid"}}
                }),

                inspector: $resource("/exams/inspector/:id",
                {
                    id: "@id"
                },
                {
                    "remove": {method: "DELETE", params: { id: "@id"}}
                }),

                examEnrolments: $resource("/examenrolments/:eid",
                {
                    eid: "@eid"
                },
                {
                    "get": {method: "GET", params: { eid: "@eid" }}
                }),

                examParticipations: $resource("/examparticipations/:eid",
                {
                    eid: "@eid"
                },
                {
                    "get": {method: "GET", params: { eid: "@eid" }}
                }),
                examParticipationsOfUser: $resource("/examparticipations/:eid/:uid",
                {
                    eid: "@eid",
                    uid: "@uid"
                },
                {
                   "get": {method: "GET", params: { eid: "@eid", uid: "@uid"}}
                }),
                studentInfo: $resource("/review/info/:id",
                {
                    id: "@id"
                }),
                email: $resource("/email/inspection/:eid/:msg",
                {
                    eid: "@eid",
                    msg: "@msg"
                },
                {
                    inspection: {method: "POST", params: { eid: "@eid", msg: "@msg" }}
                }),

                saveRecord: $resource("/exam/record", null,
                {
                    "add": {method: "POST"}
                }),
                record: $resource("/exam/record/export/:id",
                    {
                        id: "@id"
                    },
                    {
                        "export": {method: "GET", params: { id: "@id"}}
                    }),
                language: $resource("/exam/:eid/language/:code",
                {
                    eid: "@eid",
                    code: "@code"
                },
                {
                    "add": {method: "PUT"}
                }),
                languages: $resource("/exam/:eid/languages",
                {
                    eid: "@eid"
                },
                {
                    "reset": {method: "DELETE"}
                }),
                examTypes: $resource("/examtypes"),
                gradeScales: $resource("/gradescales"),
                software: $resource("/exam/:eid/software/:sid",
                {
                    eid: "@eid",
                    sid: "@sid"
                },
                {
                    "add": {method: "PUT"}
                }),
                softwares: $resource("exam/:eid/software",
                {
                    eid: "@eid"
                },
                {
                    "reset": {method: "DELETE"}
                }),
                reservation: $resource("/exams/:eid/reservation",
                {
                    eid: "@eid"
                })
            };
        }]);
}());