//package org.clever.core.email;
//
//import lombok.Getter;
//import lombok.Setter;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.mail.*;
//
//import java.util.Arrays;
//import java.util.Date;
//import java.util.List;
//
///**
// * 发送邮件工具类，使用Apache commons-email实现<br/>
// * <p/>
// * 作者：LiZW <br/>
// * 创建时间：2016-5-21 17:48 <br/>
// */
//@Slf4j
//public class ApacheSendMailUtils {
//    /**
//     * 邮件编码格式
//     */
//    private static final String defaultEncoding = "UTF-8";
//
//    /**
//     * 发送邮件默认的账户
//     */
//    @Getter
//    private final String account;
//
//    /**
//     * 发送邮件默认账户的密码
//     */
//    @Getter
//    private final String accountPassword;
//
//    /**
//     * 发送邮件默认账户的邮箱服务器地址
//     */
//    @Setter
//    @Getter
//    private String accountHost;
//
//    /**
//     * 发送人的名称
//     */
//    @Setter
//    @Getter
//    private String fromName;
//
//    /**
//     * 创建发送邮件工具(帐号需要开通smtp服务)
//     *
//     * @param account         邮箱帐号 - 需要开通smtp服务
//     * @param accountPassword 密码
//     * @param fromName        发送人的名称，可以为空
//     */
//    private ApacheSendMailUtils(String account, String accountPassword, String fromName) {
//        if (StringUtils.isBlank(account) || StringUtils.isBlank(accountPassword)) {
//            throw new IllegalArgumentException("邮箱帐号/密码参数不正确");
//        }
//        this.account = account;
//        this.accountPassword = accountPassword;
//        this.accountHost = EmailServerHostUtils.getEmailSmtpHost(account);
//        if (StringUtils.isBlank(fromName)) {
//            this.fromName = account;
//        } else {
//            this.fromName = fromName;
//        }
//    }
//
//    /**
//     * 创建发送邮件工具(帐号需要开通smtp服务)
//     *
//     * @param account         邮箱帐号 - 需要开通smtp服务
//     * @param accountPassword 密码
//     */
//    public ApacheSendMailUtils(String account, String accountPassword) {
//        this(account, accountPassword, null);
//    }
//
//    /**
//     * 为邮件信息设置基本的属性值<br/>
//     *
//     * @param email       邮件信息对象
//     * @param fromAccount 发送人的邮箱帐号，不能为空
//     * @param fromName    发送人的名称，可以为空
//     * @param password    发送人的邮箱密码，不能为空
//     * @param to          设置收件人，不能为空
//     * @param subject     设置邮件主题，不能为空
//     * @param text        设置邮件内容，可以为空
//     * @param cc          设置抄送人，可以为空
//     * @param bcc         设置密送人，可以为空
//     * @param replyTo     设置邮件回复人，可以为空
//     * @param sentDate    设置发送时间，可以为空
//     * @return 返回传入的邮件信息对象
//     */
//    @SneakyThrows
//    private Email emailValueBind(Email email,
//                                 String fromAccount,
//                                 String fromName,
//                                 String password,
//                                 String[] to,
//                                 String subject,
//                                 String text,
//                                 String[] cc,
//                                 String[] bcc,
//                                 String replyTo,
//                                 Date sentDate) {
//        // 设置邮件编码
//        email.setCharset(defaultEncoding);
//        // 设置发件人邮箱密码
//        email.setAuthentication(fromAccount, password);
//        // 设置发件人邮箱服务器地址
//        email.setHostName(accountHost);
//        // 设置发件人
//        if (StringUtils.isBlank(fromName)) {
//            fromName = fromAccount;
//        }
//        email.setFrom(fromAccount, fromName, defaultEncoding);
//        // 设置收件人
//        email.addTo(to);
//        // 设置邮件主题
//        email.setSubject(subject);
//        // 设置邮件内容
//        if (null != text) {
//            email.setMsg(text);
//        }
//        // 设置抄送人
//        if (ArrayUtils.isNotEmpty(cc)) {
//            email.addCc(cc);
//        }
//        // 设置密送人
//        if (ArrayUtils.isNotEmpty(bcc)) {
//            email.addBcc(bcc);
//        }
//        // 设置邮件回复人
//        if (StringUtils.isNotBlank(replyTo)) {
//            email.addReplyTo(replyTo);
//        }
//        // 设置发送时间
//        if (null != sentDate) {
//            email.setSentDate(sentDate);
//        }
//        return email;
//    }
//
//    /**
//     * 发送简单类型的邮件，基本文本格式邮件<br/>
//     *
//     * @param to       设置收件人，不能为空
//     * @param subject  设置邮件主题，不能为空
//     * @param text     设置邮件内容，不能为空
//     * @param cc       设置抄送人，可以为空
//     * @param bcc      设置密送人，可以为空
//     * @param replyTo  设置邮件回复人，可以为空
//     * @param sentDate 设置发送时间，可以为空
//     */
//    @SneakyThrows
//    public void sendSimpleEmail(String[] to, String subject, String text, String[] cc, String[] bcc, String replyTo, Date sentDate) {
//        SimpleEmail simpleEmail = new SimpleEmail();
//        emailValueBind(simpleEmail, fromName, account, accountPassword, to, subject, text, cc, bcc, replyTo, sentDate);
//        String result = simpleEmail.send();
//        log.debug("sendSimpleEmail-邮件发送成功，返回值：[{}]", result);
//    }
//
//    /**
//     * 发送简单类型的邮件，基本文本格式邮件<br/>
//     *
//     * @param to      设置收件人，不能为空
//     * @param subject 设置邮件主题，不能为空
//     * @param text    设置邮件内容，不能为空
//     */
//    public void sendSimpleEmail(String[] to, String subject, String text) {
//        sendSimpleEmail(to, subject, text, null, null, null, null);
//    }
//
//    /**
//     * 发送简单类型的邮件，基本文本格式邮件<br/>
//     *
//     * @param to      设置收件人，不能为空
//     * @param subject 设置邮件主题，不能为空
//     * @param text    设置邮件内容，不能为空
//     */
//    @SneakyThrows
//    public void sendSimpleEmail(String to, String subject, String text) {
//        SimpleEmail simpleEmail = new SimpleEmail();
//        emailValueBind(simpleEmail, fromName, account, accountPassword, null, subject, text, null, null, null, null);
//        String result;
//        // 单独设置收件人
//        simpleEmail.addTo(to);
//        result = simpleEmail.send();
//        log.debug("sendSimpleEmail-邮件发送成功，返回值：[{}]", result);
//    }
//
//    /**
//     * 发送文本格式，带附件邮件<br/>
//     *
//     * @param to             设置收件人，不能为空
//     * @param subject        设置邮件主题，不能为空
//     * @param text           设置邮件内容，不能为空
//     * @param attachmentList 附件集合，可以为空
//     * @param cc             设置抄送人，可以为空
//     * @param bcc            设置密送人，可以为空
//     * @param replyTo        设置邮件回复人，可以为空
//     * @param sentDate       设置发送时间，可以为空
//     */
//    @SneakyThrows
//    public void sendMultiPartEmail(String[] to, String subject, String text, List<EmailAttachment> attachmentList, String[] cc, String[] bcc, String replyTo, Date sentDate) {
//        MultiPartEmail multiPartEmail = new MultiPartEmail();
//        emailValueBind(multiPartEmail, fromName, account, accountPassword, to, subject, text, cc, bcc, replyTo, sentDate);
//        String result;
//        // 增加附件
//        if (attachmentList != null && attachmentList.size() > 0) {
//            for (EmailAttachment emailAttachment : attachmentList) {
//                multiPartEmail.attach(emailAttachment);
//            }
//        }
//        result = multiPartEmail.send();
//        log.debug("sendMultiPartEmail-邮件发送成功，返回值：[{}]", result);
//    }
//
//    /**
//     * 发送文本格式，带附件邮件<br/>
//     *
//     * @param to             设置收件人，不能为空
//     * @param subject        设置邮件主题，不能为空
//     * @param text           设置邮件内容，不能为空
//     * @param attachmentList 附件集合，可以为空
//     */
//    public void sendMultiPartEmail(String[] to, String subject, String text, List<EmailAttachment> attachmentList) {
//        sendMultiPartEmail(to, subject, text, attachmentList, null, null, null, null);
//    }
//
//    /**
//     * 发送文本格式，带附件邮件<br/>
//     *
//     * @param to          设置收件人，不能为空
//     * @param subject     设置邮件主题，不能为空
//     * @param text        设置邮件内容，不能为空
//     * @param attachments 附件集合，可以为空
//     */
//    public void sendMultiPartEmail(String[] to, String subject, String text, EmailAttachment... attachments) {
//        sendMultiPartEmail(to, subject, text, Arrays.asList(attachments), null, null, null, null);
//    }
//
//    /**
//     * 发送HTML格式带附件邮件<br/>
//     *
//     * @param to             设置收件人，不能为空
//     * @param subject        设置邮件主题，不能为空
//     * @param htmlText       设置邮件内容，不能为空
//     * @param attachmentList 附件集合，可以为空
//     * @param cc             设置抄送人，可以为空
//     * @param bcc            设置密送人，可以为空
//     * @param replyTo        设置邮件回复人，可以为空
//     * @param sentDate       设置发送时间，可以为空
//     */
//    @SneakyThrows
//    public void sendHtmlEmail(String[] to, String subject, String htmlText, List<EmailAttachment> attachmentList, String[] cc, String[] bcc, String replyTo, Date sentDate) {
//        HtmlEmail htmlEmail = new HtmlEmail();
//        emailValueBind(htmlEmail, fromName, account, accountPassword, to, subject, null, cc, bcc, replyTo, sentDate);
//        String result;
//        // 设置html内容
//        htmlEmail.setHtmlMsg(htmlText);
//        // 增加附件
//        if (attachmentList != null && attachmentList.size() > 0) {
//            for (EmailAttachment emailAttachment : attachmentList) {
//                htmlEmail.attach(emailAttachment);
//            }
//        }
//        result = htmlEmail.send();
//        log.debug("sendHtmlEmail-邮件发送成功，返回值：[{}]", result);
//    }
//
//    /**
//     * 发送HTML格式带附件邮件<br/>
//     *
//     * @param to             设置收件人，不能为空
//     * @param subject        设置邮件主题，不能为空
//     * @param htmlText       设置邮件内容，不能为空
//     * @param attachmentList 附件集合，可以为空
//     */
//    public void sendHtmlEmail(String[] to, String subject, String htmlText, List<EmailAttachment> attachmentList) {
//        sendHtmlEmail(to, subject, htmlText, attachmentList, null, null, null, null);
//    }
//
//
//    /**
//     * 发送HTML格式带附件邮件<br/>
//     *
//     * @param to          设置收件人，不能为空
//     * @param subject     设置邮件主题，不能为空
//     * @param htmlText    设置邮件内容，不能为空
//     * @param attachments 附件集合，可以为空
//     */
//    public void sendHtmlEmail(String[] to, String subject, String htmlText, EmailAttachment... attachments) {
//        sendHtmlEmail(to, subject, htmlText, Arrays.asList(attachments), null, null, null, null);
//    }
//
//    /**
//     * 发送HTML格式带附件邮件<br/>
//     *
//     * @param to       设置收件人，不能为空
//     * @param subject  设置邮件主题，不能为空
//     * @param htmlText 设置邮件内容，不能为空
//     */
//    public void sendHtmlEmail(String[] to, String subject, String htmlText) {
//        sendHtmlEmail(to, subject, htmlText, null, null, null, null, null);
//    }
//
//    /**
//     * 发送HTML格式带附件邮件<br/>
//     *
//     * @param to       设置收件人，不能为空
//     * @param subject  设置邮件主题，不能为空
//     * @param htmlText 设置邮件内容，不能为空
//     */
//    public void sendHtmlEmail(String to, String subject, String htmlText) {
//        sendHtmlEmail(new String[]{to}, subject, htmlText, null, null, null, null, null);
//    }
//}
