package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizConfig;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.homework.HomeworkTaskData;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.StringUtil;

import java.util.*;

public class HomeworkTask extends TaskModel<HomeworkTaskData, HomeworkQuizData> {
    private final QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo;

    public HomeworkTask(TaskInfo<HomeworkTaskData> taskInfo, HomeworkTaskData attachment, QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo, String url) {
        super(taskInfo, attachment, url);
        this.homeworkQuizInfo = homeworkQuizInfo;
        this.taskName = this.attachment.getProperty().getTitle();
    }

    @Override
    public void doTask() throws Exception {
        threadPrintln("thread_homework_start", this.taskName);
        startRefreshTask();
        Map<HomeworkQuizData, List<OptionInfo>> answers = getAnswers(this.homeworkQuizInfo);
        control.checkState(this);
        if (hasFail) {
            if (storeQuestion(answers))
                answers.entrySet().stream()
                        .filter(entry -> !entry.getValue().isEmpty())
                        .forEach(entry -> threadPrintln(
                                "thread_homework_store_success", new Object[]{this.taskName},
                                entry.getKey().getDescription(),
                                StringUtil.join(entry.getValue())
                        ));
        } else {
            this.homeworkQuizInfo.setPassed(answerQuestion(answers));
            if (this.homeworkQuizInfo.isPassed())
                answers.forEach((key, value) -> threadPrintln(
                        "thread_homework_answer_success", new Object[]{this.taskName},
                        key.getDescription(),
                        StringUtil.join(value)
                ));

        }
        if (control.isSleep())
            Thread.sleep(3 * 60 * 1000);
        threadPrintln(this.homeworkQuizInfo.isPassed() ?
                        "thread_homework_answer_finish" : "thread_homework_store_finish",
                this.taskName);
    }

    @Override
    protected Map<HomeworkQuizData, List<OptionInfo>> getAnswers(QuizInfo<HomeworkQuizData, ?> quizInfo) {
        Map<HomeworkQuizData, List<OptionInfo>> questions = new HashMap<>();
        Arrays.stream(quizInfo.getDatas()).forEach(quizData -> {
            CXUtil.getQuizAnswer(quizData).forEach(optionInfo -> questions.computeIfAbsent(quizData, key -> new ArrayList<>()).add(optionInfo));
            if (!questions.containsKey(quizData)) {
                threadPrintln("thread_homework_answer_failure", new Object[]{this.taskName},
                        quizData.toString());
                hasFail = !completeAnswer(questions, quizData);
            }
            if (questions.containsKey(quizData))
                quizData.setAnswered(false);
            else
                questions.put(quizData, new ArrayList<>());
        });
        return questions;
    }

    @Override
    protected boolean storeQuestion(Map<HomeworkQuizData, List<OptionInfo>> answers) throws WrongAccountException {
        boolean isPassed = Try.ever(() -> CXUtil.storeHomeworkQuiz(url, this.homeworkQuizInfo.getDefaults(), answers), checkCodeCallBack, this.homeworkQuizInfo.getDefaults(), this.taskInfo.getDefaults().getKnowledgeid(), this.homeworkQuizInfo.getDefaults().getClassId(), this.homeworkQuizInfo.getDefaults().getCourseId());
        if (isPassed)
            answers.keySet().forEach(homeworkQuizData -> homeworkQuizData.setAnswered(true));
        return isPassed;
    }

    @Override
    protected boolean answerQuestion(Map<HomeworkQuizData, List<OptionInfo>> answers) throws WrongAccountException {
        boolean isPassed = Try.ever(() -> CXUtil.answerHomeworkQuiz(url, this.homeworkQuizInfo.getDefaults(), answers), checkCodeCallBack, this.homeworkQuizInfo.getDefaults(), this.homeworkQuizInfo.getDefaults().getKnowledgeid(), this.homeworkQuizInfo.getDefaults().getClassId(), this.homeworkQuizInfo.getDefaults().getCourseId());
        if (isPassed)
            answers.keySet().forEach(homeworkQuizData -> homeworkQuizData.setAnswered(true));
        return isPassed;
    }
}
