package me.coolclk.coolchat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 负责账号的控制。
 * @author CoolCLK
 */
public class AccountController {
    /**
     * 基础账号单位。
     * @author CoolCLK
     */
    static class Account {
        String account;
        String password;
        String nickname;
        List<Session> sessions = new ArrayList<>();
        long registerTime = -1;

        /**
         * 初始化一个账号实例。
         * @author CoolCLK
         */
        private Account(String account, String password) {
            super();

            this.account = account;
            this.password = password;
            this.nickname = account;
        }

        /**
         * 设定注册时间。
         * @author CoolCLK
         */
        public void setRegisterTime(long newRegisterTime) {
            this.registerTime = newRegisterTime;
            Application.logAsFullLog("[账号管理器] 设定账号 " + account + " 的注册时间为 " + newRegisterTime);
        }

        /**
         * 修改昵称。
         * @author CoolCLK
         */
        public void setNickname(String newNickname) {
            this.nickname = newNickname;
            Application.logAsFullLog("[账号管理器] 设定账号 " + account + " 的昵称为 " + newNickname);
        }

        /**
         * 修改密码。
         * @author CoolCLK
         */
        public void setPassword(String newPassword) {
            this.password = newPassword;
            Application.logAsFullLog("[账号管理器] 设定账号 " + account + " 的密码为 " + newPassword);
        }

        /**
         * 创建一个可用的session。
         * @return session
         * @author CoolCLK
         */
        public String createSession() {
            String sessionString;
            boolean reran = true;
            do {
                sessionString = RandomStringUtils.random(32, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
                String finalSession = sessionString;
                if (AccountController.accounts.stream().filter(a -> a.sessions.stream().filter(s -> Objects.equals(s.getSession(), finalSession)).findAny().orElse(null) != null).findAny().orElse(null) == null) {
                    reran = false;
                }
            }
            while (reran);
            Session session = new Session(sessionString);
            Application.logAsFullLog("[账号管理器] 添加账号 " + this.account + " 快捷登录密钥 " + sessionString + "。");
            session.updateAge();
            this.sessions.add(session);
            return sessionString;
        }

        /**
         * 检查session是否可用。
         * @author CoolCLK
         */
        public boolean checkSession(String session) {
            Session checkingSession = this.sessions.stream().filter(s -> Objects.equals(session, s.getSession())).findAny().orElse(null);
            if (checkingSession != null) {
                return !checkingSession.checkOutOfDate();
            }
            return false;
        }

        /**
         * 移除一个已创建了的session。
         * @author CoolCLK
         */
        public void removeSession(String session) {
            this.sessions.stream().filter(s -> Objects.equals(s.getSession(), session)).findAny().ifPresent(s -> this.sessions.remove(s));
        }

        public String toString() {
            Map<String, Object> request = new HashMap<>();
            request.put("account", this.account);
            request.put("password", this.password);
            request.put("nickname", this.nickname);
            request.put("sessions", this.sessions.toString());
            return request.toString();
        }

        /**
         * 将 Json 字符串转为账号实例。
         * @return 账号实例
         * @author CoolCLK
         */
        public static Account fromString(String accountJson) {
            return new Gson().fromJson(accountJson, Account.class);
        }

        /**
         * 基础账号密钥单位。
         * @author CoolCLK
         */
        class Session {
            String session;
            long age;

            /**
             * 新建一个密钥实例。
             * @author CoolCLK
             */
            public Session(String session) {
                super();
                this.session = session;
                this.age = System.currentTimeMillis();
            }

            /**
             * 检查密钥是否过期。
             * @return 可用性
             * @author CoolCLK
             */
            public boolean checkOutOfDate() {
                if (System.currentTimeMillis() > this.age * 1000) {
                    Account removingSessionAccount = AccountController.accounts.stream().filter(a -> a.sessions.stream().filter(s -> this.getSession() == s.getSession()).findAny().orElse(null) != null).findAny().orElse(null);
                    if (removingSessionAccount != null) {
                        removingSessionAccount.removeSession(this.session);
                        Application.logAsFullLog("[账号管理器] 删除账号 " + removingSessionAccount.account + " 快捷登录密钥 " + this.session + " (密钥过期 )");
                        return true;
                    }
                }
                return false;
            }

            /**
             * 获取密钥值。
             * @return 可用性
             * @author CoolCLK
             */
            public String getSession() {
                return this.session;
            }

            /**
             * 设定密钥过期时间。
             * @author CoolCLK
             */
            public void setAge(long newAge) {
                this.age = newAge;
                Application.logAsFullLog("[账号管理器] 设定快捷登录密钥 " + this.session + " 有效期至 " + new SimpleDateFormat("yyyy 年 MM月 dd日 HH时 mm分 ss秒").format(new Date(newAge)) + " 。");
            }

            /**
             * 更新密钥过期时间。
             * @author CoolCLK
             */
            public void updateAge() {
                long newAge = System.currentTimeMillis() + Long.parseLong(ConfigController.getValue("account.session.max-life").toString()) * 1000;
                Application.logAsFullLog("[账号管理器] 更新快捷登录密钥 " + this.session + "  (原本时间为 " + new SimpleDateFormat("yyyy 年 MM月 dd日 HH时 mm分 ss秒").format(new Date(this.age)) + "  )。");
                setAge(newAge);
            }

            /**
             * 获取密钥值。
             * @author CoolCLK
             */
            @Override
            public String toString() {
                return this.getSession();
            }
        }
    }

    public static List<Account> accounts;
    private static File accountFile;

    /**
     * 获取账号储存文件路径。
     * @return 路径
     * @author CoolCLK
     */
    public static String getAccountFilepath() {
        return System.getProperty("user.dir") + "/data/accounts.json";
    }

    /**
     * 检查账号储存文件可用性。
     * @return 储存文件可用性
     * @author CoolCLK
     */
    public static boolean checkAccountFile() {
        return accountFile != null && accountFile.exists();
    }

    /**
     * 获取账号储存文件。若不存在，则创建一个新的账号储存文件。
     * @return 账号储存文件
     * @author CoolCLK
     */
    public static File getAccountFile() {
        accountFile = new File(getAccountFilepath());
        if (!checkAccountFile()) {
            try {
                accountFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return accountFile;
    }

    /**
     * 获取账号数据组。
     * @return 所有账号数据列表
     * @author CoolCLK
     */
    public static List<Account> getAccountDatas() {
        try {
            List<Account> data = new Gson().fromJson(new FileReader(getAccountFile()), new TypeToken<List<Account>>(){}.getType());
            Application.logAsFullLog("[账号管理器] 从账号文件获取了账号数据: " + data);
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查账号数据组的可用性并强制更新数据。
     * @return 可用性
     * @author CoolCLK
     */
    public static boolean checkAccountDatas(boolean forceUpdate) {
        try {
            if (AccountController.accounts == null || forceUpdate) {
                AccountController.accounts = getAccountDatas();
                AccountController.accounts.forEach(a -> a.sessions.forEach(Account.Session::checkOutOfDate));
                Application.logAsFullLog("[账号管理器] 账号数据异常或强制更新了账号数据 " + AccountController.accounts.toString());
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 检查账号数据组的可用性。
     * @return 可用性
     * @author CoolCLK
     */
    public static boolean checkAccountDatas() {
        return checkAccountDatas(false);
    }

    /**
     * 将账号数据组写入账号储存文件。
     * @author CoolCLK
     */
    public static void saveAccountFile() {
        try {
            FileWriter fw = new FileWriter(getAccountFile());
            fw.write(new Gson().toJson(AccountController.accounts));
            fw.flush();
            fw.close();
            Application.logAsFullLog("[账号管理器] 账号数据已保存 " + AccountController.accounts.toString());
            checkAccountDatas(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查账号可用性及其密码正确性（password可以为null，但将不会返回状态码2）。
     * 状态码 2: 账号可用且密码正确。
     * 状态码 1: 账号存在且可用，但密码不正确。
     * 状态码 0: 账号不存在或不可用。
     * 状态码 -1: 出现异常。
     * @return 登录状态码
     * @author CoolCLK
     */
    public static int checkAccountAvailable(String account, String password) {
        if (checkAccountDatas()) {
            Account checkAccount = AccountController.accounts.stream().filter(a -> Objects.equals(account, a.account)).findAny().orElse(null);
            if (checkAccount != null) {
                if (password != null) {
                    if (Objects.equals(password, checkAccount.password)) {
                        return 2;
                    }
                }
                return 1;
            }
            return 0;
        }
        return -1;
    }

    /**
     * 注册一个账号并返回账号实例，已被注册或出现其它问题返回null。
     * @return 账号实例
     * @author CoolCLK
     */
    public static Account accountRegister(String account, String password) {
        if (checkAccountAvailable(account, password) == 0) {
            Account newAccount = new Account(account, password);
            newAccount.setRegisterTime(System.currentTimeMillis());
            AccountController.accounts.add(newAccount);
            Application.logAsFullLog("[账号管理器] 注册账号 " + account + " ");
            saveAccountFile();
            return newAccount;
        }
        return null;
    }

    /**
     * 登录一个账号并返回账号实例，出现其它问题返回null。
     * @return 账号实例
     * @author CoolCLK
     */
    public static Account accountLogin(String account, String password) {
        if (checkAccountAvailable(account, password) == 2) {
            Account loginAccount = AccountController.accounts.stream().filter(a -> Objects.equals(account, a.account)).findAny().orElse(null);
            if (loginAccount != null) {
                if (Objects.equals(password, loginAccount.password)) {
                    Application.logAsFullLog("[账号管理器] 检查或登录账号 " + account + " ");
                    return loginAccount;
                }
            }
        }
        return null;
    }

    /**
     * 获取一个账号并返回账号实例。
     * @return 账号实例
     * @author CoolCLK
     */
    public static Account getAccount(String account) {
        if (checkAccountAvailable(account, null) == 1) {
            return AccountController.accounts.stream().filter(a -> Objects.equals(account, a.account)).findAny().orElse(null);
        }
        return null;
    }

    /**
     * 获取一个账号的昵称。
     * @return 昵称
     * @author CoolCLK
     */
    public static String getAccountNickname(String account) {
        if (getAccount(account) != null) {
            return getAccount(account).nickname;
        }
        return null;
    }

    /**
     * 删除一个账号（警告：不可恢复）。
     * @author CoolCLK
     */
    public static void deleteAccount(String account) {
        if (checkAccountAvailable(account, null) == 1) {
            accounts.remove(getAccount(account));
            saveAccountFile();
        }
    }

    /**
     * 通过浏览器保存的session登录一个账号并返回账号实例，出现其它问题返回null。
     * @return 账号实例
     * @author CoolCLK
     */
    public static Account accountLogin(String session, Boolean onlyCheck) {
        if (checkAccountDatas()) {
            Account loginAccount = AccountController.accounts.stream().filter(a -> a.checkSession(session)).findAny().orElse(null);
            if (loginAccount != null) {
                if (!onlyCheck) {
                    Application.logAsFullLog("[账号管理器] 使用快捷登录密钥 " + session + " 登录账号 " + loginAccount.account + " ");
                    loginAccount.sessions.stream().filter(s -> Objects.equals(s.getSession(), session)).findAny().ifPresent(Account.Session::updateAge);
                    saveAccountFile();
                }
            }
            return loginAccount;
        }
        return null;
    }
}
