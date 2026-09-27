# Sustain Insight Email Service

## Purpose

Sustain Insight uses the same Brevo transactional-email provider family as SuperviseSuite, but the integration is isolated behind provider-neutral interfaces so the provider can be replaced without rewriting authentication, password reset, or email-change business logic.

## Dependency flow

```text
AuthService / AuthController / AccountController / EmailChangeService
                              |
                              v
                     EmailService interface
                              |
                              v
                     EmailServiceImpl
                       |             |
                       v             v
             EmailTemplateService  EmailProvider
                                       |
                                       v
                               BrevoEmailProvider
                                       |
                                       v
                                   Brevo API
```

## Responsibilities

- `service/EmailService.java` — stable business-facing contract. Existing callers depend only on this interface.
- `email/service/EmailServiceImpl.java` — chooses subjects and templates for Sustain Insight use cases.
- `email/template/EmailTemplateService.java` — renders Thymeleaf HTML and supplies common brand/frontend variables.
- `email/model/EmailMessage.java` — provider-neutral outbound message model.
- `email/provider/EmailProvider.java` — low-level delivery interface.
- `email/provider/BrevoEmailProvider.java` — the only class that maps Sustain Insight email messages to the Brevo SDK.
- `email/config/EmailProperties.java` — central email configuration contract.
- `email/config/EmailConfig.java` — constructs the Brevo SDK client.
- `email/exception/EmailDeliveryException.java` — provider-neutral delivery failure.
- `resources/templates/email/*.html` — separate templates for every current Sustain Insight email use case.

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `EMAIL_PROVIDER` | No | Provider selector. Defaults to `brevo`. |
| `BREVO_API_KEY` | Yes for delivery | Brevo transactional-email API key. |
| `BREVO_SENDER_EMAIL` | Yes for delivery | Verified Brevo sender address. |
| `BREVO_SENDER_NAME` | No | Sender display name. Defaults to `Sustain Insight`. |
| `BRAND_NAME` | No | Brand used in subjects/templates. |
| `FRONTEND_URL` | Yes for links | Base URL used in footer/security links. |

The previous Gmail variables are no longer used by the email service:

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REFRESH_TOKEN
GOOGLE_SENDER_EMAIL
MAIL_FROM
```

They can be removed from the deployment secret after this version is deployed.

## Current email use cases

1. Account verification after signup.
2. Password reset link.
3. Password-change security notification.
4. OTP to the current email before an email-address change.
5. OTP to the new email to confirm the new address.
6. Email-change success notification.

## Replacing Brevo later

Do not change `EmailServiceImpl` or any caller. Add another implementation of `EmailProvider`, make it conditional on `app.email.provider`, add its configuration, and set `EMAIL_PROVIDER` to the new provider. Provider SDK classes must remain inside the provider/config packages.

## Security rules

- Never log API keys, OTPs, password-reset tokens, verification tokens, or rendered message bodies.
- Keep `BREVO_API_KEY` only in the GitHub `APP_ENV_FILE` secret / server `.env`; never commit the real value.
- Use a verified Brevo sender address.
- Business services should never import Brevo SDK classes.
