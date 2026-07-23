# Security Policy

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Contact the repository owner privately through the contact method listed on their GitHub profile and include:

- the affected endpoint or component
- reproducible steps
- expected and observed impact
- a suggested mitigation, if available

Do not include production credentials, personal financial data, or access tokens in the report.

## Supported version

Security fixes are applied to the latest release on the `main` branch.

## Operational requirements

- Replace every example secret before deployment.
- Keep the database on a private network.
- Serve the application only over HTTPS.
- Rotate `JWT_SECRET` after suspected exposure; existing sessions will be invalidated.
- Remove bootstrap administrator credentials after initial provisioning.
