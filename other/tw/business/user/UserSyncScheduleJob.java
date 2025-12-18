package other.tw.business.user;

import com.rkhd.platform.sdk.ScheduleJob;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.param.ScheduleJobParam;
import com.rkhd.platform.sdk.service.BatchJobProService;
import com.rkhd.platform.sdk.task.param.PrepareParam;
import other.tw.business.department.DepartmentSelector;

import java.util.Arrays;

public class UserSyncScheduleJob implements ScheduleJob {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final DepartmentSelector departmentSelector = new DepartmentSelector();

    @Override
    public void execute(ScheduleJobParam scheduleJobParam) {
        LOGGER.info("定时同步用户开始");
        try {
            syncUser(50);
            updateUsersManager();
        } catch(Exception e) {
            LOGGER.error("定时同步用户 Exception message =>" + e.getMessage());
            LOGGER.error("定时同步用户 Exception =>" + Arrays.toString(e.getStackTrace()));
        }
        LOGGER.info("定时同步用户结束");
    }

    public void syncUser(int pageSize) throws BatchJobException {
        LOGGER.error("同步用户开始");
        PrepareParam param = new PrepareParam();
        param.set("pageSize", String.valueOf(pageSize));
        BatchJobProService.instance().addBatchJob(UserSyncBatchJob.class, 10, param);
        LOGGER.error("同步用户结束");
    }

    public void updateUsersManager() throws BatchJobException {
        LOGGER.error("更新用户直属上级开始");
        BatchJobProService.instance().addBatchJob(UserUpdateManagerBatchJob.class, 500, new PrepareParam());
        LOGGER.error("更新用户直属上级结束");
    }
}
