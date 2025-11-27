package com.evcharge.entity.admin;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.rbac.*;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.*;

/**
 * 管理员基础表;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class AdminBaseEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 账号
     */
    public String account;
    /**
     * 姓氏
     */
    public String last_name;
    /**
     * 名字
     */
    public String first_name;
    /**
     * 性别：-1=未知，0=女，1=男
     */
    public int gender;

    /**
     * 职位
     */
    public String position;
    /**
     * 邮箱
     */
    public String email;
    /**
     * 联系电话
     */
    public String phone_num;
    /**
     * 头像
     */
    public String avatar;
    /**
     * 分享码
     */
    public String sharecode;
    /**
     * 个人介绍
     */
    public String introduction;
    /**
     * 状态：0=冻结，1=正常
     */
    public int status;
    /**
     * 备注
     */
    public String remark;  /**
     * 微信联系二维码
     */
    public String wechat_qr;
    /**
     * 创建IP
     */
    public String create_ip;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;
    /**
     * 真实姓名
     */
    public String actualName;
    /**
     * 身份证号码
     */
    public String identityNumber;

    //endregion

    public static AdminBaseEntity getInstance() {
        return new AdminBaseEntity();
    }

    /**
     * 生成一个Token
     *
     * @param admin_id
     * @return
     */
    public static String buildToken(long admin_id) {
        return common.md5(String.format("%s_%s_%s", admin_id, common.getUUID(), TimeUtil.getTimestamp()));
    }

    /**
     * 登录
     */
    public SyncResult login(String account, String password, UserAgent userAgent) {

        //region 限制IP登录错误最大次数
        String ip = HttpRequestUtil.getIP();
        int requestCount = DataService.getMainCache().getInt("Admin:Login:RequestCount:" + ip);
        int maxRequestCount = SysGlobalConfigEntity.getInt("Admin:Login:MaxRequestCount", 50);
        if (requestCount > maxRequestCount) return new SyncResult(1, "登录失败，限制IP次数达到最大值[88]");
        requestCount++;
        DataService.getMainCache().set("Admin:Login:RequestCount:" + ip, requestCount, 86400000);
        //endregion

        //region 判断错误密码次数
        int pwdErrorCount = DataService.getMainCache().getInt("Admin:Login:PwdErrorCount:" + account);
        int maxRequestPwdErrorCount = SysGlobalConfigEntity.getInt("Admin:Login:MaxRequestPwdErrorCount", 10);
        if (pwdErrorCount > maxRequestPwdErrorCount)
            return new SyncResult(6, "今日您输错密码已经达最大次数,请明天再试");
        //endregion

        AdminBaseEntity adminBaseEntity = new AdminBaseEntity();
        if (VerifyUtil.isEmail(account)) {
            adminBaseEntity = (AdminBaseEntity) adminBaseEntity.where("email", account);
        } else if (VerifyUtil.isPhone(account)) {
            adminBaseEntity = (AdminBaseEntity) adminBaseEntity.where("phone_num", account);
        } else {
            adminBaseEntity = (AdminBaseEntity) adminBaseEntity.where("account", account);
        }

        Map<String, Object> data = adminBaseEntity
                .field("a.*,b.password,b.password_secret")
                .alias("a")
                .join(AdminSecretEntity.getInstance().theTableName(), "b", "a.id = b.admin_id")
                .find();

        if (data.isEmpty()) return new SyncResult(3, "账号不存在");

        long admin_id = MapUtil.getLong(data, "id");
        String password_secret = MapUtil.getString(data, "password_secret");
        String password_inDB = MapUtil.getString(data, "password");

        String baseString = String.format("%s%s", common.md5(password), password_secret);
        String passwordEncryption = common.md5(baseString);
        if (!passwordEncryption.equals(password_inDB)) {
            pwdErrorCount++;
//            long expire = common.getUTCTime24();
            long expire = 86400000;
            DataService.getMainCache().set("Admin:Login:PwdErrorCount:" + account, pwdErrorCount, expire);
            return new SyncResult(5, "密码错误");
        }

        int status = MapUtil.getInt(data, "status");
        if (status != 1) return new SyncResult(6, "此账号不允许登入");

        String token = AdminBaseEntity.buildToken(admin_id);

        //region token缓存处理

        String OperatingSystemName = userAgent.getOperatingSystem().getName();
        String DeviceType = userAgent.getOperatingSystem().getDeviceType().getName();
        String Manufacturer = userAgent.getOperatingSystem().getManufacturer().name();
//        String Unique = common.md5(DeviceType + Manufacturer);
        String Unique = common.md5(common.getUUID());

        Map<String, Object> adminCacheData = new LinkedHashMap<>();
        Map<String, Object> cbdata = new LinkedHashMap<>();

        adminCacheData.put("id", admin_id);
        adminCacheData.put("account", data.get("account"));
        adminCacheData.put("last_name", data.get("last_name"));
        adminCacheData.put("first_name", data.get("first_name"));
        adminCacheData.put("gender", data.get("gender"));
        adminCacheData.put("email", data.get("email"));
        adminCacheData.put("phone_num", data.get("phone_num"));
        adminCacheData.put("avatar", data.get("avatar"));
        adminCacheData.put("remark", data.get("remark"));
        adminCacheData.put("sharecode", data.get("sharecode"));
        adminCacheData.put("OperatingSystemName", OperatingSystemName);
        adminCacheData.put("DeviceType", DeviceType);
        adminCacheData.put("Manufacturer", Manufacturer);
        adminCacheData.put("IP", HttpRequestUtil.getIP());

        Map<String, JSONObject> loginDevice = DataService.getMainCache().getMap("Admin:Device:" + admin_id);
        //region 20240405 查询用户角色组
        AdminToAUGroupEntity adminToAUGroupEntity = AdminToAUGroupEntity.getInstance().where("admin_id", admin_id).findEntity();

        if (adminToAUGroupEntity != null) {
            adminCacheData.put("group_id", adminToAUGroupEntity.group_id);
            cbdata.put("group_id", adminToAUGroupEntity.group_id);

        } else {
            adminCacheData.put("group_id", 0);
            cbdata.put("group_id", 0);
        }

        //endregion
        //region 20240702 查询用户组织信息
        AdminToOrganizeEntity adminToOrganizeEntity = AdminToOrganizeEntity.getInstance().where("admin_id", admin_id).findEntity();
        if (adminToOrganizeEntity != null) {
            adminCacheData.put("organize_code", adminToOrganizeEntity.organize_code);
            cbdata.put("organize_code", adminToOrganizeEntity.organize_code);
        } else {
            adminCacheData.put("organize_code", 0);
            cbdata.put("organize_code", 0);
        }
        //region 账号是否允许多端登录
        boolean loginUnique = SysGlobalConfigEntity.getBool("Admin:Login:Unique", false);
        if (!loginUnique && loginDevice != null && !loginDevice.isEmpty()) {
            //删除之前的登录信息
            for (JSONObject nd : loginDevice.values()) {
                String cache_token = nd.getString("Token");
                DataService.getMainCache().del("Admin:Token:" + cache_token);
            }
            DataService.getMainCache().del("Admin:Token:" + admin_id);
        }

        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("Token", token);
        deviceInfo.put("OperatingSystemName", OperatingSystemName);
        deviceInfo.put("DeviceType", DeviceType);
        deviceInfo.put("Manufacturer", Manufacturer);
        deviceInfo.put("Browser", userAgent.getBrowser());
        deviceInfo.put("IP", HttpRequestUtil.getIP());

        if (loginDevice == null) loginDevice = new LinkedHashMap<>();
        if (loginDevice.containsKey(Unique)) {
            JSONObject nd = loginDevice.get(Unique);
//            String cache_token = JsonUtil.getString(nd, "Token");
            String cache_token = nd.getString("Token");
            DataService.getMainCache().del("Admin:Token:" + cache_token);
        }
        loginDevice.put(Unique, deviceInfo);

        DataService.getMainCache().setMap("Admin:Token:" + token, adminCacheData, 86400000 * 7);
        DataService.getMainCache().setMap("Admin:Device:" + admin_id, loginDevice, 86400000 * 7);

        //endregion

        //endregion

        cbdata.put("id", data.get("id"));
        cbdata.put("token", token);
        cbdata.put("account", common.stringHide(MapUtil.getString(data, "account"), 3, 2, 4, "***"));
        cbdata.put("last_name", data.get("last_name"));
        cbdata.put("first_name", common.stringHide(MapUtil.getString(data, "first_name"), 0, 1, 1, "*"));
        cbdata.put("gender", data.get("gender"));
        cbdata.put("email", common.stringHideEmail(MapUtil.getString(data, "email")));
        cbdata.put("phone_num", common.stringHidePhoneNumCN(MapUtil.getString(data, "phone_num")));
        cbdata.put("avatar", data.get("avatar"));
        cbdata.put("remark", data.get("remark"));
        cbdata.put("sharecode", data.get("sharecode"));
        return new SyncResult(0, "", cbdata);
    }

    /**
     * 登出
     *
     * @param token
     * @return
     */
    public ISyncResult logout(String token) {
        long admin_id = getAdminIdWithToken(token);
        if (admin_id == 0) return new SyncResult(0, "");

        Map<String, JSONObject> loginDevice = DataService.getMainCache().getMap("Admin:Device:" + admin_id, new LinkedHashMap<>());
        for (String Unique : loginDevice.keySet()) {
            JSONObject device_info = MapUtil.getJSONObject(loginDevice, Unique);
            if (device_info == null) continue;

            String cache_token = JsonUtil.getString(device_info, "Token");
            if (!cache_token.equalsIgnoreCase(token)) continue;

            loginDevice.remove(Unique);
        }

        DataService.getMainCache().del("Admin:Token:" + token);
        DataService.getMainCache().setMap("Admin:Device:" + admin_id, loginDevice, 86400000 * 7);
        return new SyncResult(0, "");
    }

    /**
     * 读取指定管理员获得的角色ID列表
     *
     * @return
     */
    public List<Object> getRoleIdList(long admin_id) {
        Map<Long, Long> role_dic = new HashMap<>();
        List<Object> roleIdList = initCache().getList(String.format("Admin:%s:RoleList", admin_id));
        if (roleIdList != null && !roleIdList.isEmpty()) return roleIdList;
//        List<Long> roleIdList =new ArrayList<>();
        //region 读取组织绑定的角色
//        List<Map<String, Object>> a2o2r = AdminToOrganizeEntity.getInstance()
//                .field("b.role_id")
//                .alias("a")
//                .join(RBOrganizeToRoleEntity.getInstance().theTableName(), "b", "a.organize_id = b.organize_id")
//                .where("admin_id", admin_id)
//                .select();
//        if (a2o2r.size() > 0) {
//            Iterator it = a2o2r.iterator();
//            while (it.hasNext()) {
//                Map<String, Object> nd = (Map<String, Object>) it.next();
//                long role_id = MapUtil.getLong(nd, "role_id");
//                role_dic.put(role_id, role_id);
//            }
//        }
        //endregion

        //region 读取组织职位绑定的角色
//        List<Map<String, Object>> a2j2r = AdminToOrganizeJobEntity.getInstance()
//                .field("b.role_id")
//                .alias("a")
//                .join(RBJobToRoleEntity.getInstance().theTableName(), "b", "a.job_id = b.job_id")
//                .where("admin_id", admin_id)
//                .select();
//        if (a2j2r.size() > 0) {
//            Iterator it = a2j2r.iterator();
//            while (it.hasNext()) {
//                Map<String, Object> nd = (Map<String, Object>) it.next();
//                long role_id = MapUtil.getLong(nd, "role_id");
//                role_dic.put(role_id, role_id);
//            }
//        }
        //endregion

        //region 读取用户组绑定的角色
        List<Map<String, Object>> a2a2r = AdminToAUGroupEntity.getInstance()
                .field("b.role_id")
                .alias("a")
                .join(AUGroupToRoleEntity.getInstance().theTableName(), "b", "a.group_id = b.group_id")
                .where("admin_id", admin_id)
                .select();
        if (!a2a2r.isEmpty()) {
            for (Map<String, Object> nd : a2a2r) {
                long role_id = MapUtil.getLong(nd, "role_id");
                role_dic.put(role_id, role_id);
            }
        }
        //endregion

        roleIdList = new LinkedList<>(role_dic.keySet());
        if (!roleIdList.isEmpty()) initCache().setList(String.format("Admin:%s:RoleList", admin_id), roleIdList);
        return roleIdList;
    }

    /**
     * 读取指定管理员获得的角色信息列表
     *
     * @return
     */
    public List<RBRoleEntity> getRoleList(long admin_id) {
        List<Object> roleIdList = getRoleIdList(admin_id);
        if (roleIdList == null || roleIdList.isEmpty()) return new LinkedList<>();

        return RBRoleEntity.getInstance()
                .cache("Admin:Role:" + admin_id)
                .whereIn("id", roleIdList)
                .selectList();
    }

    /**
     * 查询所拥有的组织id
     *
     * @param adminId 管理员ID
     * @return
     */
    public String getOrganizeIdStringWithAdminId(long adminId) {
        List<Long> organizeList = getOrganizeIdListWithAdminId(adminId);
        StringBuilder idsStr = new StringBuilder();
        for (Long id : organizeList) idsStr.append(id);
        if (StringUtils.hasLength(idsStr.toString())) idsStr = new StringBuilder(idsStr.substring(1));
        return idsStr.toString();
    }

    /**
     * 查询所拥有的组织id
     *
     * @param adminId 管理员ID
     * @return
     */
    public List<Long> getOrganizeIdListWithAdminId(long adminId) {
        List<Long> organizeList = initCache().getList(String.format("Admin:Organize:%s:List", adminId));
        if (organizeList != null && !organizeList.isEmpty()) return organizeList;

        List<Map<String, Object>> list = AdminToOrganizeEntity.getInstance()
                .where("admin_id", adminId)
                .select();
        if (list.isEmpty()) return new LinkedList<>();
        if (organizeList == null) organizeList = new LinkedList<>();

        for (Map<String, Object> nd : list) {
            organizeList.add(MapUtil.getLong(nd, "organize_id"));
        }

        initCache().setList("Admin:Organize:" + adminId, organizeList);
        return organizeList;
    }

    /**
     * 读取管理员操作权限
     *
     * @param admin_id
     * @return
     */
    public List<String> getOperatePermission(long admin_id) {
        List<Object> roleIdList = getRoleIdList(admin_id);
        if (roleIdList == null || roleIdList.isEmpty()) return new LinkedList<>();

        List<String> data = initCache().getList("Admin:Permission:Operate:" + admin_id);
        if (data != null && !data.isEmpty()) return data;

        List<Map<String, Object>> list = RBRoleToPermissionEntity.getInstance()
                .field("p.name,p.path")
                .alias("r")
                .leftJoin(RBPermissionEntity.getInstance().theTableName(), "p", "r.permission_id = p.id")
                .where("type_code", "O")
                .where("parent_id", ">", 0)
                .whereIn("role_id", roleIdList)
                .select();

        data = new LinkedList<>();
        for (Map<String, Object> nd : list) {
            data.add(MapUtil.getString(nd, "path"));
//            data.put(MapUtil.getString(nd, "path"), MapUtil.getString(nd, "name"));
        }
        if (!data.isEmpty()) initCache().setList("Admin:Permission:Operate:" + admin_id, data);
        return data;
    }

    /**
     * 检查是否登录超时：通过当前Http请求
     *
     * @return
     */
    public boolean checkLoginTimeoutWithRequest() {
        String token = getTokenWithRequest();
        if (VerifyUtil.isEmpty(token)) return false;
//        if (DataService.getMainCache().exists("Admin:Token:" + token)) {
//            return getAdminIdWithToken(token) > 0;
//        }
        return getAdminIdWithToken(token) > 0;
    }

    /**
     * 读取token的值：通过当前Http请求
     *
     * @return
     */
    public String getTokenWithRequest() {
        HttpServletRequest request = HttpRequestUtil.getHttpServletRequest();
        String token = HttpRequestUtil.getHeader(request, "token");// request.getHeader("token");
        if (token == null) token = HttpRequestUtil.getString(request, "token");
        if ("".equals(token)) token = HttpRequestUtil.getString(request, "token");
        return token;
    }

    /**
     * 获取管理员ID：通过当前Http请求
     *
     * @return
     */
    public long getAdminIdWithRequest() {
        String token = getTokenWithRequest();
        return getAdminIdWithToken(token);
    }

    /**
     * 获取管理员ID：通过当前Http请求
     *
     * @return
     */
    public long getAdminIdWithToken(String token) {
        if (!StringUtils.hasLength(token)) return 0;
        Map<String, Object> data = DataService.getMainCache().getMap("Admin:Token:" + token);
        if (MapUtil.isEmpty(data)) return 0;
        return MapUtil.getLong(data, "id");
    }

    /**
     * 读取token的值：通过管理员id从缓存中读取
     *
     * @param admin_id
     * @return
     */
    public String getTokenWithId(long admin_id) {
        return DataService.getMainCache().getString(String.format("Admin:%s:Token", admin_id));
    }

    /**
     * 通过管理员Id读取管理员信息
     *
     * @param admin_id 管理员ID
     * @return
     */
    public AdminBaseEntity getWithId(long admin_id) {
        return getWithId(admin_id, true);
    }

    /**
     * 通过管理员Id读取管理员信息
     *
     * @param admin_id 管理员ID
     * @param inCache  是否从缓存中读取
     * @return
     */
    public AdminBaseEntity getWithId(long admin_id, boolean inCache) {
        if (inCache) {
            return this.cache(String.format("Admin:%s:Details", admin_id)).findEntity(admin_id);
        } else {
            return this.findEntity(admin_id);
        }
    }
}
