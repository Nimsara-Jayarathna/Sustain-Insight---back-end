# Email Migration Implementation Summary

## Goal

Replace the old Gmail-API-specific email implementation with the Brevo transactional-email approach used by SuperviseSuite while keeping Sustain Insight business code provider-independent and easy to replace later.

## Final layers

| Layer | File(s) | Responsibility |
|---|---|---|
| Business contract | `service/EmailService.java` | Stable methods used by auth/account/email-change flows. No provider types. |
| Use-case implementation | `email/service/EmailServiceImpl.java` | Chooses subject/template and creates provider-neutral messages. |
| Template rendering | `email/template/EmailTemplateService.java` | Adds brand/common variables and renders Thymeleaf HTML. |
| Message model | `email/model/EmailMessage.java` | Provider-neutral validated outbound email model. |
| Provider boundary | `email/provider/EmailProvider.java` | Tiny replaceable delivery interface. |
| Brevo adapter | `email/provider/BrevoEmailProvider.java` | Converts `EmailMessage` to Brevo SDK objects and sends it. |
| Provider config | `email/config/EmailConfig.java` | Creates the Brevo SDK client only when `EMAIL_PROVIDER=brevo`. |
| Email settings | `email/config/EmailProperties.java` | Binds generic sender/provider and Brevo settings. |
| Failure abstraction | `email/exception/EmailDeliveryException.java` | Prevents Brevo exceptions from leaking to business/controller code. |
| HTTP error handling | `controller/GlobalExceptionHandler.java` | Converts delivery failures to HTTP 503 / `EMAIL_SERVICE_UNAVAILABLE`. |
| HTML templates | `resources/templates/email/*.html` | One template per existing email use case. |

## Existing business calls preserved

The following public methods still exist with the same signatures:

```text
sendPasswordResetEmail(to, resetLink)
sendPasswordChangeNotification(to, firstName)
sendCurrentEmailVerificationOtp(to, otp)
sendNewEmailConfirmationOtp(to, otp)
sendEmailChangeSuccessNotification(to)
sendAccountVerificationEmail(to, firstName, verificationLink)
```

Therefore existing auth/account/email-change callers do not import Brevo and do not need provider-specific logic.

## Environment migration

Remove the old Gmail delivery variables from the deployed `APP_ENV_FILE` after this version is adopted:

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REFRESH_TOKEN
GOOGLE_SENDER_EMAIL
MAIL_FROM
```

Add:

```dotenv
EMAIL_PROVIDER=brevo
BREVO_API_KEY=<real Brevo transactional API key>
BREVO_SENDER_EMAIL=<verified Brevo sender email>
BREVO_SENDER_NAME=Sustain Insight
```

The complete staging contract is in `.env.staging.example`; the local contract is in `.env.example`.

## Brevo setup requirement

Before deployment, verify `BREVO_SENDER_EMAIL` in the Brevo account. The API key belongs only in GitHub `APP_ENV_FILE` / the server `.env`, never in source control.

## Provider replacement later

To migrate to SES, Postmark, Mailgun, or another provider:

1. Add a new `EmailProvider` implementation.
2. Add provider-specific configuration under `email/config`.
3. Guard the adapter with `@ConditionalOnProperty`.
4. Set `EMAIL_PROVIDER` to the new provider value.
5. Leave `EmailService`, `EmailServiceImpl`, templates, controllers, and auth/account services unchanged.

## CI/build

The previously incomplete Maven wrapper metadata was restored under `.mvn/wrapper/maven-wrapper.properties`. The staging CI validate step now runs:

```bash
./mvnw -B clean verify
```

The Docker build still packages with tests skipped because CI is responsible for the test gate before image creation.

## Existing deployment protection

The original Azure workflow was not edited. Its SHA-256 before and after this migration is:

```text
2ceab04858a4bc7145ceaf386051c853f9e9de9dc032d6e6e580c3cbf109aec5
```
