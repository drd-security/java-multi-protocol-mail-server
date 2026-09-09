
/** 
 * DNSResolver.java
 * DNS resolver to get the SMTP host (MX record) for a domain.
 * author 
 *  -Dave Donkeng ndia
 *  -leslie lucynda tingue
 * version 1.0
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DNSResolver {

    // Predefined DNS servers
    private static final String DNS_ULIEGE = "10.0.1.2";
    private static final String DNS_GEMBLOUX = "10.0.2.2";
    private static final String DNS_INFO = "10.0.3.2";

    /**
     * Resolves the mail server IP for a given domain using MX and A DNS lookups.
     * 
     * @param domain domain name
     * @return mail server IP or null if not found
     */
    public String resolveMailServerIp(String domain) {

        String dns = pickDns(domain);
        if (dns == null) {
            System.err.println("Unknown domain: " + domain);
            return null;
        }

        // MX lookup
        String mxLine = dig(dns, "MX", domain);
        if (mxLine == null) {
            System.err.println("No MX for domain " + domain);
            return null;
        }

        // Format: "10 mail.uliege.be."
        String[] parts = mxLine.split("\\s+");
        if (parts.length < 2)
            return null;

        String mxHost = parts[1];
        if (mxHost.endsWith(".")) {
            mxHost = mxHost.substring(0, mxHost.length() - 1);
        }

        // A lookup
        String ip = dig(dns, "A", mxHost);
        return ip;
    }

    /**
     * Picks the DNS server based on the domain.
     * 
     * @param domain domain name
     * @return DNS server IP or null if unknown domain
     */
    private String pickDns(String domain) {
        if (domain.equals("uliege.be"))
            return DNS_ULIEGE;
        if (domain.equals("gembloux.uliege.be"))
            return DNS_GEMBLOUX;
        if (domain.equals("info.uliege.be"))
            return DNS_INFO;
        return null;
    }

    /**
     * Executes a dig command to perform DNS lookups.
     * 
     * @param dns  DNS server IP
     * @param type DNS record type (e.g., MX, A)
     * @param name domain or host name
     * @return first line of dig output or null if not found
     */
    private String dig(String dns, String type, String name) {
        try {
            Process p = new ProcessBuilder(
                    "dig", "-4",
                    "@" + dns,
                    "+short",
                    type,
                    name).start();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {

                String line = br.readLine();
                if (line == null || line.isEmpty())
                    return null;
                return line.trim();
            }
        } catch (IOException e) {
            return null;
        }
    }
}
