package com.oaklandsw.http.sso;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.dcerpc.DcerpcBind;
import jcifs.dcerpc.DcerpcException;
import jcifs.dcerpc.DcerpcHandle;

import com.oaklandsw.http.sso.netlogon.netr_Credential;
import com.oaklandsw.http.sso.netlogon.netr_ServerAuthenticate2;
import com.oaklandsw.http.sso.netlogon.netr_ServerAuthenticate3;
import com.oaklandsw.http.sso.netlogon.netr_ServerReqChallenge;
import com.oaklandsw.util.HexFormatter;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class NetrSession {
    private static final Log _log = LogUtils.makeLogger();

    DcerpcHandle handle;

    // NRPC 2.2.1.3.1.2
    protected static final int WORKSTATION_SECURE_CHANNEL = 2;

    // NRPC
    protected static final int NEG_STRONG = 0x4000;

    public NetrSession(String location, NtlmPasswordAuthentication auth)
	    throws IOException {
	handle = DcerpcHandle.getHandle("ncacn_np:" + location
		+ "[\\PIPE\\netlogon]", auth);

	String server_name = "win2k3";
	// The client computer name does not seem to matter (at least for the
	// first exchange)
	String computer_name = "berlioz";
	netr_Credential clientNonce = new netr_Credential();
	netr_Credential serverNonce = new netr_Credential();

	clientNonce.data = new byte[8];
	new Random().nextBytes(clientNonce.data);

	System.out.println("client challenge: "
		+ HexFormatter.dump(clientNonce.data));

	netr_ServerReqChallenge rc = new netr_ServerReqChallenge(server_name,
		computer_name, clientNonce);
	_log.debug("Attempting ServerReqChallenge to: " + handle);
	try {
	    handle.sendrecv(rc);
	    System.out.println("server challenge: "
		    + HexFormatter.dump(clientNonce.data));
	    System.out.println("return value: "
		    + Long.toString(Util.toUnsigned(rc.retval), 16));
	    if (rc.retval != 0) {
		throw new RuntimeException("ServerReqChallenge failed: 0x"
			+ Long.toString(Util.toUnsigned(rc.retval), 16));
	    }
	    // FIXME - check the return value is NULL

	    // FIXME - if the server_name is wrong, you will get a 0xc0000122
	    // status
	    // which is INVALID_COMPUTER_NAME
	    rc.getResult();
	    _log.debug("ServerReqChallenge completed");
	} catch (DcerpcException ex) {
	    _log.error(ex);
	    if (ex.getMessage().equals(
		    DcerpcBind.MSG_ABSTRACT_SYNTAX_NOT_SUPPORTED)) {
		IOException io = new IOException("Netlogon bind failed to "
			+ location + " because it is not a domain controller");
		io.initCause(ex);
		throw io;
	    }
	    throw ex;
	}

	serverNonce = rc.credentials;

	String account = "Administrator";
	String password = "admin";

	netr_Credential sessionKey = new netr_Credential();
	sessionKey.data = strongSessionKey(password, clientNonce.data,
		serverNonce.data);

	netr_ServerAuthenticate2 a3 = new netr_ServerAuthenticate2(server_name,
		account, WORKSTATION_SECURE_CHANNEL, computer_name, sessionKey,
		NEG_STRONG);

	handle.sendrecv(a3);
	
	if (a3.retval != 0) {
	    throw new RuntimeException("ServerAuthenticate3 failed: 0x"
		    + Long.toString(Util.toUnsigned(a3.retval), 16));
	}

    }

    protected byte[] strongSessionKey(String password, byte[] clientNonce,
	    byte[] serverNonce) {

	// From NRPC spec, 3.4.1.3
	// SET zeroes to 4 bytes of 0
	// ComputeSessionKey(SharedSecret, ClientChallenge, ServerChallenge)
	// M4SS := MD4(UNICODE(SharedSecret))
	// CALL MD5Init(md5context)
	// CALL MD5Update(md5context, zeroes, [4 bytes])
	// CALL MD5Update(md5context, ClientChallenge, [8 bytes])
	// CALL MD5Update(md5context, ServerChallenge, [8 bytes])
	// CALL MD5Final(md5context)
	// CALL HMAC_MD5(md5context.digest, md5context.digest length,
	// M4SS, length of M4SS, output)
	// SET Sk to output

	// The key produced with strong-key support negotiated is 128 bits (16
	// bytes).

	byte[] zeroes = new byte[] { 0, 0, 0, 0 };
	byte[] unicodePassword = null;
	try {
	    unicodePassword = password.getBytes(Util.UNICODE_LE_ENCODING);
	} catch (UnsupportedEncodingException e) {
	    Util.impossible(e);
	}

	Digest md4 = new MD4Digest();
	byte[] md4ss = new byte[md4.getDigestSize()];
	md4.update(unicodePassword, 0, unicodePassword.length);
	md4.doFinal(md4ss, 0);

	Digest md5 = new MD5Digest();
	byte[] md5Out = new byte[md5.getDigestSize()];
	md5.update(zeroes, 0, zeroes.length);
	md5.update(clientNonce, 0, clientNonce.length);
	md5.update(serverNonce, 0, serverNonce.length);
	md5.doFinal(md5Out, 0);

	HMac hmac = new HMac(new MD5Digest());
	byte[] hmacOut = new byte[hmac.getMacSize()];
	hmac.init(new KeyParameter(md4ss));
	hmac.update(md5Out, 0, md5Out.length);
	hmac.doFinal(hmacOut, 0);

	return hmacOut;
    }

    protected byte[] weakSessionKey(String password, byte[] clientNonce,
	    byte[] serverNonce) {
	// From NRPC spec, 3.4.1.3
	// If strong-key support is not negotiated between the client and the
	// server, the session key is computed by using the DES encryption
	// algorithm in ECB mode, as specified in [FIPS81]:

	// ComputeSessionKey(SharedSecret, ClientChallenge,
	// ServerChallenge)
	// M4SS := MD4(UNICODE(SharedSecret))
	// SET sum to ClientChallenge + ServerChallenge
	// SET k1 to lower 7 bytes of the M4SS
	// SET k2 to upper 7 bytes of the M4SS
	// CALL DES_ECB(sum, k1, &output1)
	// CALL DES_ECB(output1, k2, &output2)
	// SET Sk to output2

	// Although normally ClientChallenge and ServerChallenge are treated as
	// byte arrays, in the above psuedocode ClientChallenge and
	// ServerChallenge are treated as 64 bit integers in little endian
	// format to set the sum. The carry of the most significant bit is
	// ignored in the sum of the ClientChallenge and ServerChallenge.
	// The key produced without strong-key support negotiated is 64 bits,
	// and is
	// padded to 128 bits with
	// zeros in the most significant bits.

	Util.implementmeDie();
	return null;
    }
}
