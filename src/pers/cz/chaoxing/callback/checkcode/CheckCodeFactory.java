package pers.cz.chaoxing.callback.checkcode;

import pers.cz.chaoxing.callback.checkcode.impl.*;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * a factory to create singleton checkCodeCallBack
 *
 * @author 橙子
 * @since 2018/11/21
 */
public enum CheckCodeFactory {
    LOGIN {
        @Override
        protected void createInstance() {
            this.callBack = new LoginCheckCodeJob("./checkCode-login.jpeg");
        }
    },
    CHAPTER {
        @Override
        protected void createInstance() {
            this.callBack = new ChapterCheckCodeJob("checkCode-chapter.jpeg");
        }
    },
    CUSTOM {
        @Override
        protected void createInstance() {
            this.callBack = new CustomCheckCodeJob("./checkCode-custom.jpeg");
        }
    },
    HOMEWORK {
        @Override
        protected void createInstance() {
            this.callBack = new HomeworkCheckCodeJob("./checkCode-homework.jpeg");
        }
    },
    EXAM {
        @Override
        protected void createInstance() {
            this.callBack = new ExamCheckCodeJob("./checkCode-exam.jpeg");
        }
    };

    protected CheckCodeJobModel callBack;

    CheckCodeFactory() {
        createInstance();
    }

    @SuppressWarnings("unchecked")
    public <T extends CheckCodeCallBack> T get() {
        return (T) this.callBack;
    }

    protected abstract void createInstance();

    public static void setLock(ReadWriteLock lock) {
        for (CheckCodeFactory factory : CheckCodeFactory.values())
            factory.callBack.setLock(lock);
    }

    static {
        CheckCodeFactory.setLock(new ReentrantReadWriteLock());
    }
}
