var ExamEditor = function () {

    this.setExamName = function (name) {
        return element(by.model('newExam.name')).sendKeys(name);
    };

    this.findSection = function (index) {
        return element.all(by.repeater('section in newExam.examSections')).get(index);
    };

    this.findQuestion = function (index) {
        return element.all(by.repeater('question in visibleQuestions')).get(index);
    };

    this.findExecutionType = function (index) {
        return element.all(by.repeater('x in range(1, 5)')).get(index);
    };
    
    this.setInstructions = function(instruction){
        return element(by.model('newExam.instruction')).sendKeys(instruction);
    };
    
    this.setSectionName = function (section, name) {
        return section.element(by.model('section.name'))
            .sendKeys(name);
    };
    
    this.dragQuestionFromLibrary = function (section, question) {
        var target = section.element(by.xpath('//ul[@droppable]'));
        return browser.actions().dragAndDrop(question, target).mouseUp().perform();
    };

    this.publishExam = function () {
        element(by.xpath('//button[@ng-click=\'saveAndPublishExam()\']')).click();
        var dialog = element(by.id('sitnet-dialog'));
        browser.wait(EC.visibilityOf(dialog), 5000);
        dialog.element(by.xpath('//button[@ng-click=\'ok()\']')).click();
        expect(browser.getCurrentUrl()).toMatch(/.+\/exams/);
    };

    this.previewExam = function () {
        element(by.xpath('//button[@ng-click=\'previewExam()\']')).click();
        //element(by.xpath('//button[@ng-click=\'setNextSection()\']')).click();
        expect(browser.getCurrentUrl()).toMatch(/.+\/preview/);
    };
};
module.exports = ExamEditor;