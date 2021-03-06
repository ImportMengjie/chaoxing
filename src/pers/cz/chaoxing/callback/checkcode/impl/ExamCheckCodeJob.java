package pers.cz.chaoxing.callback.checkcode.impl;

import com.alibaba.fastjson.JSON;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.StatusCodes;
import pers.cz.chaoxing.callback.checkcode.CheckCodeCallBack;
import pers.cz.chaoxing.callback.checkcode.CheckCodeData;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.io.StringUtil;
import pers.cz.chaoxing.util.net.ApiURL;
import pers.cz.chaoxing.util.net.NetUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ExamCheckCodeJob extends CheckCodeJobModel implements CheckCodeCallBack<CheckCodeData> {
    private CheckCodeData checkCodeData;
    private String id;
    private String classId;
    private String courseId;
    private String callback;

    public ExamCheckCodeJob(String checkCodePath) {
        super(checkCodePath);
        this.checkCodeData = new CheckCodeData(true);
    }

    @Override
    public CheckCodeData onCheckCode(String... param) {
        if (lock.writeLock().tryLock()) {
            receiveURL = param[0];
            baseURL = NetUtil.getOriginal(receiveURL);
            try {
                do {
                    if (!receiveCheckCode(checkCodePath))
                        break;
                    if (!readCheckCode(checkCodePath))
                        IOUtil.println("check_code_image_path", checkCodePath);
                    checkCodeData = setCheckCode(IOUtil.printAndNext("input_check_code"), param[1], param[2], param[3], param[4]);
                } while (!checkCodeData.isStatus());
            } finally {
                lock.writeLock().unlock();
            }
        }
        lock.readLock().lock();
        try {
            return checkCodeData;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected boolean receiveCheckCode(String path) {
        do {
            RawResponse response = Try.ever(() -> NetUtil.get(receiveURL, 0), CheckCodeFactory.CUSTOM.get());
            if (StatusCodes.NOT_FOUND == response.getStatusCode())
                return false;
            response.writeToFile(path);
            try (Stream<String> lines = Files.lines(Paths.get(path), StandardCharsets.ISO_8859_1)) {
                if (lines.anyMatch(line -> line.contains("<html")))
                    CheckCodeFactory.CUSTOM.get().onCheckCode(receiveURL);
                else
                    break;
            } catch (IOException ignored) {
            }
        } while (true);
        return true;
    }

    private CheckCodeData setCheckCode(String checkCode, String id, String classId, String courseId, String callback) {
        this.id = id;
        this.classId = classId;
        this.courseId = courseId;
        this.callback = callback;
        return sendCheckCode(checkCode);
    }

    @Override
    protected CheckCodeData sendCheckCode(String checkCode) {
        RawResponse response = Try.ever(() -> NetUtil.get(ApiURL.EXAM_CHECK_CODE_SEND.buildURL(baseURL,
                classId,
                courseId,
                id,
                callback,
                checkCode
        ), 0), CheckCodeFactory.CUSTOM.get());
        String jsonStr = StringUtil.subStringBetweenFirst(response.readToText(), callback + "(", ")");
        CheckCodeData checkCodeData = JSON.parseObject(jsonStr, CheckCodeData.class);
        checkCodeData.setCode(checkCode);
        return checkCodeData;
    }
}
