//package org.clever.core.email;
//
//import lombok.Getter;
//import lombok.Setter;
//import lombok.SneakyThrows;
//import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
//import org.springframework.mail.javamail.MimeMessageHelper;
//
//import javax.activation.DataSource;
//import javax.mail.internet.MimeMessage;
//import java.util.Date;
//import java.util.Map;
//import java.util.Set;
//
///**
// * 发送邮件工具类，使用Spring实现<br/>
// * 根据调用频率重载sendSimpleEmail和sendMimeMessage方法<br/>
// * <p/>
// * 作者：LiZW <br/>
// * 创建时间：2016-5-21 17:34 <br/>
// */
//public class SpringSendMailUtils {
//    /**
//     * Spring 发送邮件类
//     */
//    @Getter
//    private final JavaMailSender javaMailSender;
//
//    /**
//     * Spring配置的邮件发送帐号
//     */
//    @Setter
//    @Getter
//    private String fromEmailAccount;
//
//    /**
//     * 创建发送邮件工具(帐号需要开通smtp服务)
//     *
//     * @param javaMailSender   JavaMailSender对象
//     * @param fromEmailAccount Spring配置的邮件发送帐号
//     */
//    private SpringSendMailUtils(JavaMailSender javaMailSender, String fromEmailAccount) {
//        if (javaMailSender == null) {
//            throw new IllegalArgumentException("JavaMailSender参数不能为空");
//        }
//        this.javaMailSender = javaMailSender;
//        if (StringUtils.isBlank(fromEmailAccount)) {
//            if (javaMailSender instanceof JavaMailSenderImpl) {
//                this.fromEmailAccount = ((JavaMailSenderImpl) javaMailSender).getUsername();
//            }
//        } else {
//            this.fromEmailAccount = fromEmailAccount;
//        }
//        if (StringUtils.isBlank(this.fromEmailAccount)) {
//            throw new IllegalArgumentException("fromEmailAccount参数不能为空");
//        }
//    }
//
//    /**
//     * 创建发送邮件工具(帐号需要开通smtp服务)
//     *
//     * @param javaMailSender JavaMailSender对象
//     */
//    public SpringSendMailUtils(JavaMailSender javaMailSender) {
//        this(javaMailSender, null);
//    }
//
//    /**
//     * 创建发送邮件工具(帐号需要开通smtp服务)
//     *
//     * @param account         邮箱帐号 - 需要开通smtp服务
//     * @param accountPassword 密码
//     */
//    public SpringSendMailUtils(String account, String accountPassword) {
//        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
//        javaMailSender.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
//        javaMailSender.getJavaMailProperties().setProperty("mail.smtp.timeout", "20000");
//        javaMailSender.setDefaultEncoding("UtF-8");
//        javaMailSender.setHost(EmailServerHostUtils.getEmailSmtpHost(account));
//        // javaMailSender.setPort(3306);
//        javaMailSender.setUsername(account);
//        javaMailSender.setPassword(accountPassword);
//        this.javaMailSender = javaMailSender;
//        this.fromEmailAccount = javaMailSender.getUsername();
//    }
//
//    /**
//     * new一个简单的邮件信息，不支持html，不支持附件和图片<br/>
//     *
//     * @param to       设置收件人，不能为空
//     * @param subject  设置邮件主题，不能为空
//     * @param text     设置邮件内容，不能为空
//     * @param cc       设置抄送人，可以为空
//     * @param bcc      设置密送人，可以为空
//     * @param replyTo  设置邮件回复人，可以为空
//     * @param sentDate 设置发送时间，可以为空
//     * @return 返回一个新的 SimpleMailMessage 对象
//     */
//    @SuppressWarnings({"ConstantConditions", "DuplicatedCode"})
//    private SimpleMailMessage newSimpleMailMessage(String[] to, String subject, String text, String[] cc, String[] bcc, String replyTo, Date sentDate) {
//        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
//        // 设置发件人
//        simpleMailMessage.setFrom(fromEmailAccount);
//        // 设置收件人
//        simpleMailMessage.setTo(to);
//        // 设置邮件主题
//        simpleMailMessage.setSubject(subject);
//        // 设置邮件内容
//        simpleMailMessage.setText(text);
//
//        // 设置抄送人
//        if (ArrayUtils.isNotEmpty(cc)) {
//            simpleMailMessage.setCc(cc);
//        }
//        // 设置密送人
//        if (ArrayUtils.isNotEmpty(bcc)) {
//            simpleMailMessage.setBcc(bcc);
//        }
//        // 设置邮件回复人
//        if (StringUtils.isNotBlank(replyTo)) {
//            simpleMailMessage.setReplyTo(replyTo);
//        }
//        // 设置发送时间
//        if (null != sentDate) {
//            simpleMailMessage.setSentDate(sentDate);
//        }
//        return simpleMailMessage;
//    }
//
//    /**
//     * 发送一个简单的邮件，不支持html，不支持附件和图片<br/>
//     *
//     * @param to       设置收件人，不能为空
//     * @param subject  设置邮件主题，不能为空
//     * @param text     设置邮件内容，不能为空
//     * @param cc       设置抄送人，可以为空
//     * @param bcc      设置密送人，可以为空
//     * @param replyTo  设置邮件回复人，可以为空
//     * @param sentDate 设置发送时间，可以为空
//     */
//    public void sendSimpleEmail(String[] to, String subject, String text, String[] cc, String[] bcc, String replyTo, Date sentDate) {
//        SimpleMailMessage simpleMailMessage = newSimpleMailMessage(to, subject, text, cc, bcc, replyTo, sentDate);
//        javaMailSender.send(simpleMailMessage);
//    }
//
//    /**
//     * 发送一个简单的邮件，不支持html，不支持附件和图片<br/>
//     *
//     * @param to      设置收件人，不能为空
//     * @param subject 设置邮件主题，不能为空
//     * @param text    设置邮件内容，不能为空
//     */
//    public void sendSimpleEmail(String[] to, String subject, String text) {
//        SimpleMailMessage simpleMailMessage = newSimpleMailMessage(to, subject, text, null, null, null, null);
//        javaMailSender.send(simpleMailMessage);
//    }
//
//    /**
//     * 发送一个简单的邮件，不支持html，不支持附件和图片<br/>
//     *
//     * @param to      设置收件人，不能为空
//     * @param subject 设置邮件主题，不能为空
//     * @param text    设置邮件内容，不能为空
//     */
//    public void sendSimpleEmail(String to, String subject, String text) {
//        SimpleMailMessage simpleMailMessage = newSimpleMailMessage(null, subject, text, null, null, null, null);
//        simpleMailMessage.setTo(to);
//        javaMailSender.send(simpleMailMessage);
//    }
//
//    /**
//     * new一个复杂的邮件信息，支持html，支持附件和图片<br/>
//     * 如果邮件中需要显示内联的图片，html格式：&lt;img src="cid:inlineImgKey" width="214" height="46" &frasl;&gt;<br/>
//     * 再设置inlineMap参数，inlineMap.put("inlineImgKey", dataSource)<br/>
//     *
//     * @param to            设置收件人，不能为空
//     * @param subject       设置邮件主题，不能为空
//     * @param text          设置邮件内容(支持html)，不能为空
//     * @param inlineMap     设置内联资源(邮件html中的图片)，格式，，可以为空
//     * @param attachmentMap 设置附件，可以为空
//     * @param cc            设置抄送人，可以为空
//     * @param bcc           设置密送人，可以为空
//     * @param replyTo       设置邮件回复人，可以为空
//     * @param sentDate      设置发送时间，可以为空
//     * @return 返回一个新的 MimeMessage 对象
//     */
//    @SuppressWarnings("DuplicatedCode")
//    @SneakyThrows
//    private MimeMessage newMimeMessage(String[] to,
//                                       String subject,
//                                       String text,
//                                       Map<String, DataSource> inlineMap,
//                                       Map<String, DataSource> attachmentMap,
//                                       String[] cc,
//                                       String[] bcc,
//                                       String replyTo,
//                                       Date sentDate) {
//        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//        //创建MimeMessageHelper对象，处理MimeMessage的辅助类
//        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
//        // 设置发件人
//        mimeMessageHelper.setFrom(fromEmailAccount);
//        // 设置收件人
//        mimeMessageHelper.setTo(to);
//        // 设置邮件主题
//        mimeMessageHelper.setSubject(subject);
//        // 设置邮件内容，支持html
//        mimeMessageHelper.setText(text, true);
//
//        // 设置抄送人
//        if (ArrayUtils.isNotEmpty(cc)) {
//            mimeMessageHelper.setCc(cc);
//        }
//        // 设置密送人
//        if (ArrayUtils.isNotEmpty(bcc)) {
//            mimeMessageHelper.setBcc(bcc);
//        }
//        // 设置邮件回复人
//        if (StringUtils.isNotBlank(replyTo)) {
//            mimeMessageHelper.setReplyTo(replyTo);
//        }
//        // 设置发送时间
//        if (null != sentDate) {
//            mimeMessageHelper.setSentDate(sentDate);
//        }
//
//        // 增加内联资源，如：邮件html中的图片
//        if (inlineMap != null) {
//            Set<String> inlineSetKey = inlineMap.keySet();
//            for (String key : inlineSetKey) {
//                mimeMessageHelper.addInline(key, inlineMap.get(key));
//            }
//        }
//        // 增加附件
//        if (attachmentMap != null) {
//            Set<String> attachmentSetKey = attachmentMap.keySet();
//            for (String key : attachmentSetKey) {
//                mimeMessageHelper.addAttachment(key, attachmentMap.get(key));
//            }
//        }
//        return mimeMessage;
//    }
//
//    /**
//     * 发送一个复杂的邮件，支持html，支持附件和图片<br/>
//     * 如果邮件中需要显示内联的图片，html格式：&lt;img src="cid:inlineImgKey" width="214" height="46" &frasl;&gt;<br/>
//     * 再设置inlineMap参数，inlineMap.put("inlineImgKey", dataSource)<br/>
//     *
//     * @param to            设置收件人，不能为空
//     * @param subject       设置邮件主题，不能为空
//     * @param text          设置邮件内容(支持html)，不能为空
//     * @param inlineMap     设置内联资源(邮件html中的图片)，格式，，可以为空
//     * @param attachmentMap 设置附件，可以为空
//     * @param cc            设置抄送人，可以为空
//     * @param bcc           设置密送人，可以为空
//     * @param replyTo       设置邮件回复人，可以为空
//     * @param sentDate      设置发送时间，可以为空
//     */
//    public void sendMimeMessage(String[] to,
//                                String subject,
//                                String text,
//                                Map<String, DataSource> inlineMap,
//                                Map<String, DataSource> attachmentMap,
//                                String[] cc,
//                                String[] bcc,
//                                String replyTo,
//                                Date sentDate) {
//        MimeMessage mimeMessage = newMimeMessage(to, subject, text, inlineMap, attachmentMap, cc, bcc, replyTo, sentDate);
//        javaMailSender.send(mimeMessage);
//    }
//
//    /**
//     * 发送一个复杂的邮件，支持html，支持附件和图片<br/>
//     * 如果邮件中需要显示内联的图片，html格式：&lt;img src="cid:inlineImgKey" width="214" height="46" &frasl;&gt;<br/>
//     * 再设置inlineMap参数，inlineMap.put("inlineImgKey", dataSource)<br/>
//     *
//     * @param to            设置收件人，不能为空
//     * @param subject       设置邮件主题，不能为空
//     * @param text          设置邮件内容(支持html)，不能为空
//     * @param inlineMap     设置内联资源(邮件html中的图片)，格式，，可以为空
//     * @param attachmentMap 设置附件，可以为空
//     */
//    public void sendMimeMessage(String[] to, String subject, String text, Map<String, DataSource> inlineMap, Map<String, DataSource> attachmentMap) {
//        MimeMessage mimeMessage = newMimeMessage(to, subject, text, inlineMap, attachmentMap, null, null, null, null);
//        javaMailSender.send(mimeMessage);
//    }
//
//    /**
//     * 发送一个复杂的邮件，支持html，支持附件和图片<br/>
//     * 如果邮件中需要显示内联的图片，html格式：&lt;img src="cid:inlineImgKey" width="214" height="46" &frasl;&gt;<br/>
//     * 再设置inlineMap参数，inlineMap.put("inlineImgKey", dataSource)<br/>
//     *
//     * @param to            设置收件人，不能为空
//     * @param subject       设置邮件主题，不能为空
//     * @param text          设置邮件内容(支持html)，不能为空
//     * @param attachmentMap 设置附件，可以为空
//     */
//    public void sendMimeMessage(String[] to, String subject, String text, Map<String, DataSource> attachmentMap) {
//        MimeMessage mimeMessage = newMimeMessage(to, subject, text, null, attachmentMap, null, null, null, null);
//        javaMailSender.send(mimeMessage);
//    }
//
//    /**
//     * 发送一个复杂的邮件，支持html，支持附件和图片<br/>
//     * 如果邮件中需要显示内联的图片，html格式：&lt;img src="cid:inlineImgKey" width="214" height="46" &frasl;&gt;<br/>
//     * 再设置inlineMap参数，inlineMap.put("inlineImgKey", dataSource)<br/>
//     *
//     * @param to      设置收件人，不能为空
//     * @param subject 设置邮件主题，不能为空
//     * @param text    设置邮件内容(支持html)，不能为空
//     */
//    public void sendMimeMessage(String to, String subject, String text) {
//        MimeMessage mimeMessage = newMimeMessage(new String[]{to}, subject, text, null, null, null, null, null, null);
//        javaMailSender.send(mimeMessage);
//    }
//}
