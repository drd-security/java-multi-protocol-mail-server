# Java Multi-Protocol Mail Server

Concurrent Java mail server implementing core **SMTP, POP3, and IMAP** behavior over TCP sockets, with shared in-memory mailboxes, user authentication, DNS-based inter-domain routing, and a bounded worker pool.

## What it demonstrates

- Application-layer protocol implementation from raw socket streams.
- Three different protocol/session models in one server.
- Concurrent client handling with a fixed thread pool.
- Thread-safe shared mailbox/storage access.
- SMTP forwarding based on MX/A DNS resolution.
- Separation between listeners, sessions, protocol handlers, storage, authentication, and routing.

## Architecture

```text
                    +------------------+
                    |    MailServer    |
                    +---------+--------+
                              |
             +----------------+----------------+
             |                |                |
        SMTPServer        POP3Server       IMAPServer
             |                |                |
        SMTPSession       POP3Session      IMAPSession
             |                |                |
        SMTPHandler       POP3Handler      IMAPHandler
             |                |                |
             +--------+-------+-------+--------+
                      |               |
                 MailStorage     UserAuthenticator
                      |
                    Mailbox

SMTP forwarding additionally uses DNSResolver.
All listeners submit client work to ServerThreadPool.
```

## Main components

| Component | Responsibility |
|---|---|
| `MailServer` | Starts and coordinates SMTP, POP3, and IMAP listeners. |
| `SMTPServer` / `POP3Server` / `IMAPServer` | Accept protocol-specific TCP connections. |
| `*Session` classes | Maintain per-client protocol state. |
| `*Handler` classes | Parse and execute protocol commands. |
| `MailStorage` | Shared mailbox/subscription storage. |
| `Mailbox` | Thread-safe collection of email messages. |
| `Email` | Message representation with metadata, UID, and flags. |
| `UserAuthenticator` | Simple educational authentication model. |
| `DNSResolver` | MX/A lookup and routing support. |
| `ServerThreadPool` | Bounds concurrent client work. |

## Build

```bash
make
```

Equivalent command:

```bash
javac -d build src/*.java
```

## Run

The original laboratory environment used simulated domains and privileged protocol ports. A typical invocation is:

```bash
java -cp build MailServer <domain> <maxThreads>
```

For local experimentation, you may need to change SMTP/POP3/IMAP ports from `25/110/143` to unprivileged ports and adapt the educational DNS configuration.

## Concurrency design

- A fixed executor limits the number of concurrent client tasks.
- `MailStorage` synchronizes shared operations.
- `Mailbox` synchronizes mutation and retrieval operations.
- Each connection has an isolated session object while sharing the common storage layer.

## Important limitations

This is an educational implementation, not a production mail server:

- mailbox contents are memory-only;
- authentication is intentionally simplistic;
- transport is unencrypted;
- IMAP support is partial;
- DNS resolution depends on the system `dig` command and the original simulated-network addresses;
- no persistent retry queue is implemented for failed forwarding.

## Academic context and contribution

Team project with two students. I completed the majority of the implementation and integration work; my teammate also contributed meaningfully across the project. Source-level author attribution is preserved.

## Publication status

Prepared for portfolio use. The original report and assignment are not included because they contain academic/submission information. See [NOTICE.md](NOTICE.md).
