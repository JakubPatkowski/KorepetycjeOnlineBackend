package com.example.ekorki.service;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class IpHasher {
	private static final String SALT = "your_random_salt_here";

	// Hashowanie IP do przechowywania w bazie
	public String hashIp(String ip) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String saltedIp = ip + SALT;
			byte[] hash = digest.digest(saltedIp.getBytes());
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Error hashing IP", e);
		}
	}

	// Sprawdzanie czy przychodzące IP zgadza się z zahashowanym
	public boolean compareIp(String incomingIp, String hashedIp) {
		String newHash = hashIp(incomingIp);
		return newHash.equals(hashedIp);
	}
}