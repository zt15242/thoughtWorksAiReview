package other.tw.business.department;

import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Department;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import other.tw.business.util.NeoCrmUtils;

import java.util.ArrayList;
import java.util.List;

public class DepartmentSelector {

    public List<Department> getAllDepartment() throws ApiEntityServiceException {

        String sql = "SELECT id, departCode FROM department";

        return NeoCrmUtils.query(sql, Department.class,"getAllDepartment");
    }
}
