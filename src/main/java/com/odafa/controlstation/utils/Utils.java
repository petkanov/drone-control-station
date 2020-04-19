package com.odafa.controlstation.utils;

import java.io.EOFException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	
	public static byte[] readNetworkMessage(InputStream in) throws Exception {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0) {
			throw new EOFException();
		}

		int msgSize = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

		final byte[] buffer = new byte[msgSize];
		int totalReadSize = 0;
		
		while (totalReadSize < msgSize) {
			int readSize = in.read(buffer, totalReadSize, msgSize - totalReadSize);
			if (readSize < 0) {
				throw new EOFException();
			}
			totalReadSize += readSize;
		}
		return buffer;
	}

	public static byte[] createNetworkMessage(byte[] msgBody) {
		byte[] head = new byte[4];
		int size = msgBody.length;

		for (int i = head.length - 1; i >= 0; i--) {
			head[i] = (byte) (size & 0xff);
			size >>>= 8;
		}

		byte[] result = new byte[head.length + msgBody.length];

		for (int i = 0; i < head.length; i++) {
			result[i] = head[i];
		}
		for (int i = head.length, j = 0; j < msgBody.length; i++, j++) {
			result[i] = msgBody[j];
		}
		return result;
	}
	
	
	


	public static String cleanInputString(String input) {
		String pattern = "\\W";
		String pattern2 = "\\d";
		String replaceWith = "";
		String result = "";

		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);
		StringBuffer sb = new StringBuffer();

		while (m.find()) {
			m.appendReplacement(sb, replaceWith);
		}
		m.appendTail(sb);
		result = sb.toString();

		p = Pattern.compile(pattern2);
		m = p.matcher(result);
		sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, replaceWith);
		}
		m.appendTail(sb);

		return sb.toString();
	}

	public static boolean validateEmail(String emailStr) {
		Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
		return matcher.find();
	}

	public static String cleanInputStringFromSpecialChars(String input) {
		String result = "";

		if (input.length() > 33) {
			result = input.substring(0, 33).replaceAll("[^a-zA-Z0-9\\s]", "");
			result += "...";
			return result;
		} else
			return input.replaceAll("[^a-zA-Z0-9\\s]", "");
	}

	public static String checkNumber(String input) {
		if (validateInteger(input) || validateFloat(input)) {
			if (input.length() > 7)
				return input.substring(0, 7);
			else
				return input;
		} else {
			String result = input.replaceAll("[^0-9]", "");
			if (result.length() > 7)
				return result.substring(0, 7);
			else if (result.length() == 0)
				return "0";
			else
				return result;
		}
	}

	public static boolean validateInteger(String input) {
		Pattern REGEX = Pattern.compile("^[0-9]{1,7}$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = REGEX.matcher(input);
		return matcher.find();
	}

	public static boolean validateFloat(String input) {
		Pattern REGEX = Pattern.compile("^[0-9]{1,7}\\.[0-9]{1,6}$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = REGEX.matcher(input);
		return matcher.find();
	}
}
