package com.minsait.custom;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.security.crypto.password.AbstractPasswordEncoder;

public class ConfigDBPasswordEncoder extends AbstractPasswordEncoder {
	private static final String SALT_KEY = "PveFT7isDjGYFTaYhc2Fzw==";
	private static final int ITERATION_COUNT = 5;

	@Override
	protected byte[] encode(CharSequence password, byte[] salt) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			digest.reset();
			digest.update(Base64.getDecoder().decode(SALT_KEY));
			byte[] btPass = digest.digest(((String) password).getBytes("UTF-8"));
			for (int i = 0; i < ITERATION_COUNT; i++) {
				digest.reset();
				btPass = digest.digest(btPass);
			}
			return btPass;
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			digest.digest(encodedPassword.getBytes("UTF-8"));
			final byte[] salt = Base64.getDecoder().decode(SALT_KEY);
			final String rawEncodedPassword = Base64.getEncoder().encodeToString(encode(rawPassword, salt));
			return rawEncodedPassword.equals(encodedPassword);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {

		}

		return false;

	}

}
