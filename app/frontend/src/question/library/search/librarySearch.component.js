/*
 * Copyright (c) 2017 Exam Consortium
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

import angular from 'angular';

angular.module('app.question')
    .component('librarySearch', {
        template: require('./librarySearch.template.html'),
        bindings: {
            onUpdate: '&'
        },
        controller: ['$q', 'Library', 'Session',
            function ($q, Library, Session) {

                const vm = this;

                vm.$onInit = function () {
                    vm.limitations = {};
                    vm.filter = {};
                    vm.user = Session.getUser();

                    const storedData = Library.loadFilters('search');
                    if (storedData.filters) {
                        vm.exams = storedData.filters.exams || [];
                        vm.courses = storedData.filters.courses || [];
                        vm.tags = storedData.filters.tags || [];
                        vm.filter.text = storedData.filters.text;
                        query().then(function () {
                            if (vm.filter.text) {
                                vm.applyFreeSearchFilter();
                            } else {
                                vm.onUpdate({results: vm.questions});
                            }
                        });
                    } else {
                        vm.courses = [];
                        vm.exams = [];
                        vm.tags = [];
                        query().then(function () {
                            vm.onUpdate({results: vm.questions});
                        });
                    }
                };

                vm.applyFreeSearchFilter = function () {
                    const results = Library.applyFreeSearchFilter(vm.filter.text, vm.questions);
                    vm.onUpdate({results: results});
                    saveFilters();
                };

                vm.applyOwnerSearchFilter = function () {
                    const results = Library.applyOwnerSearchFilter(vm.filter.owner, vm.questions);
                    vm.onUpdate({results: results});
                    saveFilters();
                };

                const saveFilters = function () {
                    const filters = {
                        exams: vm.exams,
                        courses: vm.courses,
                        tags: vm.tags,
                        text: vm.filter.text
                    };
                    Library.storeFilters(filters, 'search');
                };

                const getCourseIds = function () {
                    return vm.courses.filter(function (course) {
                        return course && course.filtered;
                    }).map(function (course) {
                        return course.id;
                    });
                };

                const getExamIds = function () {
                    return vm.exams.filter(function (exam) {
                        return exam.filtered;
                    }).map(function (exam) {
                        return exam.id;
                    });
                };

                const getTagIds = function () {
                    return vm.tags.filter(function (tag) {
                        return !tag.isSectionTag && tag.filtered;
                    }).map(function (tag) {
                        return tag.id;
                    });
                };

                const getSectionIds = function () {
                    return vm.tags.filter(function (tag) {
                        return tag.isSectionTag && tag.filtered;
                    }).map(function (section) {
                        return section.id;
                    });
                };

                const query = function () {
                    const deferred = $q.defer();
                    Library.search(getExamIds(), getCourseIds(), getTagIds(), getSectionIds())
                        .then(
                            function (questions) {
                                vm.questions = questions;
                                saveFilters();
                                deferred.resolve();
                            }
                        );
                    return deferred.promise;
                };


                const union = function (filtered, tags) {
                    const filteredIds = filtered.map(function (tag) {
                        return tag.id;
                    });
                    return filtered.concat(tags.filter(function (tag) {
                        return filteredIds.indexOf(tag.id) === -1;
                    }));
                };

                vm.listCourses = function () {
                    vm.courses = vm.courses.filter(function (course) {
                        return course.filtered;
                    });
                    const deferred = $q.defer();
                    Library.courseApi.query({
                        examIds: getExamIds(),
                        tagIds: getTagIds(),
                        sectionIds: getSectionIds()
                    }, function (data) {
                        vm.courses = union(vm.courses, data);
                        deferred.resolve();
                    });
                    return deferred.promise;
                };

                vm.listExams = function () {
                    vm.exams = vm.exams.filter(function (exam) {
                        return exam.filtered;
                    });
                    const deferred = $q.defer();
                    Library.examApi.query({
                        courseIds: getCourseIds(),
                        sectionIds: getSectionIds(),
                        tagIds: getTagIds()
                    }, function (data) {
                        vm.exams = union(vm.exams, data);
                        deferred.resolve();
                    });
                    return deferred.promise;
                };

                const doListTags = function (sections) {
                    const examIds = getExamIds();
                    const courseIds = getCourseIds();
                    Library.tagApi.query({
                        examIds: examIds,
                        courseIds: courseIds,
                        sectionIds: getSectionIds()
                    }, function (data) {
                        vm.tags = union(vm.tags, data);
                        let examSections = [];
                        vm.exams.filter(function (e) {
                            const examMatch = examIds.length === 0 || examIds.indexOf(e.id) > -1;
                            const courseMatch = courseIds.length === 0 || courseIds.indexOf(e.course.id) > -1;
                            return examMatch && courseMatch;
                        }).forEach(function (exam) {
                            examSections = examSections.concat(exam.examSections.filter(function (es) {
                                return es.name;
                            }).map(function (section) {
                                section.isSectionTag = true;
                                return section;
                            }));
                        });
                        vm.tags = vm.tags.concat(union(sections, examSections));
                    });
                };

                vm.listTags = function () {
                    vm.tags = vm.tags.filter(function (tag) {
                        return tag.filtered && !tag.isSectionTag;
                    });
                    const sections = vm.tags.filter(function (tag) {
                        return tag.filtered && tag.isSectionTag;
                    });
                    if (getExamIds().length === 0) {
                        vm.listExams().then(function () {
                            return doListTags(sections);
                        });
                    } else {
                        return doListTags(sections);
                    }
                };

                vm.getTags = function () {
                    const courses = vm.courses.filter(function (course) {
                        return course && course.filtered;
                    });
                    const exams = vm.exams.filter(function (exam) {
                        return exam.filtered;
                    });
                    const tags = vm.tags.filter(function (tag) {
                        return tag.filtered;
                    });
                    return courses.concat(exams).concat(tags);
                };

                vm.applyFilter = function (tag) {
                    tag.filtered = !tag.filtered;
                    query().then(function () {
                        vm.applyFreeSearchFilter();
                    });
                };

            }
        ]
    });
