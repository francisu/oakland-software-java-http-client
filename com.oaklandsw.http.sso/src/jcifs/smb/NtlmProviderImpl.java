package jcifs.smb;

import java.io.UnsupportedEncodingException;

import jcifs.util.DES;
import jcifs.util.HMACT64;
import jcifs.util.LogStream;
import jcifs.util.MD4;

public class NtlmProviderImpl implements NtlmProvider {

    // private static final Random RANDOM = new Random();

    private static LogStream log = LogStream.getInstance();

    // KGS!@#$%
    private static final byte[] S8 = { (byte) 0x4b, (byte) 0x47, (byte) 0x53,
	    (byte) 0x21, (byte) 0x40, (byte) 0x23, (byte) 0x24, (byte) 0x25 };

    private static void E(byte[] key, byte[] data, byte[] e) {
	byte[] key7 = new byte[7];
	byte[] e8 = new byte[8];

	for (int i = 0; i < key.length / 7; i++) {
	    System.arraycopy(key, i * 7, key7, 0, 7);
	    DES des = new DES(key7);
	    des.encrypt(data, e8);
	    System.arraycopy(e8, 0, e, i * 8, 8);
	}
    }

    /**
     * Computes the 24 byte ANSI password hash given the 8 byte server
     * challenge.
     */
    public byte[] getAnsiHash(int level, String domain, String username,
	    String password, byte[] challenge, byte[] clientChallenge) {
	switch (level) {
	case 0:
	case 1:
	    return getPreNTLMResponse(password, challenge);
	case 2:
	    return getNTLMResponse(password, challenge);
	case 3:
	case 4:
	case 5:
	    return getLMv2Response(domain, username, password, challenge,
		    clientChallenge);
	default:
	    return getPreNTLMResponse(password, challenge);
	}
    }

    /**
     * Computes the 24 byte Unicode password hash given the 8 byte server
     * challenge.
     */
    public byte[] getUnicodeHash(int level, String domain, String username,
	    String password, byte[] challenge, byte[] clientChallenge) {
	switch (level) {
	case 0:
	case 1:
	case 2:
	    return getNTLMResponse(password, challenge);
	case 3:
	case 4:
	case 5:
	    return getNTLMv2Response(domain, username, password, null,
		    challenge, clientChallenge);
	default:
	    return getNTLMResponse(password, challenge);
	}
    }

    /**
     * Generate the ANSI DES hash for the password associated with these
     * credentials.
     */
    public byte[] getPreNTLMResponse(String password, byte[] challenge) {
	byte[] p14 = new byte[14];
	byte[] p21 = new byte[21];
	byte[] p24 = new byte[24];
	byte[] passwordBytes;
	try {
	    passwordBytes = password.toUpperCase().getBytes(
		    ServerMessageBlock.OEM_ENCODING);
	} catch (UnsupportedEncodingException uee) {
	    throw new RuntimeException("Try setting jcifs.encoding=US-ASCII",
		    uee);
	}
	int passwordLength = passwordBytes.length;

	// Only encrypt the first 14 bytes of the password for Pre 0.12 NT LM
	if (passwordLength > 14) {
	    passwordLength = 14;
	}
	System.arraycopy(passwordBytes, 0, p14, 0, passwordLength);
	E(p14, S8, p21);
	E(p21, challenge, p24);
	return p24;
    }

    public byte[] getLMResponse(String password, byte[] challenge) {
	return new byte[0];
    }

    public byte[] getLMv2Response(String domain, String user, String password,
	    byte[] challenge, byte[] clientChallenge) {
	try {
	    byte[] response = new byte[24];
	    MD4 md4 = new MD4();
	    md4.update(password.getBytes("UnicodeLittleUnmarked"));
	    HMACT64 hmac = new HMACT64(md4.digest());
	    hmac.update(user.toUpperCase().getBytes("UnicodeLittleUnmarked"));
	    hmac.update(domain.toUpperCase().getBytes("UnicodeLittleUnmarked"));
	    hmac = new HMACT64(hmac.digest());
	    hmac.update(challenge);
	    hmac.update(clientChallenge);
	    hmac.digest(response, 0, 16);
	    System.arraycopy(clientChallenge, 0, response, 16, 8);
	    return response;
	} catch (Exception ex) {
	    if (log.level > 0)
		ex.printStackTrace(log);
	    return null;
	}
    }

    public byte[] getNTLM2SessionResponse(String password, byte[] challenge,
	    byte[] clientChallenge) {
	return new byte[0];
    }

    public byte[] getNTLMResponse(String password, byte[] challenge) {
	byte[] uni = null;
	byte[] p21 = new byte[21];
	byte[] p24 = new byte[24];

	try {
	    uni = password.getBytes("UnicodeLittleUnmarked");
	} catch (UnsupportedEncodingException uee) {
	    if (log.level > 0)
		uee.printStackTrace(log);
	}
	MD4 md4 = new MD4();
	md4.update(uni);
	try {
	    md4.digest(p21, 0, 16);
	} catch (Exception ex) {
	    if (log.level > 0)
		ex.printStackTrace(log);
	}
	E(p21, challenge, p24);
	return p24;
    }

    public byte[] getNTLMv2Response(String target, String user,
	    String password, byte[] targetInformation, byte[] challenge,
	    byte[] clientChallenge) {
	return new byte[0];
    }

}
