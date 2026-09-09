# Design decisions

## Protocol separation

SMTP, POP3, and IMAP have different state models, so each protocol has separate listener, session, and handler classes. Shared concerns such as storage and authentication remain centralized.

## Shared storage

POP3 and IMAP operate on the same logical mailboxes. `MailStorage` therefore acts as the common access point, with synchronized operations to maintain consistency under concurrent sessions.

## Bounded concurrency

Connections are submitted to a fixed thread pool rather than spawning an unlimited number of worker threads. This limits resource consumption and makes overload behavior more predictable.

## SMTP routing

When the recipient domain is not local, the SMTP handler asks `DNSResolver` to resolve an MX host and then its A record before forwarding the message to the remote SMTP server. The original course network used three simulated domains and fixed DNS server addresses.

## Why this is not production-ready

Real-world mail infrastructure requires TLS, robust authentication, persistent queues and storage, comprehensive RFC compatibility, anti-abuse controls, observability, retry/backoff policies, and substantially more validation. This implementation focuses on understanding protocol mechanics and concurrent server design.
