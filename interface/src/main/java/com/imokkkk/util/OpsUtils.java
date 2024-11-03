package com.imokkkk.util;

/**
 * @author wyliu
 * @date 2024/10/27 21:14
 * @since 1.0
 */
public class OpsUtils {
    private static final String _PRIVATE_KEY =
            "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCV0jSn5mi5nFfGcKt3dut7Q9Mlr2k0LpjCUCbyNAWKzJ2JzTnHAIl6Rcip1b9hqeEqPX7PeW6tnyyt+EFEjm+C5AlZri/NTLKr8X0vB/enjGwQs7jmYkUa6V7bRvgMOAAtxRCFtkzWDMwNiHW9CasYQ6akeWhqcQWA8wB0IksPBPqxeMPsm2ZP+WJphBLY5Yk6svHCezLjlay9JrkJR98fbI33vUFvqofq1DFD0d0WtwWD4qhm/aaEo73tBQ9JMV6WFtEHocJ8VSApIXh8wy3lixcci6JQzSwHVwgaGFN1sfmMGF5nld+siiGL59FdnrkRO9bpkCtoxdMNi/iXZEHnAgMBAAECggEAPBYuIBh8b9SQL3oIisUa9Djjef27x73YbjYPKJxMjLo7hITWY0WH3Y4XSGX9d4HWWEaJkVQ2W1O3a55hLsmhV3F1fo4phcD495TGjBI59OyQerJZuaw34u8tp+vyl5PuHm0mjznp3v8K53KPJd22zOh08QnwIqBpgKn0yJ7oU4EgpTiI70wgnwE+gNzcABSd3sSVGJLhZYKZQGlQcqZG6mrz2CimCWaycS1Gnvddzv2D8CvjwyPT29yerrwBVaDXiaZIdZXLPKOmgoDAA3M6PeuhmwYQmb1wpdHOTZVcgNRfgnGeQWSfuFn7/Pfmnfp/jCn4CiBjRwlzoMbbyNGCoQKBgQDlm4adiWTYkGEJC1ABlfPrdPsO7rI09EZ2DlZkbuICEEAsXkf/+7ZWHiXJAqxzW0bmJt8TC14wVkniLg3CpFr3/yxnRMpI2OwTUQpHXDc4sUqlZeQYnTukebikOt6fChx9+9UkrdbzGhD9WBEPmHP5FUFxx5H4FBqETUjltq9+nQKBgQCnCt66MyqOG4U0m+u2SG5YHplX7U20zgnW7l63rSZOWG/qCvDj7mUjbMRN4HgnjoZW8QVqR1RHyKnHEKMRuM1jzJViSEEpv6CnLc7IDXbTVmaM/3jeXIOZDZNJHPCY5NDkVgIATz4dVuNhJQkYl3UHrpc0Cx6tvudHU7ye3F55UwKBgQDJSCoeeI+7efZW41/jw/zs8MQWNxJqcapDXBZIPRxMCsr3Gj+sHJGftRYSvRFInz/sCy+Llm+fmEV0zkKp20MUHNJkRV2/tspdtLF6FVEjleiGTmhDW7MpYLuI3ULD0S1hFB+p4+uHedZjAY0TSuZ/+S5B/F3uSFFmognBtwDp0QKBgQCVp4RyN4MJvBg0UPj136CUbB2gC+fRc8KXAAp17rfh+xOREGpPGvcbrMeWqeOcNPGglODwHaWhhmKtCPe/2dyRa+nuGqPe3mL1RHH9AysRHlhUPFGyjhoMln/9QeNAZ8fKuk+irVwN9xlRoFes44yi7EVlCA1dRip42cmkXfR6JwKBgQDJKvR7nKGlSltCsi9seIsd9q9ytNTrSnI+Eh91xlBgChaxLn53xZLfbUsQtfcc+Z/vmu7hkcfSLtOB7a7AwNWVz3oy8fSCZAbQm9EPkXxREoJRjWNpyHJm5WXYQ/rs9YgFXMQYHkrTN6ZLiwXiEAAU4yCwx46u812IpTV6sKsDkg==";
    private static final String _PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAldI0p+ZouZxXxnCrd3bre0PTJa9pNC6YwlAm8jQFisydic05xwCJekXIqdW/YanhKj1+z3lurZ8srfhBRI5vguQJWa4vzUyyq/F9Lwf3p4xsELO45mJFGule20b4DDgALcUQhbZM1gzMDYh1vQmrGEOmpHloanEFgPMAdCJLDwT6sXjD7JtmT/liaYQS2OWJOrLxwnsy45WsvSa5CUffH2yN971Bb6qH6tQxQ9HdFrcFg+KoZv2mhKO97QUPSTFelhbRB6HCfFUgKSF4fMMt5YsXHIuiUM0sB1cIGhhTdbH5jBheZ5XfrIohi+fRXZ65ETvW6ZAraMXTDYv4l2RB5wIDAQAB";

    public static String get(String opsKey) {
        // 这里为了演示，就直接返回了一个固定的值
        if ("queryPayAccoun_privateSecret".equals(opsKey)) {
            return _PRIVATE_KEY;
        } else if ("queryPayAccoun_publicSecret".equals(opsKey)) {
            return _PUBLIC_KEY;
        }
        return null;
    }
}
