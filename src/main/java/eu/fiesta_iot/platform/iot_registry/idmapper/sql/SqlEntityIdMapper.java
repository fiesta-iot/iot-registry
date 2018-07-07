/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.idmapper.sql;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.validation.constraints.Pattern;

import com.sun.istack.NotNull;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class SqlEntityIdMapper extends EntityIdMapper implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@NotNull
	@Column(name = "hash")
	@Pattern(regexp = "^[=a-zA-Z0-9_-]{44}$", message = "Invalid hash value")
	private String hash;

	@NotNull
	@Column(name = "url")
	// Still to check that this is the best fit pattern
	// @Pattern(regexp="(http://|https://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w]+)?",
	// message="Not URL")
	private String url;

	public SqlEntityIdMapper() {
	}

	public SqlEntityIdMapper(EntityType type, String hash, String url) {
		super.setType(type);
		this.hash = hash;
		this.url = url;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
