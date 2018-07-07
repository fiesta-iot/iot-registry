/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.idmapper.hashid;

import org.hashids.Hashids;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;

public class HashidEntityIdMapper extends EntityIdMapper {

	private Hashids hashids;

	private String url;

	private String hash;

	public HashidEntityIdMapper(EntityType type, String secret) {
		super.setType(type);
		this.hashids = new Hashids(secret);
		this.url = null;
		this.hash = null;
	}

	public HashidEntityIdMapper(EntityType type, String url, String secret) {
		this(type, secret);
		this.setUrl(url);
	}

	@Override
	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		long[] numbers = hashids.decode(hash);
		byte[] str = new byte[numbers.length];
		for (int i = 0; i < numbers.length; i++) {
			str[i] = (byte) (numbers[i] & 0x0FF);
		}

		this.url = new String(str);
		this.hash = hash;
	}

	@Override
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		byte[] strBytes = url.getBytes();
		long[] numbers = new long[strBytes.length];
		for (int i = 0; i < numbers.length; i++) {
			numbers[i] = (long) strBytes[i];
		}

		this.hash = hashids.encode(numbers);
		this.url = url;
	}
}
