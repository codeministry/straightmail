package com.encircle360.oss.straightmail.tenant.mail;

import com.encircle360.oss.straightmail.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Package-private utility for building {@link JavaMailSender} instances from SMTP configuration.
 *
 * <p>Centralises the sender-construction logic shared by {@link ConfigBasedMailSenderFactory}
 * and {@link TenantMailSenderFactory}, which differ only in how they supply their per-tenant
 * SMTP settings.
 *
 * <p>Not a Spring bean; all methods are static.
 */
@Slf4j
class SmtpSenderBuilder {

    /**
     * Normalised SMTP configuration record used as input to the builder.
     *
     * <p>Tenant-specific fields may be {@code null} / default-{@code false} to signal that the
     * global fallback value should be used.
     *
     * @param host     SMTP host; {@code null} falls back to the global host
     * @param port     SMTP port; {@code null} falls back to the global port
     * @param user     SMTP username; blank falls back to the global user
     * @param password SMTP password (may be {@code ENC(…)}-wrapped); blank falls back to global
     * @param ssl      whether implicit SSL is requested for this config
     * @param tls      whether STARTTLS is requested for this config
     */
    record SmtpConfig(String host, Integer port, String user, String password, boolean ssl, boolean tls) {
    }

    private SmtpSenderBuilder() {
    }

    /**
     * Builds and returns a fully configured {@link JavaMailSender}.
     *
     * <p>Tenant-specific fields take precedence; any unset field falls back to the global
     * {@code SmtpConfig}. Port 465 auto-enables SSL regardless of explicit flags.
     *
     * @param tenant the per-tenant SMTP settings
     * @param global the global SMTP fallback settings
     * @param enc    encryption service used to decrypt stored passwords
     * @param label  descriptive label used in debug log messages
     * @return a configured {@link JavaMailSender}
     */
    static JavaMailSender build(SmtpConfig tenant, SmtpConfig global, EncryptionService enc, String label) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        applySmtpConfig(sender, tenant, global, enc);
        applyTlsConfig(sender, tenant, global);
        log.debug("Built SMTP sender for '{}': host={}, port={}", label, sender.getHost(), sender.getPort());
        return sender;
    }

    /**
     * Returns {@code a} if it is non-null and non-blank, otherwise {@code b}.
     */
    static String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private static void applySmtpConfig(JavaMailSenderImpl sender, SmtpConfig tenant, SmtpConfig global,
                                        EncryptionService enc) {
        sender.setHost(coalesce(tenant.host(), global.host()));
        sender.setPort(tenant.port() != null ? tenant.port() : global.port());
        sender.setUsername(coalesce(tenant.user(), global.user()));
        sender.setPassword(coalesce(enc.decrypt(tenant.password()), global.password()));
        sender.setDefaultEncoding("UTF-8");
        sender.setProtocol("smtp");
    }

    private static void applyTlsConfig(JavaMailSenderImpl sender, SmtpConfig tenant, SmtpConfig global) {
        boolean hasTenantSmtp = tenant.host() != null && !tenant.host().isBlank();
        boolean ssl = hasTenantSmtp ? tenant.ssl() : global.ssl();
        boolean tls = hasTenantSmtp ? tenant.tls() : global.tls();

        // Port 465 is reserved for SMTPS and always requires SSL from the start.
        // Auto-enable SSL regardless of the tenant flag to prevent [EOF] errors.
        if (sender.getPort() == 465) {
            ssl = true;
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        // STARTTLS and implicit SSL are mutually exclusive; SSL takes precedence.
        props.put("mail.smtp.starttls.enable", String.valueOf(!ssl && tls));
    }
}
