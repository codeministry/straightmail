# straightmail

[![CI](https://github.com/encircle360-oss/straightmail/actions/workflows/ci.yml/badge.svg)](https://github.com/encircle360-oss/straightmail/actions/workflows/ci.yml)
[![Release](https://github.com/encircle360-oss/straightmail/actions/workflows/release.yml/badge.svg)](https://github.com/encircle360-oss/straightmail/actions/workflows/release.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Container](https://img.shields.io/badge/ghcr.io-encircle360--oss%2Fstraightmail-blue?logo=docker)](https://github.com/encircle360-oss/straightmail/pkgs/container/straightmail)
[![Matrix](https://img.shields.io/badge/Matrix-Join%20Chat-0dbd8b?logo=matrix&logoColor=white)](https://matrix.to/#/#oss:encircle360.com)

A small Spring Boot service that exposes a REST API for sending emails. Templates are rendered with [Freemarker](https://freemarker.apache.org/) and translated with standard `messages.properties` bundles, so subjects, HTML bodies and plain-text fallbacks all flow through the same locale-aware pipeline.

Maintained and sponsored by [encircle360 GmbH](https://encircle360.com) together with the open source community, partners and friends.

> **Project moved to GitHub.** The legacy GitLab repository at `gitlab.com/encircle360-oss/straightmail/straightmail` is archived and no longer receives updates. Container images are now published to GitHub Container Registry at `ghcr.io/encircle360-oss/straightmail`.

## Getting started

Pull and run the prebuilt image:

```bash
docker run -p 50003:50003 -p 50004:50004 \
    --env SMTP_HOST=host.docker.internal \
    --env SMTP_USER=foo \
    --env SMTP_PASSWORD=bar \
    --env SMTP_PORT=1025 \
    --env DEFAULT_SENDER=noreply@example.com \
    --env SMTP_ENABLE_TLS=true \
    --env SMTP_ENABLE_SSL=false \
    --env SPRING_PROFILES_ACTIVE=development \
    ghcr.io/encircle360-oss/straightmail:latest
```

`DEFAULT_SENDER` is used when a request does not specify a `sender`. For SSL use `SMTP_ENABLE_SSL=true` and `SMTP_ENABLE_TLS=false`; for STARTTLS keep the inverse.

Open `http://localhost:50003/swagger-ui/index.html` to explore the REST API. Switch the active profile to `production` for production deployments — Swagger UI is otherwise reachable.

This service is intended to run inside an internal network and should not be exposed to the public internet. There is no built-in authentication on its API.

## Sending an email with a file-based template

Templates live in `/resources/templates/` and consist of two or three files per template id:

- `<templateId>_subject.ftl` — the subject line (HTML is stripped)
- `<templateId>.ftl` — the HTML body
- `<templateId>_plain.ftl` — optional plain-text body

```bash
curl -X POST http://localhost:50003/ \
  -H "Content-Type: application/json" \
  -d '{
        "recipients": ["user@example.com"],
        "sender": "noreply@example.com",
        "senderName": "Straightmail",
        "model": { "name": "World" },
        "locale": "de",
        "emailTemplateId": "default"
      }'
```

## Sending an email with an inline template

```bash
curl -X POST http://localhost:50003/inline \
  -H "Content-Type: application/json" \
  -d '{
        "recipients": ["user@example.com"],
        "sender": "noreply@example.com",
        "subject": "Hello ${name}",
        "emailTemplate": "<p>Hello <b>${name}</b></p>",
        "model": { "name": "World" },
        "locale": "de"
      }'
```

## Sender display name

The optional `senderName` field controls the `From` header. When set, the header renders as `Display Name <noreply@example.com>` instead of the bare address.

## Attachments

Pass attachments as an array of objects. The `content` field is a base64-encoded byte string.

```json
{
  "attachments": [
    { "filename": "picture.jpg", "mimeType": "image/jpeg", "content": "IG51bGw=" }
  ]
}
```

## Customising templates and translations

Build a thin image on top of the upstream one:

```Dockerfile
FROM ghcr.io/encircle360-oss/straightmail:latest
ADD templates /resources/templates
ADD i18n /resources/i18n
```

See [src/main/resources/templates](src/main/resources/templates) and [src/main/resources/i18n](src/main/resources/i18n) for the expected file layout. The `emailTemplateId` in API requests maps to the template filename without extension (e.g. `emailConfirmation.ftl` → `"emailConfirmation"`).

## Service health

If the management port is mapped to your host, `http://localhost:50004/actuator/health` returns the liveness/readiness state.

## Building from source

```bash
./gradlew bootJar
```

Requires JDK 21 or newer. Skip tests with `-x test`.

## Contributing & community

We welcome contributors! Whether you want to:

- **Submit pull requests** for bug fixes, features or documentation improvements
- **Help with testing** and quality assurance
- **Improve documentation** and examples
- **Report bugs** or suggest new features
- **Become a maintainer** for the project

Every contribution is valuable. You don't need to be an expert — we're happy to help you get started.

### How to contribute

1. **Fork the repository** and create a feature branch
2. **Make your changes** (code, docs, tests)
3. **Test your changes** locally with `./gradlew build`
4. **Submit a Pull Request** with a clear description
5. **Engage in the review** — we'll work with you to get the change merged

### Becoming a maintainer

Interested in co-maintaining this project? Show your interest by contributing pull requests and helping in issues, then start a discussion in [GitHub Discussions](https://github.com/encircle360-oss/straightmail/discussions) so we can talk about it.

## Support & community

- **Matrix chat**: join [#oss:encircle360.com](https://matrix.to/#/#oss:encircle360.com) to talk to maintainers and other users
- **Bug reports & feature requests**: open a [GitHub Issue](https://github.com/encircle360-oss/straightmail/issues)
- **General questions and ideas**: start a [GitHub Discussion](https://github.com/encircle360-oss/straightmail/discussions)

For professional support, consulting or custom development, reach out via our website at [encircle360.com](https://encircle360.com).

## Disclaimer

This software is provided "AS IS" without warranty of any kind, either express or implied, including but not limited to the implied warranties of merchantability, fitness for a particular purpose, or non-infringement.

While we aim to keep the project healthy and well-tested, you acknowledge that:

- You use straightmail at your own risk.
- We recommend thorough testing in non-production environments before relying on it in production.
- The project may contain bugs or security issues. There is no built-in API authentication; do not expose it to the public internet.
- We are not liable for damages or losses resulting from its use.

For deployments that require guaranteed support or SLAs, please contact us via [encircle360.com](https://encircle360.com).

## License

Apache License 2.0 — see [LICENSE](LICENSE).

## Maintainers

This project is maintained and sponsored by **[encircle360 GmbH](https://encircle360.com)**, providing enterprise-grade Kubernetes and cloud-native solutions.

## Credits

Thanks to all contributors, partners and the wider open source community for making this project possible.
