package com.zhang.scanner.settings.ai;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.logging.Logger;

/**
 * API Key 加解密工具(AES-256-GCM, 纯 JDK, 无 IntelliJ 平台依赖)
 *
 * 落盘格式: ENC1:Base64( IV(12字节) || GCM密文 || GCM Tag(16字节) )
 * - 内存中始终是明文, 仅持久化层(getState/loadState)调用本工具;
 * - 旧版本明文(无 ENC1: 前缀)在解密时原样透传, 下次保存时自动转为密文;
 * - 解密失败返回空串, 绝不把密文当 API Key 发出去;
 *
 * 安全边界: 密钥为代码内置常量, 属混淆级保护, 防"直接查看 XML/导出设置/备份", 不防 jar 逆向;
 * 发布后密钥不可更换, 否则所有用户已存密文全部失效; 若必须更换, 新增 ENC2: 前缀并保留旧密钥解密。
 */
public final class AiKeyCipher {

    /**
     * 版本前缀: 标识密文由哪一代密钥/算法加密
     */
    private static final String PREFIX = "ENC1:";

    /**
     * 32 字节 AES-256 密钥(64 位十六进制)。
     * 注意: 一旦发布不可修改, 见类注释。
     */
    private static final String KEY_HEX = "03a42cb25e745d16c763a5230c2d3b86c8321d524224043cbb0e576d5315b322";

    /**
     * GCM 推荐 IV 长度
     */
    private static final int IV_LENGTH = 12;

    /**
     * GCM Tag 长度(位)
     */
    private static final int TAG_BITS = 128;

    private static final Logger LOG = Logger.getLogger(AiKeyCipher.class.getName());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final SecretKeySpec KEY_SPEC = buildKeySpec();

    private AiKeyCipher() {
    }

    /**
     * 构造 AES 密钥并校验长度, 密钥配置错误在开发期直接暴露
     */
    private static SecretKeySpec buildKeySpec() {
        byte[] key = HexFormat.of().parseHex(KEY_HEX);
        if (key.length != 32) {
            throw new IllegalStateException("API Key 加密密钥必须为 32 字节, 实际: " + key.length);
        }
        return new SecretKeySpec(key, "AES");
    }

    /**
     * 是否已加密(仅按前缀判断, 不校验密文内容)
     */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /**
     * 判断持久化原值是否"无法解密的密文"(带 ENC1: 前缀但解密结果为空):
     * 密文损坏或密钥不匹配场景。供 loadState 计算标记, 设置页保存时据此提示用户,
     * 避免在用户不知情时静默清空磁盘上仍存在的密文。
     */
    public static boolean isUndecryptable(String rawStored) {
        return isEncrypted(rawStored) && decrypt(rawStored).isEmpty();
    }

    /**
     * 加密: null 返回空串; 空串/已加密值原样返回(幂等); 异常返回空串(宁可存空, 不存半截密文)
     */
    public static String encrypt(String plain) {
        if (plain == null) {
            return "";
        }
        if (plain.isEmpty() || isEncrypted(plain)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, KEY_SPEC, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            LOG.warning("API Key 加密失败: " + e.getMessage());
            return "";
        }
    }

    /**
     * 解密: null/空串返回空串; 无前缀(旧明文)原样透传; 密文损坏/密钥不匹配返回空串
     */
    public static String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) {
            return "";
        }
        if (!isEncrypted(stored)) {
            return stored;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                return "";
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, KEY_SPEC, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 不打印密文/明文/密钥, 只记录失败原因
            LOG.warning("API Key 解密失败, 配置可能损坏或密钥不匹配: " + e.getMessage());
            return "";
        }
    }
}
