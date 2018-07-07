/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
 package eu.fiesta_iot.platform.iot_registry.storage.sql;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// IMPORTANT NOTE
// The access type depends on where you place the annotations.
// You have placed them on instance variables so the persistence 
// provider will try to access them directly by reflection.

@Entity
@Table(name = "sparql_query_execution_log")
@NamedQueries({
        // IMPORTANT NOTE
        // INSERT is not allowed as NamedQuery

        // @NamedQuery(name = "QueryLog.insert",
        // query = "INSERT INTO QueryLog ql (ql.ipAddress, ql.userToken,
        // ql.queryHash, ql.startTime) VALUES (:ip_address, :user_token,
        // :query_hash, :start_time)"),
        @NamedQuery(name = "QueryLog.update",
                    query = "UPDATE QueryLog ql SET ql.execTime = :exec_time, ql.aborted = :aborted where ql.id= :id"),
		// @NamedQuery(name = "QueryLog.insertQueryString",
		// query = "INSERT IGNORE INTO sparql_query_log (hash, query) VALUES
		// (:hash, :query)")
})
public class QueryLog implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(QueryLog.class);

	private static MessageDigest sha256;

	static {
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			log.error("Unable to create digest", e);
		}
	}

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	// Ignore the variable
	@Transient
	private long startExecTime = 0;
	// Stored in ms
	@Column(name = "exec_time")
	private long execTime;

	@NotNull
	@Column(name = "ip_address")
	private long ipAddress;

	@Column(name = "user")
	private String userName;

	@Column(name = "user_agent")
	private String userAgent;

	@Transient
	private String query;

	@NotNull
	// Currently store only hash
	@Column(name = "query_hash")
	private String queryHash;

	@Column(name = "aborted")
	private boolean aborted;

	@NotNull
	@Column(name = "created",
	        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private OffsetDateTime created;

	public QueryLog() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getExecTime() {
		return execTime;
	}

	public void setExecTime(long execTime) {
		this.execTime = execTime;
	}

	public long calculateExecTime(long endExecTime) {
		return (endExecTime - startExecTime) / 1000000;
	}

	public void setStartExecTime(long startTime) {
		this.startExecTime = startTime;
	}

	public void setIpAddress(String ip) {
		try {
			InetAddress address = InetAddress.getByName(ip);
			ByteBuffer buffer =
			        ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN);
			// Need to initialize to be unsigned
			buffer.put(new byte[] { 0, 0, 0, 0 });
			buffer.put(address.getAddress());
			buffer.position(0);
			this.ipAddress = buffer.getLong();
		} catch (UnknownHostException e) {
			this.ipAddress = 0;
		}
	}

	public InetAddress getIpAdress() throws UnknownHostException {
		byte[] bytes = BigInteger.valueOf(ipAddress).toByteArray();
		InetAddress address = InetAddress.getByAddress(bytes);
		return address;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String username) {
		this.userName = username;
	}

	public String getQueryHash() {
		return this.queryHash;
	}

	public String getQuery() {
		return this.query;
	}

	public void setQuery(String query) {
		this.query = query;
		byte[] hash = sha256.digest(query.getBytes(StandardCharsets.UTF_8));
		this.queryHash = Base64.getUrlEncoder().encodeToString(hash);
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public boolean isAborted() {
		return aborted;
	}

	public void setAborted(boolean aborted) {
		this.aborted = aborted;
	}

	public OffsetDateTime getStartTime() {
		return created;
	}

	public void setStartTime(OffsetDateTime startTime) {
		this.created = startTime;
	}
}
