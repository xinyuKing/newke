package com.shixi.ecommerce.network;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

public class ClientIpResolver {
    private final List<CidrBlock> trustedProxyBlocks;

    public ClientIpResolver(ClientIpProperties properties) {
        this.trustedProxyBlocks = compileBlocks(properties.getTrustedProxies());
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (remoteAddr == null) {
            return request.getRemoteAddr();
        }
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            for (String candidate : forwarded.split(",")) {
                String normalized = normalizeIp(candidate);
                if (normalized != null) {
                    return normalized;
                }
            }
        }

        String realIp = normalizeIp(request.getHeader("X-Real-IP"));
        return realIp == null ? remoteAddr : realIp;
    }

    private boolean isTrustedProxy(String ip) {
        if (trustedProxyBlocks.isEmpty()) {
            return false;
        }
        InetAddress address = parse(ip);
        if (address == null) {
            return false;
        }
        return trustedProxyBlocks.stream().anyMatch(block -> block.matches(address));
    }

    private static List<CidrBlock> compileBlocks(List<String> values) {
        List<CidrBlock> blocks = new ArrayList<>();
        if (values == null) {
            return blocks;
        }
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            CidrBlock block = CidrBlock.parse(value.trim());
            if (block != null) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    private static String normalizeIp(String candidate) {
        InetAddress address = parse(candidate);
        return address == null ? null : address.getHostAddress();
    }

    private static InetAddress parse(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        try {
            return InetAddress.getByName(candidate.trim());
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private static final class CidrBlock {
        private final BigInteger network;
        private final int prefixLength;
        private final int bitLength;

        private CidrBlock(BigInteger network, int prefixLength, int bitLength) {
            this.network = network;
            this.prefixLength = prefixLength;
            this.bitLength = bitLength;
        }

        private static CidrBlock parse(String cidr) {
            try {
                String[] parts = cidr.split("/", 2);
                InetAddress address = ClientIpResolver.parse(parts[0]);
                if (address == null) {
                    return null;
                }
                int bitLength = address.getAddress().length * 8;
                int prefixLength = parts.length == 2 ? Integer.parseInt(parts[1]) : bitLength;
                if (prefixLength < 0 || prefixLength > bitLength) {
                    return null;
                }
                BigInteger mask = prefixLength == 0
                        ? BigInteger.ZERO
                        : BigInteger.ONE
                                .shiftLeft(bitLength)
                                .subtract(BigInteger.ONE)
                                .shiftRight(bitLength - prefixLength)
                                .shiftLeft(bitLength - prefixLength);
                BigInteger network = toBigInteger(address).and(mask);
                return new CidrBlock(network, prefixLength, bitLength);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private boolean matches(InetAddress address) {
            int addressBits = address.getAddress().length * 8;
            if (addressBits != bitLength) {
                return false;
            }
            if (prefixLength == 0) {
                return true;
            }
            BigInteger mask = BigInteger.ONE
                    .shiftLeft(bitLength)
                    .subtract(BigInteger.ONE)
                    .shiftRight(bitLength - prefixLength)
                    .shiftLeft(bitLength - prefixLength);
            return toBigInteger(address).and(mask).equals(network);
        }

        private static BigInteger toBigInteger(InetAddress address) {
            return new BigInteger(1, address.getAddress());
        }
    }
}
