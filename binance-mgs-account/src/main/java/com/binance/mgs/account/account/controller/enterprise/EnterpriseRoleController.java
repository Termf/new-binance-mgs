package com.binance.mgs.account.account.controller.enterprise;

import com.binance.accountsubuser.core.annotation.RolePermissionCheck;
import com.binance.accountsubuser.core.helper.RolePermissionCheckHelper;
import com.binance.accountsubuser.vo.constants.Constant;
import com.binance.accountsubuser.vo.enterprise.EnterpriseRoleAcctVo;
import com.binance.accountsubuser.vo.enterprise.EnterpriseRoleVo;
import com.binance.accountsubuser.vo.enterprise.EnterpriseUserAndRoleBindingVo;
import com.binance.accountsubuser.vo.enterprise.response.CreateEnterpriseRoleAccountRes;
import com.binance.master.commons.SearchResult;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.vo.enterprise.CreateEnterpriseRoleAccountArg;
import com.binance.mgs.account.account.vo.enterprise.CreateEnterpriseRoleAccountRet;
import com.binance.mgs.account.account.vo.enterprise.DeleteEnterpriseRoleAccountArg;
import com.binance.mgs.account.account.vo.enterprise.EnterpriseRoleAcctRet;
import com.binance.mgs.account.account.vo.enterprise.QueryEnterpriseRoleUserArg;
import com.binance.mgs.account.account.vo.enterprise.QueryEnterpriseRoleUserByIdArg;
import com.binance.mgs.account.account.vo.enterprise.QueryRoleListrArg;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.service.EnterpriseRoleService;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dana.d
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/private/account/enterprise")
public class EnterpriseRoleController extends AccountBaseAction {

    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;

    @Value("${enterprise.role.user.action.limit.count:200}")
    private int enterpriseRoleUserActionCount;

    @Value("${enterprise.role.user.ddos.expire.time:3600}")
    private int enterpriseRoleUserDdosExpireTime;

    @Value("${enterprise.role.account.max.create.nums:100}")
    private Long enterpriseRoleAccountCreateLimit;

    @Autowired
    private EnterpriseRoleService enterpriseRoleService;


    @ApiOperation("角色账号信息列表")
    @PostMapping("/roleUser/roleUserList")
    @RolePermissionCheck(permissionNames = {Constant.EnterpriseBasePermission.CREATE_ROLE})
    public CommonRet<Map<String,Object>> roleUserList(@Validated @RequestBody QueryEnterpriseRoleUserArg arg) throws Exception {
        //登陆状态校验
        checkAndGetUserId();
        //获取当前登陆用户对应的企业ID（母账户）
        Long parentUserId = RolePermissionCheckHelper.getEnterpriseId();

        SearchResult<EnterpriseRoleAcctVo> searchResult = enterpriseRoleService.roleUserList(arg, parentUserId);
        List<EnterpriseRoleAcctVo> rows = searchResult.getRows();

        CommonRet<Map<String, Object>> commonPageRet = new CommonRet<>();
        Map<String, Object> map = new HashMap<>();
        List<EnterpriseRoleAcctRet> retList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(rows)) {
            for (EnterpriseRoleAcctVo row : rows) {
                EnterpriseRoleAcctRet enterpriseRoleAcctRet = CopyBeanUtils.fastCopy(row, EnterpriseRoleAcctRet.class);
                List<EnterpriseRoleVo> enterpriseRoleVos = row.getEnterpriseRoleVos();
                if (CollectionUtils.isNotEmpty(enterpriseRoleVos)) {
                    enterpriseRoleAcctRet.setSubUserBizType(enterpriseRoleVos.get(0).getRoleName());
                }
                retList.add(enterpriseRoleAcctRet);
            }
        }
        map.put("rows",retList);
        map.put("total",searchResult.getTotal());
        map.put("maxSubUserNum",enterpriseRoleAccountCreateLimit);
        commonPageRet.setData(map);
        return commonPageRet;
    }

    @ApiOperation("删除角色账号")
    @PostMapping("/roleUser/delete")
    @RolePermissionCheck(permissionNames = {Constant.EnterpriseBasePermission.CREATE_ROLE})
    public CommonRet<Integer> deleteUser(@Validated @RequestBody DeleteEnterpriseRoleAccountArg arg) throws Exception {

        //登陆状态校验
        Long currentUserId = checkAndGetUserId();
        //获取当前登陆用户对应的企业ID（母账户）
        Long parentUserId = RolePermissionCheckHelper.getEnterpriseId();
        Integer result = enterpriseRoleService.deleteRoleAccount(arg, currentUserId, parentUserId);
        return new CommonRet<>(result);
    }


    @ApiOperation(value = "注册角色账号")
    @PostMapping(value = "/roleUser/creation")
    @RolePermissionCheck(permissionNames = {Constant.EnterpriseBasePermission.CREATE_ROLE})
    public CommonRet<CreateEnterpriseRoleAccountRet> createRoleUser(@RequestBody @Validated CreateEnterpriseRoleAccountArg arg) throws Exception {
        //登陆状态校验
        Long currentUserId = checkAndGetUserId();
        //获取当前登陆用户对应的企业ID（母账户）
        Long parentUserId = RolePermissionCheckHelper.getEnterpriseId();
        // 校验邮箱格式
        if (StringUtils.isBlank(arg.getEmail()) || !timeOutRegexUtils.validateEmailForRegister(arg.getEmail())) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }
        if (ddosCacheSeviceHelper.subAccountActionCount(parentUserId, "createEnterpriseRoleUser", enterpriseRoleUserDdosExpireTime) > enterpriseRoleUserActionCount) {
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OVERLIMIT);
        }
        CreateEnterpriseRoleAccountRes enterpriseRoleAccount = enterpriseRoleService.createEnterpriseRoleAccount(arg, currentUserId, parentUserId);
        CreateEnterpriseRoleAccountRet createEnterpriseRoleAccountRet = null;
        if(enterpriseRoleAccount!=null){
            createEnterpriseRoleAccountRet = new CreateEnterpriseRoleAccountRet();
            createEnterpriseRoleAccountRet.setUserId(enterpriseRoleAccount.getRoleAccountUserId());
            createEnterpriseRoleAccountRet.setEmail(enterpriseRoleAccount.getEmail());
        }
        return new CommonRet<>(createEnterpriseRoleAccountRet);
    }


    @ApiOperation("角色列表")
    @PostMapping("/roleUser/roleList")
    public CommonRet<List<EnterpriseRoleVo>> roleList(@Validated @RequestBody QueryRoleListrArg arg) throws Exception {
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();
        List<EnterpriseRoleVo> resultList = enterpriseRoleService.roleList();
        return new CommonRet<>(resultList);
    }

    @ApiOperation("查询当前账号对应的角色")
    @PostMapping("/roleUser/getUserRoleByUserId")
    public CommonPageRet<EnterpriseUserAndRoleBindingVo> getUserRoleByUserId(@Validated @RequestBody QueryEnterpriseRoleUserByIdArg arg) throws Exception {
        //登陆状态校验
        Long userId = checkAndGetUserId();
        SearchResult<EnterpriseUserAndRoleBindingVo> searchResult = enterpriseRoleService.getEnterpriseUserAndRoleBinding(arg,userId);
        CommonPageRet<EnterpriseUserAndRoleBindingVo> commonPageRet = new CommonPageRet<>();
        commonPageRet.setTotal(searchResult.getTotal());
        commonPageRet.setData(searchResult.getRows());
        return commonPageRet;
    }
}
