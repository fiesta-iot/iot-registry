/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.idmapper.cipher;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;

/**
 * Format: HASH = AES[CRC32 | URL]
 *
 */
public abstract class AesEntityIdMapper extends EntityIdMapper {

	private static Logger log =
	        LoggerFactory.getLogger(AesEntityIdMapper.class);

	// TODO: Define configuration for init vector and salt
	private static final byte[] INIT_VECTOR = {
	        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	        0x00, 0x00, 0x00, 0x00, 0x00 };

	private static final IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR);

	private static final byte[] salt = INIT_VECTOR;

	private final Cipher cipher;
	private final SecretKey secret;

	private String url;

	private String hash;

	// Caching so to speed up
	// arrays are mutable, values() must return a copy of the array of elements
	// just in case you happen to change it. Creating this copy each time is
	// relatively expensive
	private static EntityType[] ENTITY_TYPES = EntityType.values();

	// static {
	// SecureRandom random = new SecureRandom();
	// salt = new byte[8];
	// random.nextBytes(salt);
	// }

	public AesEntityIdMapper(EntityType type, String password) throws NoSuchAlgorithmException,
	                                          NoSuchPaddingException,
	                                          InvalidKeySpecException {
		this(type, generateKeyFromPassword(password));

		this.url = null;
		this.hash = null;
	}

	public AesEntityIdMapper(EntityType type, byte[] key) throws NoSuchAlgorithmException,
	                                     NoSuchPaddingException {
		super.setType(type);
		secret = new SecretKeySpec(key, "AES");
		cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		this.url = null;
		this.hash = null;
	}

	public AesEntityIdMapper(EntityType type, String url,
	        byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException,
	                    InvalidKeySpecException {
		this(type, key);
		this.setUrl(url);
	}

	@Override
	public String getHash() {
		return hash;
	}

	public void setHash(String hash) throws InvalidEntityIdException {
		try {
			byte[] hashBytes = Base64.getUrlDecoder().decode(hash);

			byte[] urlBytes = extractUrlFromHash(hashBytes);

			// Validate CRC and extract URL
			this.url = new String(urlBytes, "UTF-8");
			this.hash = hash;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		try {
			byte[] urlBytes = url.getBytes("UTF-8");
			byte[] hashBytes = createHashFromUrl(urlBytes);

			// Use URL encoder to avoid problems when referencing it
			this.hash = Base64.getUrlEncoder().encodeToString(hashBytes);
			this.url = url;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private static byte[] calculateCrc(EntityType type, byte[] urlBytes) {
		return calculateCrc(type, urlBytes, 0, urlBytes.length);
	}

	private static byte[] calculateCrc(EntityType type, byte[] urlBytes,
	                                   int offset, int length) {
		// Calculate CRC
		Checksum checksum = new CRC32();
		// Update the current checksum with the specified array of bytes
		checksum.update(type.ordinal());
		checksum.update(urlBytes, offset, length);

		// Get the current checksum value
		long crcUrl = checksum.getValue();
		// Convert to byte array
		return ByteBuffer.allocate(Long.BYTES).putLong(crcUrl).array();
	}

	private byte[] createHashFromUrl(byte[] urlBytes) {
		byte[] encrypted = null;
		try {
			// Calculate CRC
			byte[] crcUrlBytes = calculateCrc(getType(), urlBytes);
			byte[] typeBytes = ByteBuffer.allocate(Integer.BYTES)
			        .putInt(getType().ordinal()).array();

			// Note: In this case it is not necessary to use an output byte
			// array as CRC32 is
			// shorter than a block and the update is not returning data.
			cipher.init(Cipher.ENCRYPT_MODE, secret, iv);
			// Cipher CRC
			cipher.update(crcUrlBytes);
			// Cipher type
			cipher.update(typeBytes);
			// Cipher the URL
			encrypted = cipher.doFinal(urlBytes);

		} catch (InvalidKeyException | InvalidAlgorithmParameterException
		         | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}

		return encrypted;
	}

	private byte[]
	        extractUrlFromHash(byte[] hashBytes) throws InvalidEntityIdException {
		try {
			cipher.init(Cipher.DECRYPT_MODE, secret, iv);
			byte[] decryptedHash = cipher.doFinal(hashBytes);

			byte[] crc = Arrays.copyOf(decryptedHash, Long.BYTES);
			int offset = Long.BYTES; 
			byte[] typeBytes = Arrays.copyOfRange(decryptedHash, offset, offset + Integer.BYTES);
			offset += Integer.BYTES;
			EntityType type = ENTITY_TYPES[ByteBuffer.wrap(typeBytes).getInt()];
			if (type != getType()) {
				throw new IllegalArgumentException("Not valid hash format");
			}

			byte[] url = Arrays.copyOfRange(decryptedHash, offset,
			                                decryptedHash.length);

			if (!Arrays.equals(crc, calculateCrc(type, url))) {
				throw new IllegalArgumentException("Not valid hash format");
			}

			return url;
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			// Should never get here
			e.printStackTrace();
			return null;
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new InvalidEntityIdException("Not valid hash format");
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Not valid hash format");
		}
	}

	/**
	 * Derive the key, given password and salt.
	 * <p>
	 * Note: This function takes quite a long time (aprox. 200ms)
	 * </p>
	 * 
	 * @param password
	 *            the password
	 * 
	 * @return key compliant with AES
	 */
	protected static byte[] generateKeyFromPassword(String password) {
		try {
			SecretKeyFactory factory =
			        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec =
			        new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);

			return tmp.getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			log.error("Unable to generate AES key from password", e);
		}

		return null;
	}
}
